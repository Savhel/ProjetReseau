package yowyob.resource.management.services.policy.validators.operations;

import org.springframework.stereotype.Component;
import yowyob.resource.management.models.resource.enums.ResourceStatus;

@Component
public class ResourceStatusBasedOperationValidator {

    public boolean isDeletionAllowed(ResourceStatus currentStatus) {
        return switch (currentStatus) {
            case FREE -> true;
            case IN_USE, AFFECTED -> false;
        };
    }
}
