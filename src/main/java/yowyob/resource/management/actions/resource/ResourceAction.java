package yowyob.resource.management.actions.resource;

import java.util.UUID;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;

public abstract class ResourceAction extends Action {

    public ResourceAction(UUID entityId, ActionType actionType, ActionClass resource) {
        super(entityId, actionType, ActionClass.Resource);
    }
}
