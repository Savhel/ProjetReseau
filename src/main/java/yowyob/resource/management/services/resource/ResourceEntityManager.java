package yowyob.resource.management.services.resource;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

import yowyob.resource.management.events.resource.ResourceEvent;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.services.product.ProductEntityManager;

@Service
public class ResourceEntityManager {
    private final ApplicationEventPublisher eventPublisher;
    private final ResourceActionExecutor resourceActionExecutor;
    private static final Logger logger = LoggerFactory.getLogger(ProductEntityManager.class);

    @Autowired
    public ResourceEntityManager(ApplicationEventPublisher eventPublisher, ResourceActionExecutor resourceActionExecutor) {
        this.eventPublisher = eventPublisher;
        this.resourceActionExecutor = resourceActionExecutor;
    }

    public void triggerResourceEvent(ResourceAction action, LocalDateTime eventStartDateTime) {
        logger.info("Triggering Resource Event for entityId: {} with action: {} at: {}",
                action.getEntityId(), action.getActionType(), eventStartDateTime);

        eventPublisher.publishEvent(new ResourceEvent(this, action, eventStartDateTime));
    }

    public Optional<?> executeAction(ResourceAction resourceAction) {
        logger.info("Received Action      : Type={}, entityId={}",
                resourceAction.getActionType(), resourceAction.getEntityId());

        return resourceActionExecutor.executeAction(resourceAction);
    }
}