package yowyob.resource.management.actions.service.operations;

import java.util.Optional;

import lombok.Getter;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.repositories.service.ServiceRepository;

@Getter
public class ServiceUpdateAction extends ServiceAction {
    private final Services servicesToUpdate;

    public ServiceUpdateAction(Services servicesToUpdate) {
        super(servicesToUpdate.getId(), ActionType.UPDATE);
        this.servicesToUpdate = servicesToUpdate;
    }

    @Override
    public Optional<Services> execute(CassandraRepository<?, ?> repository) {
        ServiceRepository serviceRepository = (ServiceRepository) repository;
        Services updatedServices = serviceRepository.insert(this.servicesToUpdate);
        return Optional.of(updatedServices);
    }
}
