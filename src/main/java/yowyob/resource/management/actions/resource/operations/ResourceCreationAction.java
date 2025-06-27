package yowyob.resource.management.actions.resource.operations;

import lombok.Getter;
import reactor.core.publisher.Mono;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.repositories.resource.ResourceRepository;

@Getter
public class ResourceCreationAction extends ResourceAction {
    private final Resource resourceToSave;

    public ResourceCreationAction(Resource resourceToSave) {
        super(resourceToSave.getId(), ActionType.CREATE, ActionClass.Resource);
        this.resourceToSave = resourceToSave;
    }

    @Override
    public Mono<Resource> execute(ReactiveCassandraRepository<?, ?> repository) {
        ResourceRepository resourceRepository = (ResourceRepository) repository;
        return resourceRepository.save(this.resourceToSave);
    }
}
