package yowyob.products.management.unitsTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import yowyob.resource.management.Application;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.service.operations.ServiceCreationAction;
import yowyob.resource.management.actions.service.operations.ServiceDeletionAction;
import yowyob.resource.management.actions.service.operations.ServiceReadingAction;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.service.ServiceEvent;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.models.service.enums.ServiceStatus;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.policy.updaters.ServiceUpdaterPolicy;
import yowyob.resource.management.services.policy.validators.operations.ServiceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ServiceTransitionValidator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
public class UpdateLogicTest {

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ServiceTransitionValidator transitionValidator;

    @MockitoBean
    private ServiceStatusBasedOperationValidator statusBasedOperationValidator;

    @Autowired
    private ServiceUpdaterPolicy serviceUpdaterPolicy;

    private UUID serviceId;
    private Event eventCreate;
    private Event eventRead;
    private Event eventUpdate;
    private Event eventDelete;
    private Services services;
    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        services = new Services();
        services.setId(serviceId);
        services.setStatus(ServiceStatus.PLANNED);

//        Action actionCreate = new SpecificAction(serviceId, ActionType.CREATE, ActionClass.Resource);

        ServiceAction actionCreate = new ServiceCreationAction(services);
        ServiceAction actionRead = new ServiceReadingAction(serviceId);
        ServiceAction actionDelete = new ServiceDeletionAction(serviceId);

        eventCreate = new ServiceEvent(
                this, // Source
                actionCreate,
                LocalDateTime.now().plusSeconds(10) // Date valide
        );
        eventRead = new ServiceEvent(
                this,
                actionRead,
                LocalDateTime.now().plusSeconds(20) // Date valide
        );

        eventDelete = new ServiceEvent(
                this,actionDelete, LocalDateTime.now().plusSeconds(5)
        );
    }

    @Test
    public void testCreateAllowedWhenNoExistingService() {
        Map<UUID, Event> scheduledEvents = new HashMap<>();
        when(serviceRepository.findById(serviceId)).thenReturn(Mono.empty());

        Mono<Boolean> result = serviceUpdaterPolicy.isExecutionAllowed(eventCreate, new ArrayList<>());

        assertTrue((BooleanSupplier) result,"");
    }

    @Test
    public void testCreateNotAllowedWhenServiceExists() {

        Map<UUID, List<Event>> scheduledEvents = new HashMap<>();
        ServiceAction actionCreate = new ServiceCreationAction(services);
        //a first event
        Event _eventCreate = new ServiceEvent(
                this, actionCreate, LocalDateTime.now().plusSeconds(2)
        );
        ArrayList<Event> events = new ArrayList<>();
        events.add(_eventCreate);
        scheduledEvents.put(serviceId, events);

        when(serviceRepository.findById(serviceId)).thenReturn(Mono.just(services));

        assertThrows(UpdaterPolicyViolationException.class, () ->
                serviceUpdaterPolicy.isExecutionAllowed(eventCreate, scheduledEvents.get(serviceId)));

    }

    /* @Test
    public void testReadAllowedWhenServiceExists() {
        ServiceAction actionCreate = new ServiceCreationAction(services);
        //a first event
        Event _eventCreate = new ServiceEvent(
                this, actionCreate, LocalDateTime.now().plusSeconds(2)
        );
        ArrayList<Event> events = new ArrayList<>();
        events.add(_eventCreate);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(services));
        boolean result = serviceUpdaterPolicy.isExecutionAllowed(eventRead, events);

        assertTrue(result,"");
    } */

    @Test
    public void testReadNotAllowedAfterDelete() {
        ArrayList<Event> events = new ArrayList<>();
        events.add(eventDelete);

        assertThrows(UpdaterPolicyViolationException.class, () ->
                serviceUpdaterPolicy.isExecutionAllowed(eventRead, events));
    }

    @Test
    public void testDeleteAllowedForExistingService() {
        ArrayList<Event> events = new ArrayList<>();
        events.add(eventRead);

        when(serviceRepository.findById(serviceId)).thenReturn(Mono.just(services));
        when(statusBasedOperationValidator.isDeletionAllowed(any())).thenReturn(true);

        Mono<Boolean> result = serviceUpdaterPolicy.isExecutionAllowed(eventDelete, events);

        assertTrue((BooleanSupplier) result, "DELETE should be allowed when services exists and deletion is valid.");
    }

    @Test
    public void testDeleteNotAllowedWhenStatusDoesNotPermit() {
        services.setStatus(ServiceStatus.ONGOING);
        ServiceAction actionUpdate = new ServiceUpdateAction(services);

        eventUpdate = new ServiceEvent(
                this, actionUpdate, LocalDateTime.now().plusSeconds(2)
        );
        ArrayList<Event> events = new ArrayList<>();
        events.add(eventUpdate);

        when(serviceRepository.findById(serviceId)).thenReturn(Mono.just(services));
        when(statusBasedOperationValidator.isDeletionAllowed(any())).thenReturn(false);

        assertThrows(UpdaterPolicyViolationException.class, () ->
                serviceUpdaterPolicy.isExecutionAllowed(eventDelete, events));

//        assertTrue(exception.getMessage().contains("Cannot delete services"));
    }
}