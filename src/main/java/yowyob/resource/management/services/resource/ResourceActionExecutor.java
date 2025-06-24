package yowyob.resource.management.services.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.repositories.resource.ResourceRepository;
import yowyob.resource.management.services.interfaces.executors.Executor;
import yowyob.resource.management.services.policy.executors.ResourceExecutorPolicy;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ResourceActionExecutor implements Executor {
    private final ResourceRepository resourceRepository;
    private final ResourceExecutorPolicy resourceExecutorPolicy;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final BlockingQueue<Action> waitingActions = new LinkedBlockingQueue<>();
    private final ExecutorService executorService;
    private static final Logger logger = LoggerFactory.getLogger(ResourceActionExecutor.class);
    private static final int THREAD_POOL_SIZE = 10;

    @Autowired
    public ResourceActionExecutor(ResourceExecutorPolicy resourceExecutorPolicy,
                                  ResourceRepository resourceRepository) {
        this.resourceExecutorPolicy = resourceExecutorPolicy;
        this.resourceRepository = resourceRepository;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "ResourceActionExecutor-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
    }

    @Override
    public Optional<?> executeAction(Action action) throws ExecutorPolicyViolationException {
        if (paused.get()) {
            waitingActions.add(action);
            logger.warn("ResourceActionExecutor is paused. Action of Type={} with entityId={} has been queued.",
                    action.getActionClass(),
                    action.getEntityId());
            return Optional.empty();
        }

        logger.info("Executing Resource Action: Type={} for entityId={}",
                action.getActionType(), action.getEntityId());

        if (!this.resourceExecutorPolicy.isExecutionAllowed(action)) {
            throw new ExecutorPolicyViolationException(action,
                    "Execution of the specified resource action is not allowed by policy");
        }

        Optional<?> result = action.execute(this.resourceRepository);
        logger.info("Action execution completed for Action: Type={} with entityId={}",
                action.getActionType(), action.getEntityId());

        return result;

    }

    public Optional<?> forceActionExecution(Action action) {
        logger.warn("Action execution of {} for entityId: {} without Policy verification",
                action.getActionType(), action.getEntityId());
        return executeResourceAction(action);
    }

    private Optional<?> executeResourceAction(Action action) {
        ResourceAction resourceAction = (ResourceAction) action;
        Optional<?> result = resourceAction.execute(this.resourceRepository);
        logger.info("Action execution completed for Action: {} with entityId: {}",
                resourceAction.getActionType(), resourceAction.getEntityId());
        return result;
    }

    @Override
    public void pause() {
        paused.set(true);
        logger.warn("ResourceActionExecutor is now PAUSED. New events will wait until resume() is called.");
    }

    @Override
    public void resume() {
        if (!paused.get()) {
            logger.info("ResourceActionExecutor is already running.");
            return;
        }

        logger.info("ResourceActionExecutor is processing queued actions before resuming...");
        
        // Process all waiting actions in parallel
        CompletableFuture<Void>[] futures = new CompletableFuture[waitingActions.size()];
        int index = 0;
        
        while (!this.waitingActions.isEmpty()) {
            Action action = this.waitingActions.poll();
            futures[index++] = CompletableFuture.runAsync(() -> {
                try {
                    executeResourceAction(action);
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
        logger.info("All waiting actions have been processed. ResourceActionExecutor is now RESUMED.");
    }
    
    /**
     * Shutdown the executor service gracefully
     */
    public void shutdown() {
        logger.info("Shutting down ResourceActionExecutor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ResourceActionExecutor shutdown completed.");
    }
}
