package yowyob.resource.management.events.service;

import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.enums.EventClass;

import java.time.LocalDateTime;
import java.util.UUID;

public class ServiceEvent extends Event {
    public ServiceEvent(Object source, ServiceAction serviceAction, LocalDateTime eventStartDateTime) {
        super(source, serviceAction.getEntityId(), serviceAction, EventClass.Service, eventStartDateTime);
    }
}