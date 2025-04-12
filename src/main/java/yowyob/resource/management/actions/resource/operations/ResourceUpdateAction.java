package yowyob.resource.management.actions.resource.operations;

import java.util.UUID;
import java.util.Optional;

import lombok.Getter;

import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.repositories.resource.ResourceRepository;


@Getter
public class ResourceUpdateAction extends ResourceAction {
    private final Resource resourceToUpdate;

    public ResourceUpdateAction(Resource resourceToUpdate) {
        super(resourceToUpdate.getId(), ActionType.UPDATE);
        this.resourceToUpdate = resourceToUpdate;
    }

    @Override
    public Optional<Resource> execute(CassandraRepository<?, ?> repository) {
        ResourceRepository resourceRepository = (ResourceRepository) repository;
        Resource updatedResource = resourceRepository.insert(resourceToUpdate);
        return Optional.of(updatedResource);
    }
}