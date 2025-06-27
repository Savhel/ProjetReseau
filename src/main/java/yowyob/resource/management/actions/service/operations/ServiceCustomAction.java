package yowyob.resource.management.actions.service.operations;

import java.util.UUID;

import lombok.Getter;
import reactor.core.publisher.Mono;

import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.service.ServiceAction;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;


@Getter
public class ServiceCustomAction extends ServiceAction {
    private final String query;

    public ServiceCustomAction(UUID entityId, String query) {
        super(entityId, ActionType.CUSTOM, ActionClass.Service);
        this.query = query;
    }

    @Override
    public Mono<?> execute(ReactiveCassandraRepository<?, ?> repository) {
        System.out.println("Executing custom action with query: " + query);
        return Mono.empty();
    }
}
