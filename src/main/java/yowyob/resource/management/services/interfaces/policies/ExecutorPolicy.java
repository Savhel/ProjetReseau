package yowyob.resource.management.services.interfaces.policies;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

public interface ExecutorPolicy {

    boolean isExecutionAllowed(Action action) throws ExecutorPolicyViolationException;
}

