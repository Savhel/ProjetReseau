package yowyob.resource.management.actions.resource;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;

public abstract class ResourceAction extends Action {

    public ResourceAction(UUID entityId, ActionType actionType) {
        super(entityId, actionType, ActionClass.Resource);
    }
}
