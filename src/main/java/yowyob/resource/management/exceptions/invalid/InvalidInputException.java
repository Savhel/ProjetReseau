package yowyob.resource.management.exceptions.invalid;

public abstract class InvalidInputException extends IllegalArgumentException {
    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable t) {
        super(message, t);
    }
}
