package yowyob.resource.management.exceptions;

import yowyob.resource.management.exceptions.invalid.InvalidInputException;

public class MissingParameterException extends InvalidInputException {
    public MissingParameterException(String parameterName) {
        super(String.format("Missing required parameter: '%s'. Please provide the parameter to proceed.", parameterName));
    }

    public MissingParameterException(String parameterName, Throwable cause) {
        super(String.format("Missing required parameter: '%s'. Cause: %s", parameterName, cause != null ? cause.getMessage() : "None"), cause);
    }
}

