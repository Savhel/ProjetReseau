package yowyob.resource.management.exceptions.invalid;

import yowyob.resource.management.actions.Action;

public class InvalidActionClassException extends InvalidInputException {
    public InvalidActionClassException(Action action) {
        super("Invalid action class: " + action.getActionClass() + " for action type: " + action.getActionType());
    }
    public InvalidActionClassException(Action action, String reason) {
        super("Invalid action class: " + action.getActionClass() + " for action type: " + action.getActionType() + " Reason : "+reason);
    }
    
}
