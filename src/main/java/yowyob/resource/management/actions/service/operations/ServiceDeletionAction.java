package yowyob.resource.management.actions.service.operations;

import lombok.Getter;
import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;

import java.util.Optional;
import java.util.UUID;

@Getter
public class ServiceDeletionAction extends ServiceAction {

    public ServiceDeletionAction(UUID entityID) {
        super(entityID, ActionType.DELETE);
    }

    @Override
    public Optional<?> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        serviceRepository.deleteById(this.getEntityId());
        return Optional.empty();
    }
}
