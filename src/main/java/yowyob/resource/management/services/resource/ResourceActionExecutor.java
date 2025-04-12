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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ResourceActionExecutor implements Executor {
    private final ResourceRepository resourceRepository;
    private final ResourceExecutorPolicy resourceExecutorPolicy;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final BlockingQueue<Action> waitingActions = new LinkedBlockingQueue<>();
    private static final Logger logger = LoggerFactory.getLogger(ResourceUpdater.class);

    @Autowired
    public ResourceActionExecutor(ResourceExecutorPolicy resourceExecutorPolicy,
                                  ResourceRepository resourceRepository) {
        this.resourceExecutorPolicy = resourceExecutorPolicy;
        this.resourceRepository = resourceRepository;
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

    private synchronized Optional<?> executeResourceAction(Action action) {
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
        while (!this.waitingActions.isEmpty()) {
            Action action = this.waitingActions.poll();
            executeAction(action);
        }
        paused.set(false);
        logger.info("All waiting actions have been processed. ResourceActionExecutor is now RESUMED.");
    }
}
