package yowyob.resource.management.services.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.commons.Command;
import yowyob.resource.management.services.resource.ResourceUpdater;
import yowyob.resource.management.services.service.ServiceActionExecutor;
import yowyob.resource.management.services.resource.ResourceActionExecutor;
import yowyob.resource.management.exceptions.invalid.InvalidEventClassException;
import yowyob.resource.management.exceptions.invalid.InvalidActionClassException;
import yowyob.resource.management.services.context.updaters.UpdaterContextManager;
import yowyob.resource.management.services.context.executors.ExecutorContextManager;
import yowyob.resource.management.services.service.ServiceUpdater;

@Service
public class ContextManager {
    private final ContextStack contextStack;
    private final UpdaterContextManager updaterContextManager;
    private final ExecutorContextManager executorContextManager;
    private final ResourceActionExecutor resourceActionExecutor;
    private final ServiceActionExecutor serviceActionExecutor;
    private final ServiceUpdater serviceUpdater;
    private final ResourceUpdater resourceUpdater;

    private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);

    @Autowired
    public ContextManager(ExecutorContextManager executorContextManager,
                          UpdaterContextManager updaterContextManager,
                          ContextStack contextStack,
                          ResourceActionExecutor resourceActionExecutor,
                          ServiceActionExecutor serviceActionExecutor,
                          ServiceUpdater serviceUpdater,
                          ResourceUpdater resourceUpdater) {
        this.executorContextManager = executorContextManager;
        this.updaterContextManager = updaterContextManager;
        this.contextStack = contextStack;
        this.resourceActionExecutor = resourceActionExecutor;
        this.serviceActionExecutor = serviceActionExecutor;
        this.resourceUpdater = resourceUpdater;
        this.serviceUpdater = serviceUpdater;
    }

    public void init() {
        logger.info("Starting context initialization");
        executorContextManager.init();
        updaterContextManager.init();
        logger.info("Successfully initialized context");
    }

    public void pushAction(Action action) {
        contextStack.push(action);
        logger.info("Action : Class= {}, Type={}, entityId={} has been pushed to the ContextManager",
                action.getActionClass(), action.getActionType(), action.getEntityId());
    }

    public void pushEvent(Event event) {
        contextStack.push(event);
        logger.info("Event : Class= {}, ActionType={}, entityId={}, start={} has been pushed to ContextManager",
                event.getEventClass(), event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime());
    }

    public void rollback() {
        logger.info("Starting Context rollback");
        while (!contextStack.empty()) {
            Command command = contextStack.pop();

            if (command instanceof Action action) {
                switch (action.getActionClass()) {
                    case Resource -> this.resourceActionExecutor.forceActionExecution(action);
                    case Service -> this.serviceActionExecutor.forceActionExecution(action);
                    default -> throw new InvalidActionClassException(action);
                }
                logger.info("Reverse Action : Class= {}, Type={}, entityId={} has been successfully executed",
                        action.getActionClass(), action.getActionType(), action.getEntityId());

            } else if (command instanceof Event event) {
                switch (event.getEventClass()) {
                    case Resource -> resourceUpdater.unscheduleEvent(event);
                    case Service -> serviceUpdater.unscheduleEvent(event);
                    default -> throw new InvalidEventClassException(event);
                }

                logger.info("Event : Class= {}, ActionType={}, entityId={}, start={} has been successfully removed from timeline",
                        event.getEventClass(), event.getAction().getActionType(), event.getEntityId(), event.getEventStartDateTime());
            }
        }

        logger.info("Context has been successfully rolled back");
    }

    public void clear() {
        logger.info("Starting context clearing");
        executorContextManager.clear();
        updaterContextManager.clear();
        logger.info("Successfully cleared context");
    }
}
