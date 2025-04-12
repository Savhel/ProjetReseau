package yowyob.resource.management.actions.resource.operations;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.repositories.resource.ResourceRepository;

import java.util.Optional;
import java.util.UUID;

@Getter
public class ResourceDeletionAction extends ResourceAction {

    public ResourceDeletionAction(UUID entityId) {
        super(entityId, ActionType.DELETE);
    }

    @Override
    public Optional<?> execute(CassandraRepository<?, ?> repository) {
        ResourceRepository resourceRepository = (ResourceRepository)repository;
        resourceRepository.deleteById(this.getEntityId());
        return Optional.empty();
    }
}
