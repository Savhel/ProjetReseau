package yowyob.resource.management.exceptions.policy;

import yowyob.resource.management.actions.Action;

public class ExecutorPolicyViolationException extends PolicyViolationException {

    public ExecutorPolicyViolationException(Action action, String reason) {
        super(String.format(
                "Executor Policy violation: Action [%s] on Entity [%s] of Class [%s] - Reason: %s",
                action.getActionType(), action.getEntityId(), action.getActionClass(), reason
        ));
    }

    public ExecutorPolicyViolationException(Exception e) {
        super(e.getMessage());
    }
}
