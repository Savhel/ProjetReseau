package yowyob.resource.management.actions.service.operations;

import java.util.UUID;
import java.util.Optional;

import lombok.Getter;

import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import org.springframework.data.cassandra.repository.CassandraRepository;


@Getter
public class ServiceCustomAction extends ServiceAction {
    private final String query;

    public ServiceCustomAction(UUID entityId, String query) {
        super(entityId, ActionType.CUSTOM);
        this.query = query;
    }

    @Override
    public Optional<?> execute(CassandraRepository<?, ?> repository) {
        System.out.println("Executing custom action with query: " + query);
        return Optional.empty();
    }
}
