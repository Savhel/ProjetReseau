package yowyob.resource.management.exceptions.invalid;

import yowyob.resource.management.commons.Command;

public class InvalidCommandException extends InvalidInputException {
    public InvalidCommandException(String message) {
        super(message);
    }
}
