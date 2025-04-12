package yowyob.resource.management.exceptions.invalid;


public class InvalidJsonFormatException extends InvalidInputException {

    public InvalidJsonFormatException(String message) {
        super(String.format("Invalid JSON format error: %s", message));
    }

    public InvalidJsonFormatException(String message, Throwable cause) {
        super(String.format("Invalid JSON format error: %s. Cause: %s", message, cause != null ? cause.getMessage() : "None"), cause);
    }
}

