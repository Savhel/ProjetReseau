package yowyob.resource.management.actions.service.operations;

import java.util.UUID;
import java.util.Optional;

import lombok.Getter;
import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceUpdateAction extends ServiceAction {
    private final Service serviceToUpdate;

    public ServiceUpdateAction(Service serviceToUpdate) {
        super(serviceToUpdate.getId(), ActionType.UPDATE);
        this.serviceToUpdate = serviceToUpdate;
    }

    @Override
    public Optional<Service> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        Service updatedService = serviceRepository.insert(this.serviceToUpdate);
        return Optional.of(updatedService);
    }
}
