package yowyob.resource.management.services.policy.updaters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.helpers.Tuple;
import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.models.service.enums.ServiceStatus;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.interfaces.policies.UpdaterPolicy;
import yowyob.resource.management.services.policy.validators.operations.ServiceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ServiceTransitionValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class ServiceUpdaterPolicy implements UpdaterPolicy {
    private final ServiceRepository serviceRepository;
    private final ServiceTransitionValidator transitionValidator;
    private final ServiceStatusBasedOperationValidator statusBasedOperationValidator;
    private static final Logger logger = LoggerFactory.getLogger(ServiceUpdaterPolicy.class);

    @Autowired
    public ServiceUpdaterPolicy(ServiceRepository serviceRepository,
                                ServiceTransitionValidator transitionValidator,
                                ServiceStatusBasedOperationValidator statusBasedOperationValidator) {
        this.serviceRepository = serviceRepository;
        this.transitionValidator = transitionValidator;
        this.statusBasedOperationValidator = statusBasedOperationValidator;
    }

    @Override
    public boolean isExecutionAllowed(Event event, List<Event> scheduledEvents) {
        logger.info("Evaluating Service Updater policy for Event with Action : {} with entityId: {} at {}",
                event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime());

        List<Event> timeline = scheduledEvents.stream()
                .filter(scheduledEvent -> scheduledEvent.getAction().getActionClass() == ActionClass.Service)
                .sorted(Comparator.comparing(Event::getEventStartDateTime))
                .toList();

        Event eventBefore = this.getEventBefore(event, timeline);
        boolean decision = switch (event.getAction().getActionType()) {
            case CREATE -> {
                if (eventBefore != null && eventBefore.getAction().getActionType() != ActionType.DELETE) {
                    throw new UpdaterPolicyViolationException(event,
                            eventBefore,
                            "Cannot schedule a service CREATE action because a CREATE action is only allowed after a DELETE action."
                    );
                }

                if (this.serviceRepository.existsById(event.getEntityId())) {
                    throw new UpdaterPolicyViolationException(event,
                            "Cannot schedule a service CREATE action because the specified service will already exists in the database."
                    );
                }

                yield true;
            }

            case READ -> {
                if (eventBefore != null && eventBefore.getAction().getActionType() == ActionType.DELETE) {
                    throw new UpdaterPolicyViolationException(event,
                            eventBefore,
                            "Cannot schedule a service READ action because a READ action is not allowed after a DELETE action."
                    );
                }

                if (!this.serviceRepository.existsById(event.getEntityId())) {
                    throw new UpdaterPolicyViolationException(event,
                            "Cannot schedule a service READ action at %s because the specified service will not exists in the database."
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

                Tuple<ServiceStatus, Event> nextStatus = this.getNextStatus(event, timeline);
                Tuple<ServiceStatus, Event> previousStatus = this.getPreviousStatus(event, timeline);
                ServiceStatus statusToUpdate = ((ServiceUpdateAction) event.getAction()).getServiceToUpdate().getStatus();

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
                Tuple<ServiceStatus, Event> currentStatus = this.getPreviousStatus(event, timeline);
                if (!this.statusBasedOperationValidator.isDeletionAllowed(currentStatus.getFirst())) {
                    throw new UpdaterPolicyViolationException(
                            event,
                            currentStatus.getSecond(),
                            String.format("Cannot delete service %s: Current status %s does not allow deletion.",
                                    event.getEntityId(),
                                    currentStatus)
                    );
                }

                yield true;
            }

            default -> false;
        };

        logger.info("Updater policy decision for Event with Action: {} with entityId: {} at {} is: {}",
                event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime(), decision ? "ALLOWED" : "FORBIDDEN");
        return decision;
    }

    private Tuple<ServiceStatus, Event> getPreviousStatus(Event event, List<Event> timeline) {
        Event previousUpdateEvent = this.getPreviousEventByActionType(event, timeline, ActionType.UPDATE);
        if (previousUpdateEvent == null) {
            Service currentService = this.serviceRepository.findById(event.getEntityId()).orElse(null);
            if (currentService == null) {
                Event creationEvent = this.getPreviousEventByActionType(event, timeline, ActionType.CREATE);
                if (creationEvent == null) {
                    throw new UpdaterPolicyViolationException(event,
                            String.format("Cannot get previous status for service %s: " +
                                            "The service does not exist in the database and no creation event was found in the timeline.",
                                    event.getEntityId())
                    );
                } else {
                    Event deletionEvent = this.getPreviousEventByActionType(event, timeline, ActionType.DELETE);
                    if (deletionEvent != null) {
                        if (deletionEvent.getEventStartDateTime().isAfter(creationEvent.getEventStartDateTime())) {
                            throw new UpdaterPolicyViolationException(event,
                                    deletionEvent,
                                    String.format("Cannot get previous status for service %s: " +
                                                    "Invalid timeline detected - found creation event at %s but it's followed by a deletion at %s. " +
                                                    "Deletion must come before creation to determine previous status.",
                                            event.getEntityId(),
                                            creationEvent.getEventStartDateTime(),
                                            deletionEvent.getEventStartDateTime())
                            );
                        } else {
                            ServiceStatus previousStatus = ((ServiceUpdateAction) creationEvent.getAction()).getServiceToUpdate().getStatus();
                            return new Tuple<>(previousStatus, creationEvent);
                        }
                    } else {
                        ServiceStatus previousStatus = ((ServiceUpdateAction) creationEvent.getAction()).getServiceToUpdate().getStatus();
                        return new Tuple<>(previousStatus, creationEvent);
                    }
                }
            } else {
                return new Tuple<>(currentService.getStatus(), null);
            }
        } else {
            ServiceStatus previousStatus = ((ServiceUpdateAction) previousUpdateEvent.getAction()).getServiceToUpdate().getStatus();
            return new Tuple<>(previousStatus, previousUpdateEvent);
        }
    }

    private Tuple<ServiceStatus, Event> getNextStatus(Event event, List<Event> timeline) {
        ServiceStatus statusToUpdate = ((ServiceUpdateAction) event.getAction()).getServiceToUpdate().getStatus();
        Event nextUpdateEvent = this.getNextEventByActionType(event, timeline, ActionType.UPDATE);
        if (nextUpdateEvent == null) {
            return new Tuple<>(statusToUpdate, event);
        } else {
            ServiceStatus nextStatus = ((ServiceUpdateAction) nextUpdateEvent.getAction()).getServiceToUpdate().getStatus();
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

