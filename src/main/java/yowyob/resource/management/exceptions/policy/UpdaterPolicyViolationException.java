package yowyob.resource.management.exceptions.policy;

import yowyob.resource.management.events.Event;

public class UpdaterPolicyViolationException extends PolicyViolationException {
    public UpdaterPolicyViolationException(Event event, Event conflictingEvent) {
        super(String.format(
                "Updater Policy violation detected for the entity [%s] of class [%s] in the action [%s] scheduled for [%s] : "
                        + "Conflict detected with the event in the action [%s] scheduled at [%s].",
                event.getEntityId(),
                event.getAction().getActionClass(),
                event.getAction().getActionType(),
                event.getEventStartDateTime(),
                conflictingEvent.getAction().getActionType(),
                conflictingEvent.getEventStartDateTime()
        ));
    }

    public UpdaterPolicyViolationException(Event event, Event conflictingEvent, String reason) {
        super(String.format(
                "Updater Policy violation detected for the entity [%s] of class [%s] in the action [%s] scheduled for [%s] : "
                        + "Conflict detected with the event in the action [%s] scheduled at [%s]. Reason : %s",
                event.getEntityId(),
                event.getAction().getActionClass(),
                event.getAction().getActionType(),
                event.getEventStartDateTime(),
                conflictingEvent.getAction().getActionType(),
                conflictingEvent.getEventStartDateTime(),
                reason
        ));
    }

    public UpdaterPolicyViolationException(Event event, String reason) {
        super(String.format(
            "Updater Policy violation detected for the entity [%s] of class [%s] in the action [%s] scheduled for [%s] : "
                + reason,
            event.getEntityId(),
            event.getAction().getActionClass(),
            event.getAction().getActionType(),
            event.getEventStartDateTime()
        ));
    }
}