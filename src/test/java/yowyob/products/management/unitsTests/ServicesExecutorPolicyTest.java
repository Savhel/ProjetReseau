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
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.service.operations.ServiceCreationAction;
import yowyob.resource.management.actions.service.operations.ServiceDeletionAction;
import yowyob.resource.management.actions.service.operations.ServiceReadingAction;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.models.service.enums.ServiceStatus;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.policy.executors.ServiceExecutorPolicy;
import yowyob.resource.management.services.policy.validators.operations.ServiceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ServiceTransitionValidator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicesExecutorPolicyTest {

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ServiceTransitionValidator transitionValidator;

    @MockitoBean
    private ServiceStatusBasedOperationValidator statusValidator;

    @Autowired
    private ServiceExecutorPolicy serviceExecutorPolicy;

    private UUID entityId;
    private Services services;

    @BeforeEach
    void setUp() {
        this.entityId = UUID.fromString("550e8400-b29b-41d4-a716-446655440000");
        this.services = new Services();
        this.services.setId(entityId);
        this.services.setStatus(ServiceStatus.PLANNED);
    }

    @Test
    void isExecutionAllowed_CreateAction_ServiceDoesNotExist_ShouldAllow() throws ExecutorPolicyViolationException {

        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.empty());
        ServiceAction action = new ServiceCreationAction(services);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_CreateAction_ServiceExists_ShouldNotAllow() throws ExecutorPolicyViolationException {

        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
//        System.out.println("is present : "+serviceRepository.findById(entityId).isPresent());
        ServiceAction action = new ServiceCreationAction(services);
        System.out.println((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));

        // Act & Assert
        assertFalse((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));

    }

    @Test
    void isExecutionAllowed_ReadAction_ServiceExists_ShouldAllow() throws ExecutorPolicyViolationException {

        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        ServiceAction action = new ServiceReadingAction(entityId);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_ReadAction_ServiceDoesNotExist_ShouldNotAllow() throws ExecutorPolicyViolationException {
        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.empty());
        ServiceAction action = new ServiceReadingAction(entityId);

        // Act & Assert
        assertFalse((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromPlannedToCancelled_ShouldAllow() throws ExecutorPolicyViolationException {

        // Arrange
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.CANCELLED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PLANNED, ServiceStatus.CANCELLED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_ShouldThrowException() {
        // Arrange
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.ONGOING);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PLANNED, ServiceStatus.ONGOING)).thenReturn(false);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ServiceExistsAndDeletionAllowed_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(statusValidator.isDeletionAllowed(ServiceStatus.PLANNED)).thenReturn(true);
        ServiceAction action = new ServiceDeletionAction(entityId);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ServiceDoesNotExist_ShouldThrowException() {
        
        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.empty());
        ServiceAction action = new ServiceDeletionAction(entityId);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_DeleteAction_ServiceExistsButDeletionNotAllowed_ShouldNotAllow() throws ExecutorPolicyViolationException {

        // Arrange
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(statusValidator.isDeletionAllowed(ServiceStatus.ONGOING)).thenReturn(false);
        ServiceAction action = new ServiceDeletionAction(entityId);

        // Act & Assert
        assertFalse((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    } //*/

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromPlannedToPublished_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.PUBLISHED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PLANNED, ServiceStatus.PUBLISHED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromPublishedToPlanned_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        services.setStatus(ServiceStatus.PUBLISHED);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.PLANNED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PUBLISHED, ServiceStatus.PLANNED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromPublishedToOngoing_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        services.setStatus(ServiceStatus.PUBLISHED);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.ONGOING);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PUBLISHED, ServiceStatus.ONGOING)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromPublishedToCancelled_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        services.setStatus(ServiceStatus.PUBLISHED);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.CANCELLED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.PUBLISHED, ServiceStatus.CANCELLED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_ValidTransition_FromOngoingToFinished_ShouldAllow() throws ExecutorPolicyViolationException {
        // Arrange
        services.setStatus(ServiceStatus.ONGOING);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.FINISHED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.ONGOING, ServiceStatus.FINISHED)).thenReturn(true);

        // Act & Assert
        assertTrue((BooleanSupplier) serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_FromOngoingToCancelled_ShouldThrowException() {
        // Arrange
        services.setStatus(ServiceStatus.ONGOING);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.CANCELLED);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.ONGOING, ServiceStatus.CANCELLED)).thenReturn(false);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> serviceExecutorPolicy.isExecutionAllowed(action));
    }

    @Test
    void isExecutionAllowed_UpdateAction_InvalidTransition_FromFinishedToOngoing_ShouldThrowException() {
        // Arrange
        services.setStatus(ServiceStatus.FINISHED);
        ServiceUpdateAction action = mock(ServiceUpdateAction.class);
        Services updatedServices = new Services();
        updatedServices.setStatus(ServiceStatus.ONGOING);

        when(action.getEntityId()).thenReturn(entityId);
        when(action.getActionType()).thenReturn(ActionType.UPDATE);
        when(action.getServicesToUpdate()).thenReturn(updatedServices);
        when(serviceRepository.findById(entityId)).thenReturn(Mono.just(services));
        when(transitionValidator.isTransitionAllowed(ServiceStatus.FINISHED, ServiceStatus.ONGOING)).thenReturn(false);

        // Act & Assert
        assertThrows(ExecutorPolicyViolationException.class, () -> serviceExecutorPolicy.isExecutionAllowed(action));
    }
}