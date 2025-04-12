package yowyob.resource.management.actions.service.operations;

import java.util.UUID;
import java.util.Optional;

import lombok.Getter;

import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceReadingAction extends ServiceAction {

    public ServiceReadingAction(UUID entityId) {
        super(entityId, ActionType.READ);
    }

    @Override
    public Optional<Service> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        Service readService = serviceRepository.findById(this.getEntityId()).get();
        return Optional.of(readService);
    }
}
