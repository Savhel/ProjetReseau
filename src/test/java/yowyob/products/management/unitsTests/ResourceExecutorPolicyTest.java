package yowyob.products.management.unitsTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import yowyob.resource.management.Application;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.actions.resource.operations.ResourceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceDeletionAction;
import yowyob.resource.management.actions.resource.operations.ResourceReadingAction;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.models.resource.enums.ResourceStatus;
import yowyob.resource.management.repositories.resource.ResourceRepository;
import yowyob.resource.management.services.policy.executors.ResourceExecutorPolicy;
import yowyob.resource.management.services.policy.validators.operations.ResourceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ResourceTransitionValidator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = Application.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceExecutorPolicyTest {

    @MockitoBean
    private ResourceRepository resourceRepository;

    @MockitoBean
    private ResourceTransitionValidator transitionValidator;

    @MockitoBean
    private ResourceStatusBasedOperationValidator statusValidator;

    @Autowired
    private ResourceExecutorPolicy resourceExecutorPolicy;

    private UUID entityId;
    private Resource resource;

    @BeforeEach
    void setUp() {
        this.entityId = UUID.randomUUID();
        this.resource = new Resource();
        this.resource.setId(entityId);
        this.resource.setStatus(ResourceStatus.FREE);
    }

    @Test
    void isExecutionAllowed_CreateAction_ResourceDoesNotExist_ShouldAllow() throws ExecutorPolicyViolationException {

        when(resourceRepository.findById(entityId)).thenReturn(Mono.empty());
        ResourceAction action = new ResourceCreationAction(resource);

        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_CreateAction_ResourceExists_ShouldNotAllow() throws ExecutorPolicyViolationException {

        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        ResourceAction action = new ResourceCreationAction(resource);

        assertFalse((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_ReadAction_ResourceExists_ShouldAllow() throws ExecutorPolicyViolationException {

        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        ResourceAction action = new ResourceReadingAction(entityId);

        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_ReadAction_ResourceDoesNotExist_ShouldNotAllow() throws ExecutorPolicyViolationException {
        when(resourceRepository.findById(entityId)).thenReturn(Mono.empty());
        ResourceAction action = new ResourceReadingAction(entityId);

        assertFalse((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_ShouldAllow() throws ExecutorPolicyViolationException {

        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.AFFECTED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.FREE, ResourceStatus.AFFECTED)).thenReturn(true);

        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_ShouldThrowException() {
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.IN_USE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.FREE, ResourceStatus.IN_USE)).thenReturn(false);

        assertThrows(ExecutorPolicyViolationException.class, () -> resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ResourceExistsAndDeletionAllowed_ShouldAllow() throws ExecutorPolicyViolationException {
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(statusValidator.isDeletionAllowed(ResourceStatus.FREE)).thenReturn(true);
        ResourceAction action = new ResourceDeletionAction(entityId);

        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ResourceDoesNotExist_ShouldThrowException() {
        when(resourceRepository.findById(entityId)).thenReturn(Mono.empty());
        ResourceAction action = new ResourceDeletionAction(entityId);

        assertThrows(ExecutorPolicyViolationException.class, () -> resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ResourceExistsButDeletionNotAllowed_ShouldNotAllow() throws ExecutorPolicyViolationException {

        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(statusValidator.isDeletionAllowed(ResourceStatus.IN_USE)).thenReturn(false);
        ResourceAction action = new ResourceDeletionAction(entityId);

        assertFalse((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    // Tests pour les transitions depuis l'état FREE
    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromFreeToFree_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.FREE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.FREE, ResourceStatus.FREE)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromFreeToAffected_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.AFFECTED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.FREE, ResourceStatus.AFFECTED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    // Tests pour les transitions depuis l'état AFFECTED
    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromAffectedToFree_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        resource.setStatus(ResourceStatus.AFFECTED);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.FREE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.AFFECTED, ResourceStatus.FREE)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromAffectedToAffected_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        resource.setStatus(ResourceStatus.AFFECTED);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.AFFECTED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.AFFECTED, ResourceStatus.AFFECTED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromAffectedToInUse_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        resource.setStatus(ResourceStatus.AFFECTED);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.IN_USE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.AFFECTED, ResourceStatus.IN_USE)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    // Tests pour les transitions depuis l'état IN_USE
    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromInUseToFree_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        resource.setStatus(ResourceStatus.IN_USE);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.FREE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.IN_USE, ResourceStatus.FREE)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromInUseToInUse_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        resource.setStatus(ResourceStatus.IN_USE);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.IN_USE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.IN_USE, ResourceStatus.IN_USE)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) resourceExecutorPolicy.isExecutionAllowed(action));
    }

    // Tests pour les transitions invalides
    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_FromFreeToInUse_ShouldThrowException() {
        // Arrange
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.IN_USE);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.FREE, ResourceStatus.IN_USE)).thenReturn(false);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> resourceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_FromInUseToAffected_ShouldThrowException() {
        // Arrange
        resource.setStatus(ResourceStatus.IN_USE);
        ResourceUpdateAction action = mock(ResourceUpdateAction.class);
        Resource updatedResource = new Resource();
        updatedResource.setStatus(ResourceStatus.AFFECTED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getResourceToUpdate()).thenReturn(updatedResource);
        when(resourceRepository.findById(entityId)).thenReturn(Mono.just(resource));
        when(transitionValidator.isTransitionAllowed(ResourceStatus.IN_USE, ResourceStatus.AFFECTED)).thenReturn(false);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> resourceExecutorPolicy.isExecutionAllowed(action));
    }
}
