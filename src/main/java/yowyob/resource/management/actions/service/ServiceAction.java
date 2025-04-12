package yowyob.resource.management.actions.service;

import java.util.UUID;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.repositories.service.ServiceRepository;


public abstract class ServiceAction extends Action {
    public ServiceAction(UUID entityId, ActionType actionType) {
        super(entityId, actionType, ActionClass.Service);
    }
}