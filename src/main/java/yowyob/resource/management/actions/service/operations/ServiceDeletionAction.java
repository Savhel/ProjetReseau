package yowyob.resource.management.actions.service.operations;

import lombok.Getter;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.repositories.service.ServiceRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Getter
public class ServiceDeletionAction extends ServiceAction {

    public ServiceDeletionAction(UUID serviceId) {
        super(serviceId, ActionType.DELETE, ActionClass.Service);
    }

    @Override
    public Mono<Void> execute(ReactiveCassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        return serviceRepository.deleteById(this.getEntityId());
    }
}
