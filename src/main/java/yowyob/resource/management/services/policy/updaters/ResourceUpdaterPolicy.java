package yowyob.resource.management.services.policy.updaters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    public boolean isExecutionAllowed(Event event, List<Event> scheduledEvents) {
        logger.info("Evaluating Resource Updater policy for Event with Action : Type={} with entityId={} start={}",
                event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime());

        List<Event> timeline = scheduledEvents.stream()
                .filter(scheduledEvent -> scheduledEvent.getAction().getActionClass() == ActionClass.Resource)
                .sorted(Comparator.comparing(Event::getEventStartDateTime))
                .toList();

        Event eventBefore = this.getEventBefore(event, timeline);
        boolean decision = switch (event.getAction().getActionType()) {
            case CREATE -> {
                if (eventBefore != null && eventBefore.getAction().getActionType() != ActionType.DELETE) {
                    throw new UpdaterPolicyViolationException(event,
                            eventBefore,
                            "Cannot schedule a resource CREATE action because a CREATE action is only allowed after a DELETE action."
                    );
                }

                if (this.resourceRepository.existsById(event.getEntityId())) {
                    throw new UpdaterPolicyViolationException(event,
                            "Cannot schedule a resource CREATE action because the specified resource will already exists in the database."
                    );
                }
                yield true;
            }

            case READ -> {
                if (eventBefore != null && eventBefore.getAction().getActionType() == ActionType.DELETE) {
                    throw new UpdaterPolicyViolationException(event,
                            eventBefore,
                            "Cannot schedule a resource READ action because a READ action is not allowed after a DELETE action."
                    );
                }

                if (!this.resourceRepository.existsById(event.getEntityId())) {
                    throw new UpdaterPolicyViolationException(event,
                            "Cannot schedule a resource READ action at %s because the specified resource will not exists in the database."
                    );
                }
                yield true;
            }

            case UPDATE -> {
                if (eventBefore != null && eventBefore.getAction().getActionType() == ActionType.DELETE) {
                    throw new UpdaterPolicyViolationException(event,
                            eventBefore,
                            "UPDATE action is not allowed after a DELETE action."
                    );
                }

                Tuple<ResourceStatus, Event> nextStatus = this.getNextStatus(event, timeline);
                Tuple<ResourceStatus, Event> previousStatus = this.getPreviousStatus(event, timeline);
                ResourceStatus statusToUpdate = ((ResourceUpdateAction) event.getAction()).getResourceToUpdate().getStatus();

                if (!this.transitionValidator.isTransitionAllowed(previousStatus.getFirst(), statusToUpdate)) {
                    if (previousStatus.getSecond() == null) {
                        throw new UpdaterPolicyViolationException(
                                event,
                                String.format("Transition from %s to %s not allowed. %s",
                                        previousStatus.getFirst(),
                                        statusToUpdate,
                                        "Previous status from database record.")
                        );
                    } else {
                        throw new UpdaterPolicyViolationException(
                                event,
                                previousStatus.getSecond(),
                                String.format("Transition from %s to %s not allowed. %s",
                                        previousStatus.getFirst(),
                                        statusToUpdate,
                                        String.format("Previous status set by event at %s", previousStatus.getSecond().getEventStartDateTime())
                                )
                        );
                    }
                }
                if (!this.transitionValidator.isTransitionAllowed(statusToUpdate, nextStatus.getFirst())) {
                    throw new UpdaterPolicyViolationException(
                            event,
                            nextStatus.getSecond(),
                            String.format("Cannot update to %s: Conflicts with scheduled transition to %s at %s",
                                    statusToUpdate,
                                    nextStatus.getFirst(),
                                    nextStatus.getSecond().getEventStartDateTime())
                    );
                }

                yield true;
            }

            case DELETE -> {
                Tuple<ResourceStatus, Event> currentStatus = this.getPreviousStatus(event, timeline);
                if (!this.statusBasedOperationValidator.isDeletionAllowed(currentStatus.getFirst())) {
                    throw new UpdaterPolicyViolationException(
                            event,
                            currentStatus.getSecond(),
                            String.format("Cannot delete resource %s: Current status %s does not allow deletion.",
                                    event.getEntityId(),
                                    currentStatus)
                    );
                }

                yield true;
            }

            default -> false;
        };

        logger.info("Decision of Updater policy for Event with Action: Type={}, entityId={}, start{} is: {}",
                event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime(), decision ? "ALLOWED" : "FORBIDDEN");
        return decision;
    }

    private Tuple<ResourceStatus, Event> getPreviousStatus(Event event, List<Event> timeline) {
        Event previousUpdateEvent = this.getPreviousEventByActionType(event, timeline, ActionType.UPDATE);
        if (previousUpdateEvent == null) {
            Resource currentResource = this.resourceRepository.findById(event.getEntityId()).orElse(null);
            if (currentResource == null) {
                Event creationEvent = this.getPreviousEventByActionType(event, timeline, ActionType.CREATE);
                if (creationEvent == null) {
                    throw new UpdaterPolicyViolationException(event,
                            String.format("Cannot get previous status for resource %s: " +
                                            "The resource does not exist in the database and no creation event was found in the timeline.",
                                    event.getEntityId())
                    );
                } else {
                    Event deletionEvent = this.getPreviousEventByActionType(event, timeline, ActionType.DELETE);
                    if (deletionEvent != null) {
                        if (deletionEvent.getEventStartDateTime().isAfter(creationEvent.getEventStartDateTime())) {
                            throw new UpdaterPolicyViolationException(event,
                                    deletionEvent,
                                    String.format("Cannot get previous status for resource %s: " +
                                                    "Invalid timeline detected - found creation event at %s but it's followed by a deletion at %s. " +
                                                    "Deletion must come before creation to determine previous status.",
                                            event.getEntityId(),
                                            creationEvent.getEventStartDateTime(),
                                            deletionEvent.getEventStartDateTime())
                            );
                        } else {
                            ResourceStatus previousStatus = ((ResourceCreationAction) creationEvent.getAction()).getResourceToSave().getStatus();
                            return new Tuple<>(previousStatus, creationEvent);
                        }
                    } else {
                        ResourceStatus previousStatus = ((ResourceCreationAction) creationEvent.getAction()).getResourceToSave().getStatus();
                        return new Tuple<>(previousStatus, creationEvent);
                    }
                }
            } else {
                return new Tuple<>(currentResource.getStatus(), null);
            }
        } else {
            ResourceStatus previousStatus = ((ResourceUpdateAction) previousUpdateEvent.getAction()).getResourceToUpdate().getStatus();
            return new Tuple<>(previousStatus, previousUpdateEvent);
        }
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