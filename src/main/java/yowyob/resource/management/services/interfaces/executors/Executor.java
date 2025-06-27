package yowyob.resource.management.services.interfaces.executors;

import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

public interface Executor {
    void pause();
    void resume();
    Mono<?> executeAction(Action action) throws ExecutorPolicyViolationException;
}