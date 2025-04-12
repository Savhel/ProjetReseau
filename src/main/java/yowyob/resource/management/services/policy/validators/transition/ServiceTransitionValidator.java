package yowyob.resource.management.services.policy.validators.transition;

import org.springframework.stereotype.Component;
import yowyob.resource.management.models.service.enums.ServiceStatus;

@Component
public class ServiceTransitionValidator {
    private final boolean[][] transitionMatrix;

    public ServiceTransitionValidator() {
        this.transitionMatrix = new boolean[ServiceStatus.values().length][ServiceStatus.values().length];
        this.initTransitionMatrix();
    }

    private void initTransitionMatrix() {
        // defining authorized transitions, all others are false
        this.transitionMatrix[ServiceStatus.PLANNED.value()][ServiceStatus.PUBLISHED.value()] = true;
        this.transitionMatrix[ServiceStatus.PLANNED.value()][ServiceStatus.CANCELLED.value()] = true;

        this.transitionMatrix[ServiceStatus.PUBLISHED.value()][ServiceStatus.PLANNED.value()] = true;
        this.transitionMatrix[ServiceStatus.PUBLISHED.value()][ServiceStatus.ONGOING.value()] = true;
        this.transitionMatrix[ServiceStatus.PUBLISHED.value()][ServiceStatus.CANCELLED.value()] = true;

        this.transitionMatrix[ServiceStatus.ONGOING.value()][ServiceStatus.FINISHED.value()] = true;
    }

    public boolean isTransitionAllowed(ServiceStatus from, ServiceStatus to) {
        return this.transitionMatrix[from.value()][to.value()];
    }
}
