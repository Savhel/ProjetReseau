package yowyob.resource.management.actions.resource.operations;

import java.util.Optional;

import lombok.Getter;
import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.repositories.resource.ResourceRepository;

@Getter
public class ResourceCreationAction extends ResourceAction {
    private final Resource resourceToSave;

    public ResourceCreationAction(Resource resourceToSave) {
        super(resourceToSave.getId(), ActionType.CREATE);
        this.resourceToSave = resourceToSave;
    }

    @Override
    public Optional<Resource> execute(CassandraRepository<?, ?> repository) {
        ResourceRepository resourceRepository = (ResourceRepository) repository;
        Resource savedResource = resourceRepository.save(this.resourceToSave);
        return Optional.of(savedResource);
    }
}
