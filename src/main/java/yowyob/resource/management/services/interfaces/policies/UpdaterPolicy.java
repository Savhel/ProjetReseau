package yowyob.resource.management.services.interfaces.policies;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;


public interface UpdaterPolicy {
    boolean isExecutionAllowed(Event event, List<Event> scheduledEvents) throws UpdaterPolicyViolationException;
}
