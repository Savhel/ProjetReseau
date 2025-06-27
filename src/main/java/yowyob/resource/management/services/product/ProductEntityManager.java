package yowyob.resource.management.services.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.invalid.InvalidEventClassException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.services.service.ServiceEntityManager;
import yowyob.resource.management.exceptions.invalid.InvalidActionClassException;
import yowyob.resource.management.services.resource.ResourceEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ProductEntityManager {
    private final ServiceEntityManager serviceEntityManager;
    private final ResourceEntityManager resourceEntityManager;
    private static final Logger logger = LoggerFactory.getLogger(ProductEntityManager.class);

    @Autowired
    public ProductEntityManager(ServiceEntityManager serviceEntityManager, ResourceEntityManager resourceEntityManager) {
        this.serviceEntityManager = serviceEntityManager;
        this.resourceEntityManager = resourceEntityManager;
    }

    public Mono<?> executeAction(Action action) throws InvalidActionClassException, ExecutorPolicyViolationException {
        logger.info("Received Action      : Type={}, Class={}, entityId={}",
                action.getActionType(),
                action.getActionClass(),
                action.getEntityId());

        return switch (action.getActionClass()) {
            case ActionClass.Resource -> this.resourceEntityManager.executeAction((ResourceAction) action);
            case ActionClass.Service -> this.serviceEntityManager.executeAction((ServiceAction) action);
            default -> Mono.error(new InvalidActionClassException(action));
        };
    }

    public Mono<Void> scheduleEvent(LocalDateTime eventStartDateTime, Action action) throws InvalidEventClassException, UpdaterPolicyViolationException {
        logger.info("Scheduling action: {} of Class: {} for entityId: {} at : {}",
                action.getActionType(),
                action.getActionClass(),
                action.getEntityId(),
                eventStartDateTime);

        return switch (action.getActionClass()) {
            case ActionClass.Resource -> this.resourceEntityManager.triggerResourceEvent((ResourceAction) action, eventStartDateTime);
            case ActionClass.Service -> this.serviceEntityManager.triggerServiceEvent((ServiceAction) action, eventStartDateTime);
            default -> Mono.error(new InvalidEventClassException(action, eventStartDateTime));
        };
    }
}
