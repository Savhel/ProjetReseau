package yowyob.resource.management.actions.resource.operations;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.repositories.resource.ResourceRepository;

import java.util.UUID;

@Getter
public class ResourceDeletionAction extends ResourceAction {

    public ResourceDeletionAction(UUID entityId) {
        super(entityId, ActionType.DELETE, ActionClass.Resource);
    }

    @Override
    public Mono<Void> execute(ReactiveCassandraRepository<?, ?> repository) {
        ResourceRepository resourceRepository = (ResourceRepository) repository;
        return resourceRepository.deleteById(this.getEntityId());
    }
}
