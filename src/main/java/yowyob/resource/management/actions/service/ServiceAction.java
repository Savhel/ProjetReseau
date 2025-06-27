package yowyob.resource.management.actions.service;

import java.util.UUID;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;


public abstract class ServiceAction extends Action {
    public ServiceAction(UUID entityId, ActionType actionType, ActionClass service) {
        super(entityId, actionType, ActionClass.Service);
    }
}