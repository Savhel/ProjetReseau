package yowyob.resource.management.services.policy.validators.operations;

import org.springframework.stereotype.Component;
import yowyob.resource.management.models.service.enums.ServiceStatus;

@Component
public class ServiceStatusBasedOperationValidator {

    public boolean isDeletionAllowed(ServiceStatus currentStatus) {
        return switch (currentStatus) {
            case PLANNED, PUBLISHED, ONGOING -> false;
            case FINISHED, CANCELLED -> true;
        };
    }
}
