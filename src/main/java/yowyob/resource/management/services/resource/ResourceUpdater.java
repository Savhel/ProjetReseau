package yowyob.resource.management.services.resource;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.enums.EventClass;
import yowyob.resource.management.events.resource.ResourceEvent;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.services.interfaces.updaters.Updater;
import yowyob.resource.management.services.policy.updaters.ResourceUpdaterPolicy;
import yowyob.resource.management.helpers.Tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ResourceUpdater implements Updater {
    private final ResourceUpdaterPolicy resourceUpdaterPolicy;
    private final ResourceActionExecutor resourceActionExecutor;
    private final TaskScheduler taskScheduler;

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final BlockingQueue<Event> waitingEvents = new LinkedBlockingQueue<>();

    @Getter
    private final Map<UUID, List<Event>> scheduledEvents = new ConcurrentHashMap<>();
    private final Map<UUID, List<Tuple<Event, ScheduledFuture<?>>>> scheduledFutures = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock eventLock = new ReentrantReadWriteLock();
    private static final Logger logger = LoggerFactory.getLogger(ResourceUpdater.class);

    @Autowired
    public ResourceUpdater(ResourceUpdaterPolicy resourceUpdaterPolicy,
                           ResourceActionExecutor resourceActionExecutor,
                           TaskScheduler taskScheduler) {
        this.resourceUpdaterPolicy = resourceUpdaterPolicy;
        this.resourceActionExecutor = resourceActionExecutor;
        this.taskScheduler = taskScheduler;
    }

    @Override
    @EventListener
    public Mono<Void> handleEvent(Event event) throws ExecutorPolicyViolationException, UpdaterPolicyViolationException {
        eventLock.writeLock().lock();
        try {
        if (event != null) {
            logger.info("Received event of class: {}", event.getEventClass());

            if (event.getEventClass() == EventClass.Resource) {
                if (paused.get()) {
                    this.waitingEvents.add(event);
                    logger.warn("ResourceUpdater is paused. Event of class={} with entityId={} scheduled at {} has been queued.",
                            event.getEventClass(),
                            event.getEntityId(),
                            event.getEventStartDateTime());
                    return null;
                }

                ResourceEvent resourceEvent = (ResourceEvent) event;
                logger.info("Processing Resource Event for entityId: {}", resourceEvent.getEntityId());
                this.resourceUpdaterPolicy.isExecutionAllowed(resourceEvent,
                        this.scheduledEvents.getOrDefault(resourceEvent.getEntityId(), new ArrayList<>()))
                        .doOnSuccess(allowed -> {
                            if (allowed) {
                                scheduleTask(resourceEvent);
                            }
                        })
                        .doOnError(error -> {
                            logger.error("Policy violation for Resource Event with entityId: {}: {}", 
                                    resourceEvent.getEntityId(), error.getMessage());
                        })
                        .subscribe();
            }
        }
        } finally {
            eventLock.writeLock().unlock();
        }
        return Mono.empty();
    }

    public Mono<Void> forceEventScheduling(Event event) {
        eventLock.writeLock().lock();
        try {
        ResourceEvent resourceEvent = (ResourceEvent) event;
        logger.warn("Resource Event scheduling for entityId: {} at {} without Policy verification",
                resourceEvent.getEntityId(), resourceEvent.getEventStartDateTime());
        scheduleTask(resourceEvent);
        } finally {
            eventLock.writeLock().unlock();
        }
        return Mono.empty();
    }

    private Mono<Void> scheduleTask(ResourceEvent resourceEvent) throws ExecutorPolicyViolationException, UpdaterPolicyViolationException {
        ResourceAction action = (ResourceAction) resourceEvent.getAction();

        Instant executionTime = resourceEvent.getEventStartDateTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
        logger.info("Scheduling task for Resource Event with entityId: {} at time: {}",
                resourceEvent.getEntityId(), executionTime);

        ScheduledFuture<?> future = taskScheduler.schedule(() -> this.executeAction(action), executionTime);
        if (!scheduledEvents.containsKey(resourceEvent.getEntityId())) {
            scheduledEvents.put(resourceEvent.getEntityId(), new ArrayList<>());
        }
        scheduledEvents.get(resourceEvent.getEntityId()).add(resourceEvent);

        if (!scheduledFutures.containsKey(resourceEvent.getEntityId())) {
            scheduledFutures.put(resourceEvent.getEntityId(), new ArrayList<>());
        }
        scheduledFutures.get(resourceEvent.getEntityId()).add(new Tuple<>(resourceEvent, future));

        logger.info("Successfully scheduled Task for Resource Event with entityId: {} at time: {}",
                resourceEvent.getEntityId(), executionTime);
        return Mono.empty();
    }

    private void executeAction(ResourceAction action) {
        logger.info("Executing scheduled Resource Action for entityId: {}", action.getEntityId());
        this.resourceActionExecutor.executeAction(action)
                .doOnSuccess(result -> {
                    scheduledEvents.remove(action.getEntityId());
                    logger.info("Successfully executed scheduled Resource Action for entityId: {}", action.getEntityId());
                })
                .doOnError(error -> {
                    logger.error("Failed to execute scheduled Resource Action for entityId: {}: {}", 
                            action.getEntityId(), error.getMessage());
                })
                .subscribe();
    }

    public Mono<Void> unscheduleEvent(Event event) {
        eventLock.writeLock().lock();
        try {
        if (event instanceof ResourceEvent resourceEvent) {
            UUID entityId = resourceEvent.getEntityId();
            List<Event> events = scheduledEvents.get(entityId);
            if (events != null && events.remove(resourceEvent)) {
                if (events.isEmpty()) {
                    scheduledEvents.remove(entityId);
                }
            } else {
                logger.warn("No scheduled event found for entityId: {}", entityId);
            }

            List<Tuple<Event, ScheduledFuture<?>>> futures = scheduledFutures.get(entityId);
            Tuple<Event, ScheduledFuture<?>> futureRecord = futures.stream()
                    .filter(tuple -> tuple.getFirst().equals(event))
                    .findFirst()
                    .orElse(null);

            if (futureRecord != null && futures.remove(futureRecord)) {
                futureRecord.getSecond().cancel(true);
                if (futures.isEmpty()) {
                    scheduledFutures.remove(entityId);
                }
            } else {
                logger.warn("No scheduled event found for entityId: {}", entityId);
            }

            logger.info("Successfully unscheduled event for entityId: {}", entityId);
        } else {
            logger.warn("Invalid event type received for un-scheduling.");
        }
        } finally {
            eventLock.writeLock().unlock();
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> pause() {
        paused.set(true);
        logger.warn("ResourceUpdater is now PAUSED. New events will wait until resume() is called.");
        return Mono.empty();
    }

    @Override
    public Mono<Void> resume() {
        if (!paused.get()) {
            logger.info("ResourceUpdater is already running.");
            return Mono.empty();
        }

        logger.info("ResourceUpdater is processing queued events before resuming...");
        while (!this.waitingEvents.isEmpty()) {
            Event event = this.waitingEvents.poll();
            handleEvent(event);
        }
        paused.set(false);
        logger.info("All waiting events have been processed. ResourceUpdater is now RESUMED.");
        return Mono.empty();
    }
}