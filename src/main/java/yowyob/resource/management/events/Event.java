package yowyob.resource.management.events;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yowyob.resource.management.actions.Action;
import org.springframework.context.ApplicationEvent;
import yowyob.resource.management.commons.Command;
import yowyob.resource.management.events.enums.EventClass;
import yowyob.resource.management.exceptions.invalid.InvalidEventException;

import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
public abstract class Event extends ApplicationEvent implements Command {
    protected final UUID entityId;
    protected final Action action;
    protected final EventClass eventClass;
    private final LocalDateTime eventStartDateTime;
    private static final Logger logger = LoggerFactory.getLogger(Event.class);

    public Event(Object source, UUID entityId, Action action, EventClass eventClass, LocalDateTime eventStartDateTime) {
        super(source);

        if (eventStartDateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidEventException(
                    String.format("Cannot schedule event: Start date and time (%s) must be in the future.",
                            eventStartDateTime)
            );
        }

        this.action = action;
        this.entityId = entityId;
        this.eventClass = eventClass;
        this.eventStartDateTime = eventStartDateTime;
        logger.info("New Event generated : EntityId={}, ActionType={}, EventClass={}, start={}",
                entityId, action.getActionType(), eventClass, eventStartDateTime);
    }
}