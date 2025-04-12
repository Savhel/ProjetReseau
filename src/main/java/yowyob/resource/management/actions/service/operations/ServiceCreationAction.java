package yowyob.resource.management.actions.service.operations;

import java.util.Optional;

import lombok.Getter;

import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;
import org.springframework.data.cassandra.repository.CassandraRepository;

@Getter
public class ServiceCreationAction extends ServiceAction {
    private final Service serviceToSave;
    
    public ServiceCreationAction(Service service) {
        super(service.getId(), ActionType.CREATE);
        this.serviceToSave = service;
    }

    @Override
    public Optional<Service> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        Service service = serviceRepository.save(this.serviceToSave);
        return Optional.of(service);
    }
}
