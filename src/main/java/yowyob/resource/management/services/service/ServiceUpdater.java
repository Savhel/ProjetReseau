package yowyob.resource.management.services.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.enums.EventClass;
import yowyob.resource.management.events.service.ServiceEvent;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.services.interfaces.updaters.Updater;
import yowyob.resource.management.services.policy.updaters.ServiceUpdaterPolicy;
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

@Component
public class ServiceUpdater implements Updater {
    private final ServiceUpdaterPolicy serviceUpdaterPolicy;
    private final ServiceActionExecutor serviceActionExecutor;

    private final TaskScheduler taskScheduler;

    @Getter
    private final Map<UUID, List<Event>> scheduledEvents = new ConcurrentHashMap<>();
    private final Map<UUID, List<Tuple<Event, ScheduledFuture<?>>>> scheduledFutures = new ConcurrentHashMap<>();
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final BlockingQueue<Event> waitingEvents = new LinkedBlockingQueue<>();
    private static final Logger logger = LoggerFactory.getLogger(ServiceUpdater.class);

    @Autowired
    public ServiceUpdater(ServiceUpdaterPolicy serviceUpdaterPolicy,
                          ServiceActionExecutor serviceActionExecutor,
                          TaskScheduler taskScheduler) {
        this.serviceUpdaterPolicy = serviceUpdaterPolicy;
        this.serviceActionExecutor = serviceActionExecutor;
        this.taskScheduler = taskScheduler;
    }

    @Override
    @EventListener
    public synchronized void handleEvent(Event event) throws ExecutorPolicyViolationException, UpdaterPolicyViolationException {
        if (event != null) {
            logger.info("Received event of class: {} {}", event.getEventClass(),
                    event.getEventClass() == EventClass.Service ? "" : "unmanaged, passing");

            if (event.getEventClass() == EventClass.Service) {
                if (paused.get()) {
                    this.waitingEvents.add(event);
                    logger.warn("ServiceUpdater is paused. Event of class={} with entityId={} scheduled at {} has been queued.",
                            event.getEventClass(),
                            event.getEntityId(),
                            event.getEventStartDateTime());
                    return;
                }

                logger.info("Processing Service Event for entityId: {}",
                        event.getEntityId());
                if (this.serviceUpdaterPolicy.isExecutionAllowed(event,
                        this.scheduledEvents.getOrDefault(event.getEntityId(), new ArrayList<>()))
                ) {
                    scheduleTask((ServiceEvent) event);
                }
            }
        }
    }

    public synchronized void forceEventScheduling(Event event) {
        ServiceEvent serviceEvent = (ServiceEvent) event;
        logger.info("Service Event scheduling for entityId: {} at {} without Policy verification",
                serviceEvent.getEntityId(), serviceEvent.getEventStartDateTime());
        scheduleTask(serviceEvent);
    }

    private void scheduleTask(ServiceEvent serviceEvent) throws UpdaterPolicyViolationException {
        ServiceAction action = (ServiceAction) serviceEvent.getAction();

        Instant executionTime = serviceEvent.getEventStartDateTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
        logger.info("Scheduling task for Service Event with entityId: {} at time: {}",
                serviceEvent.getEntityId(), executionTime);

        ScheduledFuture<?> future = taskScheduler.schedule(() -> executeAction(action), executionTime);
        if (!scheduledEvents.containsKey(serviceEvent.getEntityId())) {
            scheduledEvents.put(serviceEvent.getEntityId(), new ArrayList<>());
        }
        scheduledEvents.get(serviceEvent.getEntityId()).add(serviceEvent);

        if (!scheduledFutures.containsKey(serviceEvent.getEntityId())) {
            scheduledFutures.put(serviceEvent.getEntityId(), new ArrayList<>());
        }
        scheduledFutures.get(serviceEvent.getEntityId()).add(new Tuple<>(serviceEvent, future));

        logger.info("Successfully scheduled Task for Service Event with entityId: {} at time: {}",
                serviceEvent.getEntityId(), executionTime);
    }

    private void executeAction(ServiceAction action) {
        logger.info("Executing scheduled Service Action for entityId: {}", action.getEntityId());
        this.serviceActionExecutor.executeAction(action);
        scheduledEvents.remove(action.getEntityId());

        logger.info("Successfully executed scheduled Service Action for entityId: {}", action.getEntityId());
    }

    public synchronized void unscheduleEvent(Event event) {
        if (event instanceof ServiceEvent serviceEvent) {
            UUID entityId = serviceEvent.getEntityId();
            List<Event> events = scheduledEvents.get(entityId);
            if (events != null && events.remove(serviceEvent)) {
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
        }
    }

    @Override
    public void pause() {
        paused.set(true);
        logger.warn("ServiceUpdater is now PAUSED. New events will wait until resume() is called.");
    }

    @Override
    public void resume() {
        if (!paused.get()) {
            logger.info("ServiceUpdater is already running.");
            return;
        }

        logger.info("ServiceUpdater is processing queued events before resuming...");
        while (!this.waitingEvents.isEmpty()) {
            Event event = this.waitingEvents.poll();
            handleEvent(event);
        }
        paused.set(false);
        logger.info("All waiting events have been processed. ServiceUpdater is now RESUMED.");
    }
}