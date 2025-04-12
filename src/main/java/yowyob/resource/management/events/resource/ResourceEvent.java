package yowyob.resource.management.events.resource;

import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.enums.EventClass;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResourceEvent extends Event {
    public ResourceEvent(Object source, ResourceAction resourceAction, LocalDateTime eventStartDateTime) {
        super(source, resourceAction.getEntityId(), resourceAction, EventClass.Resource, eventStartDateTime);
    }
}
