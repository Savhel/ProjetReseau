package yowyob.resource.management.services.context.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import yowyob.resource.management.services.cache.ReactiveCacheService;
import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.services.service.ServiceActionExecutor;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.resource.ResourceActionExecutor;
import yowyob.resource.management.repositories.resource.ResourceRepository;
import yowyob.resource.management.exceptions.invalid.InvalidActionClassException;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.actions.service.operations.ServiceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;
import yowyob.resource.management.actions.service.operations.ServiceDeletionAction;
import yowyob.resource.management.actions.resource.operations.ResourceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceDeletionAction;

import java.util.Optional;

@org.springframework.stereotype.Service
public class ExecutorContextManager {

    private final ServiceActionExecutor serviceActionExecutor;
    private final ResourceActionExecutor resourceActionExecutor;
    private final ServiceRepository serviceRepository;
    private final ResourceRepository resourceRepository;
    private final ReactiveCacheService reactiveCacheService;

    private static final Logger logger = LoggerFactory.getLogger(ExecutorContextManager.class);
    private static final Duration REVERSE_ACTION_TTL = Duration.ofMinutes(30);
    private static final String REVERSE_ACTION_PREFIX = "reverse-actions";

    @Autowired
    public ExecutorContextManager(ServiceActionExecutor serviceActionExecutor, ResourceActionExecutor resourceActionExecutor,
                                  ServiceRepository serviceRepository, ResourceRepository resourceRepository,
                                  ReactiveCacheService reactiveCacheService) {
        this.serviceActionExecutor = serviceActionExecutor;
        this.resourceActionExecutor = resourceActionExecutor;
        this.serviceRepository = serviceRepository;
        this.resourceRepository = resourceRepository;
        this.reactiveCacheService = reactiveCacheService;
    }
    
    public void init() {
        pauseExecutor();
        logger.info("Context has been successfully initialized");
    }

    public void clear() {
        resumeExecutors();
        logger.info("Context has been successfully cleared");
    }

    public Mono<Action> generateReverseAction(Action action) {
        String cacheKey = reactiveCacheService.generateKey(REVERSE_ACTION_PREFIX, action.getEntityId(), action.getActionType());
        
        return reactiveCacheService.getOrCompute(
            cacheKey,
            computeReverseAction(action),
            REVERSE_ACTION_TTL,
            Action.class
        );
    }
    
    private Mono<Action> computeReverseAction(Action action) {
        return switch (action.getActionClass()) {
            case Resource -> {
                ResourceAction resourceAction = (ResourceAction) action;

                yield switch (resourceAction.getActionType()) {
                    case CREATE -> Mono.just(new ResourceDeletionAction(resourceAction.getEntityId()));
                    case UPDATE -> this.resourceRepository.findById(resourceAction.getEntityId())
                            .map(ResourceUpdateAction::new)
                            .cast(Action.class);
                    case DELETE -> this.resourceRepository.findById(resourceAction.getEntityId())
                            .map(ResourceCreationAction::new)
                            .cast(Action.class);
                    default -> Mono.empty();
                };
            }

            case Service -> {
                ServiceAction serviceAction = (ServiceAction) action;

                yield switch (serviceAction.getActionType()) {
                    case CREATE -> Mono.just(new ServiceDeletionAction(serviceAction.getEntityId()));
                    case UPDATE -> this.serviceRepository.findById(serviceAction.getEntityId())
                            .map(ServiceUpdateAction::new)
                            .cast(Action.class);
                    case DELETE -> this.serviceRepository.findById(serviceAction.getEntityId())
                            .map(ServiceCreationAction::new)
                            .cast(Action.class);
                    default -> Mono.empty();
                };
            }

            default -> Mono.error(new InvalidActionClassException(action));
        };
    }
    private void pauseExecutor() {
        this.serviceActionExecutor.pause();
        this.resourceActionExecutor.pause();
    }

    private void resumeExecutors() {
        this.serviceActionExecutor.resume();
        this.resourceActionExecutor.resume();
    }
}
