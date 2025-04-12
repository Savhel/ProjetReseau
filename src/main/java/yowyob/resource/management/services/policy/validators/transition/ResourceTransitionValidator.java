package yowyob.resource.management.services.policy.validators.transition;

import org.springframework.stereotype.Component;
import yowyob.resource.management.models.resource.enums.ResourceStatus;

@Component
public class ResourceTransitionValidator {
    private final boolean[][] transitionMatrix;

    public ResourceTransitionValidator() {
        this.transitionMatrix = new boolean[ResourceStatus.values().length][ResourceStatus.values().length];
        this.initTransitionMatrix();
    }

    private void initTransitionMatrix() {
        // from FREE state to other states
        this.transitionMatrix[ResourceStatus.FREE.value()][ResourceStatus.FREE.value()] = true;
        this.transitionMatrix[ResourceStatus.FREE.value()][ResourceStatus.AFFECTED.value()] = true;
        this.transitionMatrix[ResourceStatus.FREE.value()][ResourceStatus.IN_USE.value()] = false;

        // from AFFECTED to other states
        this.transitionMatrix[ResourceStatus.AFFECTED.value()][ResourceStatus.FREE.value()] = true;
        this.transitionMatrix[ResourceStatus.AFFECTED.value()][ResourceStatus.AFFECTED.value()] = true;
        this.transitionMatrix[ResourceStatus.AFFECTED.value()][ResourceStatus.IN_USE.value()] = true;

        // from IN_USE to other states
        this.transitionMatrix[ResourceStatus.IN_USE.value()][ResourceStatus.FREE.value()] = true;
        this.transitionMatrix[ResourceStatus.IN_USE.value()][ResourceStatus.AFFECTED.value()] = false;
        this.transitionMatrix[ResourceStatus.IN_USE.value()][ResourceStatus.IN_USE.value()] = true;
    }

    public boolean isTransitionAllowed(ResourceStatus from, ResourceStatus to) {
        return this.transitionMatrix[from.value()][to.value()];
    }
}
