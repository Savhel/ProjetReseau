package yowyob.resource.management.actions.service.operations;

import lombok.Getter;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import reactor.core.publisher.Mono;

import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceCreationAction extends ServiceAction {
    private final Services serviceToSave;
    
    public ServiceCreationAction(Services service) {
        super(service.getId(), ActionType.CREATE, ActionClass.Service);
        this.serviceToSave = service;
    }

    @Override
    public Mono<Services> execute(ReactiveCassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        return serviceRepository.save(this.serviceToSave);
    }
}
