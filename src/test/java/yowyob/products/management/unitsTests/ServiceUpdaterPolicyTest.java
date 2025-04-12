package yowyob.products.management.unitsTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yowyob.resource.management.Application;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.service.operations.*;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.service.ServiceEvent;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;
import yowyob.resource.management.models.service.Service;
import yowyob.resource.management.models.service.enums.ServiceStatus;
import yowyob.resource.management.repositories.service.ServiceRepository;
import yowyob.resource.management.services.policy.updaters.ServiceUpdaterPolicy;
import yowyob.resource.management.services.policy.validators.operations.ServiceStatusBasedOperationValidator;
import yowyob.resource.management.services.policy.validators.transition.ServiceTransitionValidator;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
public class ServiceUpdaterPolicyTest {

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
    private Service service;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        service = new Service();
        service.setId(serviceId);
        service.setStatus(ServiceStatus.PLANNED);

        ServiceAction actionCreate = new ServiceCreationAction(service);
        ServiceAction actionRead = new ServiceReadingAction(serviceId);
        ServiceAction actionDelete = new ServiceDeletionAction(serviceId);

        eventCreate = new ServiceEvent(
                this,
                actionCreate,
                LocalDateTime.now().plusSeconds(10)
        );
        eventRead = new ServiceEvent(
                this,
                actionRead,
                LocalDateTime.now().plusSeconds(20)
        );
        eventDelete = new ServiceEvent(
                this,
                actionDelete,
                LocalDateTime.now().plusSeconds(5)
        );
    }

    @Test
    public void testCreateAllowedWhenNoExistingService() {
        ArrayList<Event> events = new ArrayList<>();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        boolean result = serviceUpdaterPolicy.isExecutionAllowed(eventCreate, events);

        assertTrue(result, "Create should be allowed when no service exists");
    }

    @Test
    public void testCreateNotAllowedWhenServiceExists() {
        Map<UUID, List<Event>> scheduledEvents = new HashMap<>();
        ServiceAction actionCreate = new ServiceCreationAction(service);
        Event _eventCreate = new ServiceEvent(
                this, actionCreate, LocalDateTime.now().plusSeconds(2)
        );
        ArrayList<Event> events = new ArrayList<>();
        events.add(_eventCreate);
        scheduledEvents.put(serviceId, events);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        assertThrows(UpdaterPolicyViolationException.class, () ->
                serviceUpdaterPolicy.isExecutionAllowed(eventCreate, scheduledEvents.get(serviceId)));
    }

    /*@Test
    public void testReadAllowedWhenServiceExists() {
        ArrayList<Event> events = new ArrayList<>();
        ServiceAction actionCreate = new ServiceCreationAction(service);
        Event _eventCreate = new ServiceEvent(
                this, actionCreate, LocalDateTime.now().plusSeconds(2)
        );
        events.add(_eventCreate);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        boolean result = serviceUpdaterPolicy.isExecutionAllowed(eventRead, events);

        assertTrue(result, "Read should be allowed when service exists");
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

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(statusBasedOperationValidator.isDeletionAllowed(any())).thenReturn(true);

        boolean result = serviceUpdaterPolicy.isExecutionAllowed(eventDelete, events);

        assertTrue(result, "DELETE should be allowed when service exists and deletion is valid.");
    }

    @Test
    public void testDeleteNotAllowedWhenStatusDoesNotPermit() {
        service.setStatus(ServiceStatus.ONGOING);
        ServiceAction actionUpdate = new ServiceUpdateAction(service);
        eventUpdate = new ServiceEvent(
                this, actionUpdate, LocalDateTime.now().plusSeconds(2)
        );

        ArrayList<Event> events = new ArrayList<>();
        events.add(eventUpdate);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(statusBasedOperationValidator.isDeletionAllowed(any())).thenReturn(false);

        assertThrows(UpdaterPolicyViolationException.class, () ->
                serviceUpdaterPolicy.isExecutionAllowed(eventDelete, events));
    }
}