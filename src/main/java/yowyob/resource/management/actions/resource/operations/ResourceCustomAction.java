package yowyob.resource.management.actions.resource.operations;

import java.util.UUID;
import java.util.Optional;

import lombok.Getter;

import org.springframework.data.cassandra.repository.CassandraRepository;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;

@Getter
public class ResourceCustomAction extends ResourceAction {
    private final String query;

    public ResourceCustomAction(UUID entityId, String query) {
        super(entityId, ActionType.CUSTOM);
        this.query = query;
    }

    @Override
    public Optional<?> execute(CassandraRepository<?, ?> repository) {
        System.out.println("Executing custom action with query: " + query);
        return Optional.empty();
    }
}
