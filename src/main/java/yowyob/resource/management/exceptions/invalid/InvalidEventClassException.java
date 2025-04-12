package yowyob.resource.management.exceptions.invalid;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.events.Event;

import java.time.LocalDateTime;

public class InvalidEventClassException extends InvalidInputException {
    public InvalidEventClassException(Event event) {
        super("Invalid event class: "
                +event.getEventClass()
                +" for action type: "
                +event.getAction().getActionType()
                +" scheduled At :"
                +event.getEventStartDateTime());
    }

    public InvalidEventClassException(Action action, LocalDateTime startDateTime) {
        super("Invalid action class found inside Event, ActionClass=" + action.getActionClass()
                +" start=" + startDateTime
        );
    }
}