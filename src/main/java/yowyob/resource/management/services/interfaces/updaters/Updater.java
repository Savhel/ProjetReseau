package yowyob.resource.management.services.interfaces.updaters;


import reactor.core.publisher.Mono;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;


public interface Updater {
    Mono<Void> pause();
    Mono<Void> resume();
    Mono<Void> handleEvent(Event event) throws ExecutorPolicyViolationException, UpdaterPolicyViolationException;
}