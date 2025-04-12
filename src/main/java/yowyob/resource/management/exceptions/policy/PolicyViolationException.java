package yowyob.resource.management.exceptions.policy;

public abstract class PolicyViolationException extends RuntimeException {
    public PolicyViolationException(String message) {
        super(message);
    }
}
