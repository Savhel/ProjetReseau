package yowyob.resource.management.services.interfaces.executors;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

import java.util.Optional;

public interface Executor {
    void pause();
    void resume();
    Optional<?> executeAction(Action action) throws ExecutorPolicyViolationException;
}