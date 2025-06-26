package yowyob.resource.management.actions.service.operations;

import java.util.Optional;

import lombok.Getter;

import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;
import org.springframework.data.cassandra.repository.CassandraRepository;

@Getter
public class ServiceCreationAction extends ServiceAction {
    private final Services servicesToSave;
    
    public ServiceCreationAction(Services services) {
        super(services.getId(), ActionType.CREATE);
        this.servicesToSave = services;
    }

    @Override
    public Optional<Services> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        Services services = serviceRepository.save(this.servicesToSave);
        return Optional.of(services);
    }
}
