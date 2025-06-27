package yowyob.resource.management.actions.service.operations;

import reactor.core.publisher.Mono;

import lombok.Getter;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceUpdateAction extends ServiceAction {
    private final Services servicesToUpdate;

    public ServiceUpdateAction(Services servicesToUpdate) {
        super(servicesToUpdate.getId(), ActionType.UPDATE, ActionClass.Service);
        this.servicesToUpdate = servicesToUpdate;
    }

    @Override
    public Mono<Services> execute(ReactiveCassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        return serviceRepository.save(this.servicesToUpdate);
    }
}
