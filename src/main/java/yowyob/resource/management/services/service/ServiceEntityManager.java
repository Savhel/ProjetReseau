package yowyob.resource.management.services.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import yowyob.resource.management.events.service.ServiceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.services.product.ProductEntityManager;


@org.springframework.stereotype.Service
public class ServiceEntityManager {
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceActionExecutor serviceActionExecutor;
    private static final Logger logger = LoggerFactory.getLogger(ProductEntityManager.class);

    @Autowired
    public ServiceEntityManager(ApplicationEventPublisher eventPublisher, ServiceActionExecutor serviceActionExecutor) {
        this.eventPublisher = eventPublisher;
        this.serviceActionExecutor = serviceActionExecutor;
    }

    public Mono<Void> triggerServiceEvent(ServiceAction action, LocalDateTime eventStartDateTime) {
        logger.info("Triggering Services Event for entityId: {} with action: {} at: {}",
                action.getEntityId(), action.getActionType(), eventStartDateTime);

        eventPublisher.publishEvent(new ServiceEvent(this, action, eventStartDateTime));
        return Mono.empty();
    }

    public Mono<?> executeAction(ServiceAction serviceAction) {
        logger.info("Executing Services Action: {} for entityId: {}",
                serviceAction.getActionType(), serviceAction.getEntityId());

        return this.serviceActionExecutor.executeAction(serviceAction);
    }
}