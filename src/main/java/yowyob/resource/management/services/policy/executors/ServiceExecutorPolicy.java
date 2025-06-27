package yowyob.resource.management.services.policy.executors;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import yowyob.resource.management.services.cache.ReactiveCacheService;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.models.service.Services;
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
    private final ReactiveCacheService reactiveCacheService;
    private final static Logger logger = LoggerFactory.getLogger(ServiceExecutorPolicy.class);
    
    private static final Duration POLICY_VALIDATION_TTL = Duration.ofMinutes(10);
    private static final String POLICY_VALIDATION_PREFIX = "policy-validations:service";

    public Mono<Boolean> validateActionPolicy(Action action) {
        String cacheKey = reactiveCacheService.generateKey(POLICY_VALIDATION_PREFIX, action.getEntityId(), action.getActionType());
        logger.debug("Validating policy for action: {} with entityId: {}", action.getActionType(), action.getEntityId());
        
        return reactiveCacheService.getOrCompute(
            cacheKey,
            isExecutionAllowed(action),
            POLICY_VALIDATION_TTL,
            Boolean.class
        );
    }

    @Autowired
    public ServiceExecutorPolicy(ServiceRepository serviceRepository, ServiceTransitionValidator transitionValidator, ServiceStatusBasedOperationValidator statusValidator, ReactiveCacheService reactiveCacheService) {
        this.serviceRepository = serviceRepository;
        this.transitionValidator = transitionValidator;
        this.statusValidator = statusValidator;
        this.reactiveCacheService = reactiveCacheService;
    }

    @Override
    public Mono<Boolean> isExecutionAllowed(Action action) {
        logger.info("Evaluating execution policy for Action: {} with entityId: {}",
                action.getActionType(), action.getEntityId());

        ServiceAction serviceAction = (ServiceAction) action;

        return switch (serviceAction.getActionType()) {
            case CREATE -> this.serviceRepository.findById(serviceAction.getEntityId())
                    .map(service -> false) // Service exists, creation not allowed
                    .defaultIfEmpty(true) // Service doesn't exist, creation allowed
                    .doOnSuccess(decision -> logger.info("CREATE decision for entityId={}: {}", 
                            serviceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            case READ -> this.serviceRepository.findById(serviceAction.getEntityId())
                    .map(service -> true) // Service exists, read allowed
                    .defaultIfEmpty(false) // Service doesn't exist, read not allowed
                    .doOnSuccess(decision -> logger.info("READ decision for entityId={}: {}", 
                            serviceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            case UPDATE -> {
                ServiceUpdateAction serviceUpdateAction = (ServiceUpdateAction) serviceAction;
                yield this.serviceRepository.findById(serviceAction.getEntityId())
                        .switchIfEmpty(Mono.error(new ExecutorPolicyViolationException(action, "Service not found")))
                        .flatMap(currentService -> {
                            ServiceStatus targetStatus = serviceUpdateAction.getServicesToUpdate().getStatus();
                            ServiceStatus currentStatus = currentService.getStatus();
                            
                            if (!this.transitionValidator.isTransitionAllowed(currentStatus, targetStatus)) {
                                return Mono.error(new ExecutorPolicyViolationException(action,
                                        String.format("Invalid status transition from %s to %s", currentStatus, targetStatus)));
                            }
                            
                            return Mono.just(true);
                        })
                        .doOnSuccess(decision -> logger.info("UPDATE decision for entityId={}: {}", 
                                serviceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));
            }

            case DELETE -> this.serviceRepository.findById(serviceAction.getEntityId())
                    .switchIfEmpty(Mono.error(new ExecutorPolicyViolationException(action, "Service not found.")))
                    .map(currentService -> this.statusValidator.isDeletionAllowed(currentService.getStatus()))
                    .doOnSuccess(decision -> logger.info("DELETE decision for entityId={}: {}", 
                            serviceAction.getEntityId(), decision ? "ALLOWED" : "FORBIDDEN"));

            default -> Mono.just(false)
                    .doOnSuccess(decision -> logger.info("DEFAULT decision for entityId={}: FORBIDDEN", 
                            serviceAction.getEntityId()));
        };
    }
}
