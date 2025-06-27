package yowyob.resource.management.services.strategy;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import org.springframework.stereotype.Service;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.commons.Command;
import yowyob.resource.management.services.context.ContextManager;
import yowyob.resource.management.services.service.ServiceUpdater;
import yowyob.resource.management.services.resource.ResourceUpdater;
import yowyob.resource.management.services.service.ServiceActionExecutor;
import yowyob.resource.management.services.resource.ResourceActionExecutor;
import yowyob.resource.management.exceptions.invalid.InvalidCommandException;
import yowyob.resource.management.services.kafka.KafkaStrategyResponseProducer;
import yowyob.resource.management.exceptions.invalid.InvalidEventClassException;
import yowyob.resource.management.services.policy.updaters.ServiceUpdaterPolicy;
import yowyob.resource.management.services.policy.updaters.ResourceUpdaterPolicy;
import yowyob.resource.management.exceptions.invalid.InvalidActionClassException;
import yowyob.resource.management.services.policy.executors.ServiceExecutorPolicy;
import yowyob.resource.management.services.policy.executors.ResourceExecutorPolicy;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

@Service
public class StrategyEntityManager {
    private final ServiceUpdater serviceUpdater;
    private final ContextManager contextManager;
    private final ResourceUpdater resourceUpdater;
    private final StrategyConverter strategyConverter;
    private final ServiceUpdaterPolicy serviceUpdaterPolicy;
    private final ServiceActionExecutor serviceActionExecutor;
    private final ServiceExecutorPolicy serviceExecutorPolicy;
    private final ResourceUpdaterPolicy resourceUpdaterPolicy;
    private final ResourceActionExecutor resourceActionExecutor;
    private final ResourceExecutorPolicy resourceExecutorPolicy;
    private final KafkaStrategyResponseProducer kafkaStrategyResponseProducer;
    private static final Logger logger = LoggerFactory.getLogger(StrategyEntityManager.class);

    @Autowired
    public StrategyEntityManager(StrategyConverter strategyConverter, ContextManager contextManager,
                                 ServiceUpdater serviceUpdater, ResourceUpdater resourceUpdater,
                                 ServiceActionExecutor serviceActionExecutor, ResourceActionExecutor resourceActionExecutor,
                                 ServiceExecutorPolicy serviceExecutorPolicy, ResourceExecutorPolicy resourceExecutorPolicy,
                                 ServiceUpdaterPolicy serviceUpdaterPolicy, ResourceUpdaterPolicy resourceUpdaterPolicy,
                                 KafkaStrategyResponseProducer kafkaStrategyResponseProducer) {
        this.strategyConverter = strategyConverter;
        this.contextManager = contextManager;
        this.serviceUpdater = serviceUpdater;
        this.resourceUpdater = resourceUpdater;
        this.serviceActionExecutor = serviceActionExecutor;
        this.resourceActionExecutor = resourceActionExecutor;
        this.serviceExecutorPolicy = serviceExecutorPolicy;
        this.resourceExecutorPolicy = resourceExecutorPolicy;
        this.serviceUpdaterPolicy = serviceUpdaterPolicy;
        this.resourceUpdaterPolicy = resourceUpdaterPolicy;
        this.kafkaStrategyResponseProducer = kafkaStrategyResponseProducer;
    }
    
    public void processStrategy(String strategy) {
        contextManager.init();
        try {
            StrategyConverter converter = new StrategyConverter();
            List<Command> commands = converter.convertToCommandListFromJson(strategy);
            logger.debug("Successfully parsed {} commands from Kafka record", commands.size());
            processCommands(commands);
        } catch (Exception e) {
            logger.error("Error processing Received Strategy - Error: {}", e.getMessage());
            handleError(e);
        }
    }

    private void processCommands(List<Command> commands) {
        logger.debug("Starting to process {} commands, initializing contextManager", commands.size());
        contextManager.init();

        for (Command command : commands) {
            if (command instanceof Action action) {
                processAction(action);
            } else if (command instanceof Event event) {
                processEvent(event);
            } else {
                logger.error("Unknown command type received: {}", command.getClass().getSimpleName());
                throw new InvalidCommandException(
                        String.format("Unsupported command type: %s. Expected types are Action or Event",
                                command.getClass().getSimpleName())
                );
            }
        }

        logger.info("Finished processing all commands, clearing contextManager");
        contextManager.clear();
    }

