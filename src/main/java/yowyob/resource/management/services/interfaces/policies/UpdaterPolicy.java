package yowyob.resource.management.services.interfaces.policies;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import reactor.core.publisher.Mono;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;


public interface UpdaterPolicy {
    Mono<Boolean> isExecutionAllowed(Event event, List<Event> scheduledEvents);
}
