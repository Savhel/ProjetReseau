package yowyob.resource.management.actions.resource.operations;

import java.util.UUID;

import lombok.Getter;
import reactor.core.publisher.Mono;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;

@Getter
public class ResourceCustomAction extends ResourceAction {
    private final String query;

    public ResourceCustomAction(UUID entityId, String query) {
        super(entityId, ActionType.CUSTOM, ActionClass.Resource);
        this.query = query;
    }

    @Override
    public Mono<?> execute(ReactiveCassandraRepository<?, ?> repository) {
        System.out.println("Executing custom action with query: " + query);
        return Mono.empty();
    }
}
