package yowyob.resource.management.services.policy.executors;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.services.interfaces.policies.ExecutorPolicy;
import yowyob.resource.management.models.resource.enums.ResourceStatus;
import yowyob.resource.management.repositories.resource.ResourceRepository;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;
import yowyob.resource.management.services.policy.validators.transition.ResourceTransitionValidator;
import yowyob.resource.management.services.policy.validators.operations.ResourceStatusBasedOperationValidator;


@Component
public class ResourceExecutorPolicy implements ExecutorPolicy {
    private final ResourceRepository resourceRepository;
    private final ResourceTransitionValidator transitionValidator;
    private final ResourceStatusBasedOperationValidator statusBasedOperationValidator;
    private static final Logger logger = LoggerFactory.getLogger(ResourceExecutorPolicy.class);

    @Autowired
    public ResourceExecutorPolicy(ResourceRepository resourceRepository, ResourceTransitionValidator transitionValidator, ResourceStatusBasedOperationValidator statusBasedOperationValidator) {
        this.resourceRepository = resourceRepository;
        this.transitionValidator = transitionValidator;
        this.statusBasedOperationValidator = statusBasedOperationValidator;
    }

    @Override
    public Mono<Boolean> isExecutionAllowed(Action action) {
        logger.info("Evaluating execution policy for Action: Type={}, entityId={}",
                action.getActionType(), action.getEntityId());

        ResourceAction resourceAction = (ResourceAction) action;

        return switch (resourceAction.getActionType()) {
            case CREATE -> this.resourceRepository.findById(resourceAction.getEntityId())
                    .map(resource -> false) // Resource exists, creation not allowed
                    .defaultIfEmpty(true) // Resource doesn't exist, creation allowed
                    .doOnSuccess(decision -> logger.info("CREATE decision for entityId={}: {}", 
                            resourceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            case READ -> this.resourceRepository.findById(resourceAction.getEntityId())
                    .map(resource -> true) // Resource exists, read allowed
                    .defaultIfEmpty(false) // Resource doesn't exist, read not allowed
                    .doOnSuccess(decision -> logger.info("READ decision for entityId={}: {}", 
                            resourceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            case UPDATE -> {
                ResourceUpdateAction resourceUpdateAction = (ResourceUpdateAction) resourceAction;
                yield this.resourceRepository.findById(resourceAction.getEntityId())
                        .switchIfEmpty(Mono.error(new ExecutorPolicyViolationException(action, "Resource not found")))
                        .flatMap(currentResource -> {
                            ResourceStatus targetStatus = resourceUpdateAction.getResourceToUpdate().getStatus();
                            ResourceStatus currentStatus = currentResource.getStatus();
                            
                            if (!this.transitionValidator.isTransitionAllowed(currentStatus, targetStatus)) {
                                return Mono.error(new ExecutorPolicyViolationException(action,
                                        String.format("Invalid status transition from %s to %s", currentStatus, targetStatus)));
                            }
                            
                            return Mono.just(true);
                        })
                        .doOnSuccess(decision -> logger.info("UPDATE decision for entityId={}: {}", 
                                resourceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));
            }

            case DELETE -> this.resourceRepository.findById(resourceAction.getEntityId())
                    .switchIfEmpty(Mono.error(new ExecutorPolicyViolationException(action, "Resource not found.")))
                    .map(currentResource -> this.statusBasedOperationValidator.isDeletionAllowed(currentResource.getStatus()))
                    .doOnSuccess(decision -> logger.info("DELETE decision for entityId={}: {}", 
                            resourceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            default -> Mono.just(false)
                    .doOnSuccess(decision -> logger.info("DEFAULT decision for entityId={}: FORBIDDEN", 
                            resourceAction.getEntityId()));
        };
    }
}
