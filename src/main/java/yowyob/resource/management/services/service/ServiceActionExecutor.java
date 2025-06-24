package yowyob.resource.management.services.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.interfaces.executors.Executor;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.services.policy.executors.ServiceExecutorPolicy;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ServiceActionExecutor implements Executor {

    private final ServiceRepository serviceRepository;
    private final ServiceExecutorPolicy serviceExecutorPolicy;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final BlockingQueue<Action> waitingActions = new LinkedBlockingQueue<>();
    private final ExecutorService executorService;
    private static final Logger logger = LoggerFactory.getLogger(ServiceActionExecutor.class);
    private static final int THREAD_POOL_SIZE = 10;
    
    @Autowired
    public ServiceActionExecutor(ServiceExecutorPolicy serviceExecutorPolicy,
                                 ServiceRepository serviceRepository) {
        this.serviceExecutorPolicy = serviceExecutorPolicy;
        this.serviceRepository = serviceRepository;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "ServiceActionExecutor-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
    }

    public Optional<?> executeAction(Action action) throws ExecutorPolicyViolationException {
        if (paused.get()) {
            waitingActions.add(action);
            logger.warn("ServiceActionExecutor is paused. Action of Type={} with entityId={} has been queued.",
                    action.getActionType(),
                    action.getEntityId());
            return Optional.empty();
        }

        logger.info("{} for entityId: {}",
                action.getActionType(), action.getEntityId());

        if (!this.serviceExecutorPolicy.isExecutionAllowed(action)) {
            throw new ExecutorPolicyViolationException(
                    action,
                    String.format("Execution of service action %s is not allowed by policy",
                            action.getClass().getSimpleName())
            );
        }

        return executeServiceAction(action);
    }

    public Optional<?> forceActionExecution(Action action) {
        logger.warn("Action execution of {} for entityId: {} without Policy verification",
                action.getActionType(), action.getEntityId());
        return executeServiceAction(action);
    }

    private Optional<?> executeServiceAction(Action action) {
        ServiceAction serviceAction = (ServiceAction) action;
        Optional<?> result = serviceAction.execute(this.serviceRepository);
        logger.info("Action execution completed for Action: {} with entityId: {}",
                serviceAction.getActionType(), serviceAction.getEntityId());
        return result;
    }

    @Override
    public void pause() {
        paused.set(true);
        logger.warn("ServiceActionExecutor is now PAUSED. New events will wait until resume() is called.");
    }

    @Override
    public void resume() {
        if (!paused.get()) {
            logger.info("ServiceActionExecutor is already running.");
            return;
        }

        logger.info("ServiceActionExecutor is processing queued actions before resuming...");
        
        // Process all waiting actions in parallel
        CompletableFuture<Void>[] futures = new CompletableFuture[waitingActions.size()];
        int index = 0;
        
        while (!this.waitingActions.isEmpty()) {
            Action action = this.waitingActions.poll();
            futures[index++] = CompletableFuture.runAsync(() -> {
                try {
                    executeServiceAction(action);
                } catch (Exception e) {
                    logger.error("Error executing queued action: {}", e.getMessage(), e);
                }
            }, executorService);
        }
        
        // Wait for all actions to complete
        try {
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error waiting for queued actions to complete: {}", e.getMessage(), e);
        }
        
        paused.set(false);
        logger.info("All waiting actions have been processed. ServiceActionExecutor is now RESUMED.");
    }
    
    /**
     * Shutdown the executor service gracefully
     */
    public void shutdown() {
        logger.info("Shutting down ServiceActionExecutor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ServiceActionExecutor shutdown completed.");
    }
}