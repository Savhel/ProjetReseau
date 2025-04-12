package yowyob.resource.management.services.policy.executors;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.models.service.enums.ServiceStatus;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.services.interfaces.policies.ExecutorPolicy;
import yowyob.resource.management.services.policy.validators.operations.ServiceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ServiceTransitionValidator;

@Component
public class ServiceExecutorPolicy implements ExecutorPolicy {

    private final ServiceRepository serviceRepository;
    private final ServiceTransitionValidator transitionValidator;
    private final ServiceStatusBasedOperationValidator statusValidator;
    private final static Logger logger = LoggerFactory.getLogger(ServiceExecutorPolicy.class);

    @Autowired
    public ServiceExecutorPolicy(ServiceRepository serviceRepository, ServiceTransitionValidator transitionValidator, ServiceStatusBasedOperationValidator statusValidator) {
        this.serviceRepository = serviceRepository;
        this.transitionValidator = transitionValidator;
        this.statusValidator = statusValidator;
    }

    @Override
    public boolean isExecutionAllowed(Action action) throws ExecutorPolicyViolationException {
        logger.info("Evaluating execution policy for Action: {} with entityId: {}",
                action.getActionType(), action.getEntityId());

        ServiceAction serviceAction = (ServiceAction) action;

        boolean decision = switch (serviceAction.getActionType()) {
            case CREATE -> {
                Optional<Service> service = this.serviceRepository.findById(serviceAction.getEntityId());
                yield service.isEmpty();
            }

            case READ -> {
                Optional<Service> service = this.serviceRepository.findById(serviceAction.getEntityId());
                yield service.isPresent();
            }

            case UPDATE -> {
                ServiceUpdateAction serviceUpdateAction = (ServiceUpdateAction) serviceAction;
                Optional<Service> currentService = serviceRepository.findById(serviceAction.getEntityId());

                if (currentService.isEmpty()) {
                    throw new ExecutorPolicyViolationException(action, "Service not found");
                }

                ServiceStatus targetStatus = serviceUpdateAction.getServiceToUpdate().getStatus();
                ServiceStatus currentStatus = currentService.get().getStatus();
                if (!this.transitionValidator.isTransitionAllowed(currentStatus, targetStatus)) {
                    throw new ExecutorPolicyViolationException(action,
                            String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
                }

                yield true;
            }

            case DELETE -> {
                Optional<Service> currentService = serviceRepository.findById(serviceAction.getEntityId());
                if (currentService.isEmpty()) {
                    throw new ExecutorPolicyViolationException(action, "Service not found.");
                }

                yield this.statusValidator.isDeletionAllowed(currentService.get().getStatus());
            }

            default -> false;
        };

        logger.info("Execution policy decision for Action: {} with entityId: {} is: {}",
                serviceAction.getActionType(), serviceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN");

        return decision;
    }
}
