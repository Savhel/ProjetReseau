package yowyob.resource.management.services.policy.executors;

import java.util.Optional;

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
    public boolean isExecutionAllowed(Action action) throws ExecutorPolicyViolationException {
        logger.info("Evaluating execution policy for Action: Type={}, entityId={}",
                action.getActionType(), action.getEntityId());

        ResourceAction resourceAction = (ResourceAction) action;

        boolean decision = switch (resourceAction.getActionType()) {
            case CREATE -> {
                Optional<Resource> resource = this.resourceRepository.findById(resourceAction.getEntityId());
                yield resource.isEmpty();
            }

            case READ -> {
                Optional<Resource> resource = this.resourceRepository.findById(resourceAction.getEntityId());
                yield resource.isPresent();
            }

            case UPDATE -> {
                ResourceUpdateAction resourceUpdateAction = (ResourceUpdateAction) resourceAction;
                Optional<Resource> currentResource = resourceRepository.findById(resourceAction.getEntityId());

                if (currentResource.isEmpty()) {
                    throw new ExecutorPolicyViolationException(action, "Resource not found");
                }

                ResourceStatus targetStatus = resourceUpdateAction.getResourceToUpdate().getStatus();
                ResourceStatus currentStatus = currentResource.get().getStatus();
                if (!this.transitionValidator.isTransitionAllowed(currentStatus, targetStatus)) {
                    throw new ExecutorPolicyViolationException(action,
                            String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
                }

                yield true;
            }

            case DELETE -> {
                Optional<Resource> currentResource = resourceRepository.findById(resourceAction.getEntityId());
                if (currentResource.isEmpty()) {
                    throw new ExecutorPolicyViolationException(action, "Resource not found.");
                }

                yield this.statusBasedOperationValidator.isDeletionAllowed(currentResource.get().getStatus());
            }

            default -> false;
        };

        logger.info("Decision of Execution policy for Action: Type={}, entityId={} is: {}",
                resourceAction.getActionType(), resourceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN");

        return decision;
    }
}
