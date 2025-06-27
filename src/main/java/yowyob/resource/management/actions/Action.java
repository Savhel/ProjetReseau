package yowyob.resource.management.actions;

import lombok.Getter;

import java.util.Optional;
import java.util.UUID;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.commons.Command;


@Getter
@Setter
public abstract class Action implements Command {
    protected final UUID entityId;
    protected final ActionType actionType;
    protected final ActionClass actionClass;
    private static final Logger logger = LoggerFactory.getLogger(Action.class);

    public Action(UUID entityId, ActionType actionType, ActionClass actionClass) {
        this.entityId = entityId;
        this.actionType = actionType;
        this.actionClass = actionClass;

        logger.info("New Action generated : Type={}, Class={}, entityId={}", actionType, actionClass, entityId);
    }

    public abstract Mono<?> execute(ReactiveCassandraRepository<?, ?> repository);
}