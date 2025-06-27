package yowyob.resource.management.services.policy.updaters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.operations.ResourceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.helpers.Tuple;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.models.resource.enums.ResourceStatus;
import yowyob.resource.management.repositories.resource.ResourceRepository;
import yowyob.resource.management.services.interfaces.policies.UpdaterPolicy;
import yowyob.resource.management.services.policy.validators.operations.ResourceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ResourceTransitionValidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ResourceUpdaterPolicy implements UpdaterPolicy {

    private final ResourceRepository resourceRepository;
    private final ResourceTransitionValidator transitionValidator;
    private final ResourceStatusBasedOperationValidator statusBasedOperationValidator;
    private static final Logger logger = LoggerFactory.getLogger(ResourceUpdaterPolicy.class);

    @Autowired
    public ResourceUpdaterPolicy(ResourceRepository resourceRepository,
                                 ResourceTransitionValidator transitionValidator,
                                 ResourceStatusBasedOperationValidator statusBasedOperationValidator) {
        this.resourceRepository = resourceRepository;
        this.transitionValidator = transitionValidator;
        this.statusBasedOperationValidator = statusBasedOperationValidator;
    }

    @Override
    public Mono<Boolean> isExecutionAllowed(Event event, List<Event> scheduledEvents) {
        logger.info("Evaluating Resource Updater policy for Event with Action: Type={} entityId={} start={}",
                event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime());

        List<Event> timeline = scheduledEvents.stream()
                .filter(scheduledEvent -> scheduledEvent.getAction().getActionClass() == ActionClass.Resource)
                .sorted(Comparator.comparing(Event::getEventStartDateTime))
                .toList();

        Event eventBefore = this.getEventBefore(event, timeline);
        ActionType actionType = event.getAction().getActionType();

        Mono<Boolean> result;

        if (actionType == ActionType.CREATE) {
            if (eventBefore != null && eventBefore.getAction().getActionType() != ActionType.DELETE) {
                return Mono.error(new UpdaterPolicyViolationException(event, eventBefore,
                        "Cannot schedule a resource CREATE action because a CREATE action is only allowed after a DELETE action."));
            }

            result = resourceRepository.existsById(event.getEntityId())
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new UpdaterPolicyViolationException(event,
                                    "Cannot schedule a resource CREATE action because the specified resource will already exist in the database."));
                        }
                        return Mono.just(true);
                    });

        } else if (actionType == ActionType.READ) {
            if (eventBefore != null && eventBefore.getAction().getActionType() == ActionType.DELETE) {
                return Mono.error(new UpdaterPolicyViolationException(event, eventBefore,
                        "Cannot schedule a resource READ action because a READ action is not allowed after a DELETE action."));
            }

            result = resourceRepository.existsById(event.getEntityId())
                    .flatMap(exists -> {
                        if (!exists) {
                            return Mono.error(new UpdaterPolicyViolationException(event,
                                    "Cannot schedule a resource READ action because the specified resource will not exist in the database."));
                        }
                        return Mono.just(true);
                    });

        } else if (actionType == ActionType.UPDATE) {
            if (eventBefore != null && eventBefore.getAction().getActionType() == ActionType.DELETE) {
                return Mono.error(new UpdaterPolicyViolationException(event, eventBefore,
                        "UPDATE action is not allowed after a DELETE action."));
            }

            result = getPreviousStatusReactive(event, timeline)
                    .flatMap(previousStatus -> {
                        Tuple<ResourceStatus, Event> nextStatus = this.getNextStatus(event, timeline);
                        ResourceStatus statusToUpdate = ((ResourceUpdateAction) event.getAction()).getResourceToUpdate().getStatus();

                        if (!this.transitionValidator.isTransitionAllowed(previousStatus.getFirst(), statusToUpdate)) {
                            String reason = previousStatus.getSecond() == null
                                    ? String.format("Transition from %s to %s not allowed. Previous status from database record.",
                                    previousStatus.getFirst(), statusToUpdate)
                                    : String.format("Transition from %s to %s not allowed. Previous status set by event at %s",
                                    previousStatus.getFirst(), statusToUpdate, previousStatus.getSecond().getEventStartDateTime());

                            return Mono.error(new UpdaterPolicyViolationException(event, previousStatus.getSecond(), reason));
                        }

                        if (!this.transitionValidator.isTransitionAllowed(statusToUpdate, nextStatus.getFirst())) {
                            return Mono.error(new UpdaterPolicyViolationException(event, nextStatus.getSecond(),
                                    String.format("Cannot update to %s: Conflicts with scheduled transition to %s at %s",
                                            statusToUpdate, nextStatus.getFirst(), nextStatus.getSecond().getEventStartDateTime())));
                        }

                        return Mono.just(true);
                    });

        } else if (actionType == ActionType.DELETE) {
            result = getPreviousStatusReactive(event, timeline)
                    .flatMap(currentStatus -> {
                        if (!this.statusBasedOperationValidator.isDeletionAllowed(currentStatus.getFirst())) {
                            return Mono.error(new UpdaterPolicyViolationException(event, currentStatus.getSecond(),
                                    String.format("Cannot delete resource %s: Current status %s does not allow deletion.",
                                            event.getEntityId(), currentStatus.getFirst())));
                        }
                        return Mono.just(true);
                    });

        } else {
            result = Mono.just(false); // Unknown or unsupported action
        }

        return result.doOnSuccess(decision -> logger.info(
                "Decision of Updater policy for Event with Action: Type={}, entityId={}, start={} is: {}",
                actionType, event.getEntityId(), event.getEventStartDateTime(), decision ? "ALLOWED" : "FORBIDDEN"));
    }

    private Mono<Tuple<ResourceStatus, Event>> getPreviousStatusReactive(Event event, List<Event> timeline) {
        Event previousUpdateEvent = this.getPreviousEventByActionType(event, timeline, ActionType.UPDATE);
        if (previousUpdateEvent == null) {
            return resourceRepository.findById(event.getEntityId())
                    .map(currentResource -> new Tuple<>(currentResource.getStatus(), (Event) null))
                    .switchIfEmpty(Mono.<Tuple<ResourceStatus, Event>>defer(() -> {
                        Event creationEvent = this.getPreviousEventByActionType(event, timeline, ActionType.CREATE);
                        if (creationEvent == null) {
                            return Mono.error(new UpdaterPolicyViolationException(event,
                                    String.format("Cannot get previous status for resource %s: " +
                                                    "The resource does not exist in the database and no creation event was found in the timeline.",
                                            event.getEntityId())
                            ));
                        } else {
                            Event deletionEvent = this.getPreviousEventByActionType(event, timeline, ActionType.DELETE);
                            if (deletionEvent != null) {
                                if (deletionEvent.getEventStartDateTime().isAfter(creationEvent.getEventStartDateTime())) {
                                    return Mono.error(new UpdaterPolicyViolationException(event,
                                            deletionEvent,
                                            String.format("Cannot get previous status for resource %s: " +
                                                            "Invalid timeline detected - found creation event at %s but it's followed by a deletion at %s. " +
                                                            "Deletion must come before creation to determine previous status.",
                                                    event.getEntityId(),
                                                    creationEvent.getEventStartDateTime(),
                                                    deletionEvent.getEventStartDateTime())
                                    ));
                                }
                            }
                            // dans les deux cas (deletion null ou pas après creation), on retourne le status du creationEvent
                            ResourceStatus previousStatus = ((ResourceCreationAction) creationEvent.getAction()).getResourceToSave().getStatus();
                            return Mono.just(new Tuple<>(previousStatus, creationEvent));
                        }
                    }));

        } else {
            ResourceStatus previousStatus = ((ResourceUpdateAction) previousUpdateEvent.getAction()).getResourceToUpdate().getStatus();
            return Mono.just(new Tuple<>(previousStatus, previousUpdateEvent));
        }
    }

    private Tuple<ResourceStatus, Event> getPreviousStatus(Event event, List<Event> timeline) {
        return getPreviousStatusReactive(event, timeline).block();
    }

    private Tuple<ResourceStatus, Event> getNextStatus(Event event, List<Event> timeline) {
        ResourceStatus statusToUpdate = ((ResourceUpdateAction) event.getAction()).getResourceToUpdate().getStatus();
        Event nextUpdateEvent = this.getNextEventByActionType(event, timeline, ActionType.UPDATE);
        if (nextUpdateEvent == null) {
            return new Tuple<>(statusToUpdate, event);
        } else {
            ResourceStatus nextStatus = ((ResourceUpdateAction) nextUpdateEvent.getAction()).getResourceToUpdate().getStatus();
            return new Tuple<>(nextStatus, nextUpdateEvent);
        }
    }

    private Event getEventBefore(Event event, List<Event> timeline) {
        return timeline.stream()
                .filter(e -> e.getEventStartDateTime().isBefore(event.getEventStartDateTime()))
                .max(Comparator.comparing(Event::getEventStartDateTime))
                .orElse(null);
    }

    private List<Event> getAllEventsBefore(Event event, List<Event> timeline) {
        return timeline.stream()
                .filter(e -> e.getEventStartDateTime().isBefore(event.getEventStartDateTime()))
                .toList();
    }

    private Event getPreviousEventByActionType(Event event, List<Event> timeline, ActionType actionType) {
        return this.getAllEventsBefore(event, timeline)
                .stream()
                .filter(e -> e.getAction().getActionType() == actionType)
                .max(Comparator.comparing(Event::getEventStartDateTime))
                .orElse(null);
    }

    private List<Event> getAllEventsAfter(Event event, List<Event> timeline) {
        return timeline.stream()
                .filter(e -> e.getEventStartDateTime().isAfter(event.getEventStartDateTime()))
                .toList();
    }

    private Event getNextEventByActionType(Event event, List<Event> timeline, ActionType actionType) {
        return this.getAllEventsAfter(event, timeline)
                .stream()
                .filter(e -> e.getAction().getActionType() == actionType)
                .min(Comparator.comparing(Event::getEventStartDateTime))
                .orElse(null);
    }
}