package yowyob.resource.management.services.interfaces.policies;

import reactor.core.publisher.Mono;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;

public interface ExecutorPolicy {

    Mono<Boolean> isExecutionAllowed(Action action);
}

