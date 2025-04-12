package yowyob.resource.management.services.interfaces.updaters;


import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;


public interface Updater {
    void pause();
    void resume();
    void handleEvent(Event event) throws ExecutorPolicyViolationException, UpdaterPolicyViolationException;
}