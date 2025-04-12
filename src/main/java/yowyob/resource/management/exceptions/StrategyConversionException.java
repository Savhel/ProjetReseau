package yowyob.resource.management.exceptions;

public class StrategyConversionException extends RuntimeException {
    public StrategyConversionException(String message) {
        super(String.format("Strategy conversion error: %s", message));
    }

    public StrategyConversionException(String message, Throwable cause) {
        super(String.format("Strategy conversion error: %s. Cause: %s", message, cause != null ? cause.getMessage() : "None"), cause);
    }
}

