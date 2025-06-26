package yowyob.resource.management.services.context.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    private static final Logger logger = LoggerFactory.getLogger(ExecutorContextManager.class);

    @Autowired
    public ExecutorContextManager(ServiceActionExecutor serviceActionExecutor, ResourceActionExecutor resourceActionExecutor,
                                  ServiceRepository serviceRepository, ResourceRepository resourceRepository) {
        this.serviceActionExecutor = serviceActionExecutor;
        this.resourceActionExecutor = resourceActionExecutor;
        this.serviceRepository = serviceRepository;
        this.resourceRepository = resourceRepository;
    }
    
    public void init() {
        pauseExecutor();
        logger.info("Context has been successfully initialized");
    }

    public void clear() {
        resumeExecutors();
        logger.info("Context has been successfully cleared");
    }

    public Action generateReverseAction(Action action) {
        return switch (action.getActionClass()) {
            case Resource -> {
                ResourceAction resourceAction = (ResourceAction) action;
                Optional<Resource> initialResource = this.resourceRepository.findById(resourceAction.getEntityId());

                yield switch (resourceAction.getActionType()) {
                    case CREATE -> new ResourceDeletionAction(resourceAction.getEntityId());
                    case UPDATE -> new ResourceUpdateAction(initialResource.get()); // Executors policy already manage empty case, no worries
                    case DELETE -> new ResourceCreationAction(initialResource.get()); // Executors policy already manage empty case, no worries
                    default -> null;
                };
            }

            case Service -> {
                ServiceAction serviceAction = (ServiceAction) action;
                Optional<Services> initialService = this.serviceRepository.findById(serviceAction.getEntityId());

                yield switch (serviceAction.getActionType()) {
                    case CREATE -> new ServiceDeletionAction(serviceAction.getEntityId());
                    case UPDATE -> new ServiceUpdateAction(initialService.get()); // Executors policy already manage empty case, no worries
                    case DELETE -> new ServiceCreationAction(initialService.get()); // Executors policy already manage empty case, no worries
                    default -> null;
                };
            }

            default -> throw new InvalidActionClassException(action);
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