    private void processAction(Action action) {
        logger.info("Processing action - Type={}, EntityId={}", action.getActionClass(), action.getEntityId());

        switch (action.getActionClass()) {
            case Resource -> {
                resourceExecutorPolicy.isExecutionAllowed(action)
                        .flatMap(isAllowed -> {
                            if (!isAllowed) {
                                return Mono.error(new ExecutorPolicyViolationException(action,
                                        "Execution of resource action is not allowed by policy"));
                            }
                            
                            contextManager.pushAction(action);
                            return resourceActionExecutor.forceActionExecution(action);
                        })
                        .doOnSuccess(result -> {
                            kafkaStrategyResponseProducer.pushMessage(String.format("Action - Class=%s, Type=%s, EntityId=%s -> status : OK",
                                    action.getActionClass(), action.getActionType(), action.getEntityId()));
                        })
                        .doOnError(error -> {
                            logger.error("Error processing resource action: {}", error.getMessage());
                            throw new RuntimeException(error);
                        })
                        .block(); // Block here since processCommands is synchronous
            }
            case Service -> {
                serviceExecutorPolicy.isExecutionAllowed(action)
                        .flatMap(isAllowed -> {
                            if (!isAllowed) {
                                return Mono.error(new ExecutorPolicyViolationException(action,
                                        "Execution of service action is not allowed by policy"));
                            }
                            
                            contextManager.pushAction(action);
                            return serviceActionExecutor.forceActionExecution(action);
                        })
                        .doOnSuccess(result -> {
                            kafkaStrategyResponseProducer.pushMessage(String.format("Action - Class=%s, Type=%s, EntityId=%s -> status : OK",
                                    action.getActionClass(), action.getActionType(), action.getEntityId()));
                        })
                        .doOnError(error -> {
                            logger.error("Error processing service action: {}", error.getMessage());
                            throw new RuntimeException(error);
                        })
                        .block(); // Block here since processCommands is synchronous
            }
            default -> throw new InvalidActionClassException(action);
        }
    }

    private void processEvent(Event event) {
        logger.info("Processing Event - Type={}, EntityId={}, start={}",
                event.getEventClass(), event.getEntityId(), event.getEventStartDateTime());

        switch (event.getEventClass()) {
            case Resource -> {
                List<Event> events = !resourceUpdater.getScheduledEvents().containsKey(event.getEntityId())
                        ? new ArrayList<>() : resourceUpdater.getScheduledEvents().get(event.getEntityId());

                resourceUpdaterPolicy.isExecutionAllowed(event, events)
                        .flatMap(isAllowed -> {
                            if (!isAllowed) {
                                return Mono.error(new UpdaterPolicyViolationException(event,
                                        "Scheduling of resource event is not allowed by policy"));
                            }
                            
                            contextManager.pushEvent(event);
                            resourceUpdater.forceEventScheduling(event);
                            return Mono.just("success");
                        })
                        .doOnSuccess(result -> {
                            kafkaStrategyResponseProducer.pushMessage(String.format("Event - Class=%s, ActionType=%s, EntityId=%s -> status : OK",
                                    event.getEventClass(), event.getAction().getActionType(), event.getEntityId()));
                        })
                        .doOnError(error -> {
                            logger.error("Error processing resource event: {}", error.getMessage());
                            throw new RuntimeException(error);
                        })
                        .block(); // Block here since processCommands is synchronous
            }
            case Service -> {
                List<Event> serviceEvents = serviceUpdater.getScheduledEvents().get(event.getEntityId());
                
                serviceUpdaterPolicy.isExecutionAllowed(event, serviceEvents)
                        .flatMap(isAllowed -> {
                            if (!isAllowed) {
                                return Mono.error(new UpdaterPolicyViolationException(event,
                                        "Scheduling of service event is not allowed by policy"));
                            }
                            
                            contextManager.pushEvent(event);
                            serviceUpdater.forceEventScheduling(event);
                            return Mono.just("success");
                        })
                        .doOnSuccess(result -> {
                            kafkaStrategyResponseProducer.pushMessage(String.format("Event - Class=%s, ActionType=%s, EntityId=%s -> status : OK",
                                    event.getEventClass(), event.getAction().getActionType(), event.getEntityId()));
                        })
                        .doOnError(error -> {
                            logger.error("Error processing service event: {}", error.getMessage());
                            throw new RuntimeException(error);
                        })
                        .block(); // Block here since processCommands is synchronous
            }
            default -> throw new InvalidEventClassException(event);
        }
    }

    private void handleError(Exception e) {
        logger.error("Handling error in KafkaStrategyConsumer. message : ");
        logger.error(e.getMessage());
        contextManager.rollback();
        logger.error("Context rolled back");
        contextManager.clear();
        logger.info("Context cleared");

        kafkaStrategyResponseProducer.pushMessage(e.getMessage());
        kafkaStrategyResponseProducer.send();
        logger.info("Error message sent to Kafka response entity");
    }
}
