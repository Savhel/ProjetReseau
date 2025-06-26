package yowyob.products.management.unitsTests;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import yowyob.resource.management.Application;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.services.ServiceAction;
import yowyob.resource.management.commons.Command;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.StrategyConversionException;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.models.resource.enums.ResourceStatus;
import yowyob.resource.management.models.services.Services;
import yowyob.resource.management.actions.resource.operations.*;
import yowyob.resource.management.actions.services.operations.*;
import yowyob.resource.management.events.services.ServiceEvent;
import yowyob.resource.management.models.services.enums.ServiceStatus;
import yowyob.resource.management.services.strategy.StrategyBuilder;
import yowyob.resource.management.services.strategy.StrategyConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
class StrategyTests {

    private StrategyConverter converter;
    private StrategyBuilder builder;

    @BeforeEach
    void setUp() {
        converter = new StrategyConverter();
        builder = new StrategyBuilder();
    }

    @Nested
    class ConverterTests {

        @Test
        void testConvertValidResourceCreateAction() throws Exception {

            // Given
            String json = """
            {
                "strategies": [{
                    "entityId": "550e8400-e29b-41d4-a716-446655440000",
                    "actionType": "CREATE",
                    "actionClass": "Resource",
                    "params": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "status": "FREE"
                    }
                }]
            }
            """;

            // When
            Map<String, ArrayList<Command>> converted = converter.convertFromJson(json);
            ArrayList<Action> strategyActions = new ArrayList<>(
                    converted.get("actions").stream()
                            .map(command -> (Action) command)
                            .toList()
            );

            // Then
            assertFalse(strategyActions.isEmpty());
            Action action = strategyActions.getFirst();
            assertInstanceOf(ResourceCreationAction.class, action);
            ResourceCreationAction createAction = (ResourceCreationAction) action;
            assertEquals(0, createAction.getResourceToSave().getState());
        }

        @Test
        void testConvertValidServiceEvent() throws Exception {
            // Given
            LocalDateTime eventTime = LocalDateTime.now().plusMinutes(20);
            String json = String.format("""
            {
                "strategies": [{
                    "entityId": "550e8400-e29b-41d4-a716-446655440000",
                    "actionType": "UPDATE",
                    "actionClass": "Services",
                    "eventStartDateTime": "%s",
                    "params": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "status": "PLANNED"
                    }
                }]
            }
            """, eventTime);

            // When
            Map<String, ArrayList<Command>> converted = converter.convertFromJson(json);
            ArrayList<Event> strategyEvents = new ArrayList<>(
                    converted.get("events").stream()
                            .map(command -> (Event) command)
                            .toList()
            );

            // Then
            assertFalse(strategyEvents.isEmpty());
            Event event = strategyEvents.getFirst();
            assertInstanceOf(ServiceEvent.class, event);
            assertEquals(eventTime, event.getEventStartDateTime());
        }

        @Test
        void testConvertCustomActionWithQuery() throws Exception {

            // Given
            String json = """
            {
                "strategies": [{
                    "entityId": "550e8400-e29b-41d4-a716-446655440000",
                    "actionType": "CUSTOM",
                    "actionClass": "Resource",
                    "query": "custom query"
                }]
            }
            """;

            // When
            Map<String, ArrayList<Command>> converted = converter.convertFromJson(json);
            ArrayList<Action> strategyActions = new ArrayList<>(
                    converted.get("actions").stream()
                            .map(command -> (Action) command)
                            .toList()
            );

            // Then
            assertFalse(strategyActions.isEmpty());
            Action action = strategyActions.getFirst();
            assertInstanceOf(ResourceCustomAction.class, action);
            assertEquals("custom query", ((ResourceCustomAction) action).getQuery());
        }

        @Test
        void testMissingRequiredFields() {
            // Given
            String json = """
            {
                "strategies": [{
                    "entityId": "550e8400-e29b-41d4-a716-446655440000",
                    "actionType": "CREATE"
                }]
            }
            """;

            // Then
            assertThrows(StrategyConversionException.class, () -> converter.convertFromJson(json));
        }
    }

    @Nested
    class BuilderTests {

        @Test
        void testBuildResourceActions() throws Exception {
            // Given
            List<Action> actions = new ArrayList<>();
            Resource resource = new Resource();
            resource.setId(UUID.randomUUID());
            resource.setStatus(ResourceStatus.AFFECTED);

            actions.add(new ResourceCreationAction(resource));
            actions.add(new ResourceReadingAction(UUID.randomUUID()));
            actions.add(new ResourceCustomAction(UUID.randomUUID(), "custom query"));

            // When
            String json = builder.buildFromActions(actions);

            // Then
            assertTrue(json.contains("CREATE"));
            assertTrue(json.contains("READ"));
            assertTrue(json.contains("CUSTOM"));
            assertTrue(json.contains("AFFECTED"));
            assertTrue(json.contains("custom query"));
        }

        @Test
        void testBuildServiceEvents() throws Exception {
            // Given
            List<Event> events = new ArrayList<>();
            Services services = new Services();
            services.setId(UUID.randomUUID());
            services.setStatus(ServiceStatus.PLANNED);

            ServiceAction action = new ServiceUpdateAction(services);
            LocalDateTime eventTime = LocalDateTime.now().plusMinutes(20);
            events.add(new ServiceEvent(this, action, eventTime));

            // When
            String json = builder.buildFromEvents(events);

            // Then
            assertTrue(json.contains("UPDATE"));
            assertTrue(json.contains("Services"));
            assertTrue(json.contains("PLANNED"));
            assertTrue(json.contains(eventTime.toString()));
        }

        @Test
        void testRoundTripConversion() throws Exception {
            // Given
            List<Action> originalActions = new ArrayList<>();
            Resource resource = new Resource();
            resource.setId(UUID.randomUUID());
            resource.setStatus(ResourceStatus.IN_USE);
            originalActions.add(new ResourceCreationAction(resource));

            // When
            String json = builder.buildFromActions(originalActions);
            Map<String, ArrayList<Command>> converted = converter.convertFromJson(json);
            ArrayList<Action> strategyActions = new ArrayList<>(
                    converted.get("actions").stream()
                            .map(command -> (Action)command)
                            .toList()
            );

            // Then
            Action convertedAction = strategyActions.getFirst();
            assertInstanceOf(ResourceCreationAction.class, convertedAction);
            ResourceCreationAction createAction = (ResourceCreationAction) convertedAction;
            assertEquals(resource.getState(), createAction.getResourceToSave().getState());
        }
    }
}