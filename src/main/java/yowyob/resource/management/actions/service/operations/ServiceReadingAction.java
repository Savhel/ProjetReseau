package yowyob.resource.management.actions.service.operations;

import java.util.UUID;

import lombok.Getter;
import reactor.core.publisher.Mono;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceReadingAction extends ServiceAction {

    public ServiceReadingAction(UUID entityId) {
        super(entityId, ActionType.READ, ActionClass.Service);
    }

    @Override
    public Mono<Services> execute(ReactiveCassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        return serviceRepository.findById(this.getEntityId());
    }
}
