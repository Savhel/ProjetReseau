package yowyob.resource.management.services.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import yowyob.resource.management.actions.Action;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.actions.resource.operations.ResourceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceCustomAction;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.actions.service.operations.ServiceCreationAction;
import yowyob.resource.management.actions.service.operations.ServiceCustomAction;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.events.resource.ResourceEvent;
import yowyob.resource.management.events.service.ServiceEvent;
import yowyob.resource.management.exceptions.StrategyConversionException;

import java.util.List;

@org.springframework.stereotype.Service
public class StrategyBuilder {
    private final ObjectMapper objectMapper;

    public StrategyBuilder() {
        this.objectMapper = new ObjectMapper();
    }

    public String buildFromActions(List<Action> actions) {
        try {
            ObjectNode rootNode = this.objectMapper.createObjectNode();
            ArrayNode strategiesNode = rootNode.putArray("strategies");

            for (Action action : actions) {
                strategiesNode.add(convertActionToJson(action));
            }

            return this.objectMapper.writeValueAsString(rootNode);

        } catch (Exception e) {
            throw new StrategyConversionException("Error converting actions to JSON strategy", e);
        }
    }

    public String buildFromEvents(List<Event> events) {
        try {
            ObjectNode rootNode = this.objectMapper.createObjectNode();
            ArrayNode strategiesNode = rootNode.putArray("strategies");

            for (Event event : events) {
                strategiesNode.add(convertEventToJson(event));
            }

            return this.objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new StrategyConversionException("Error converting events to JSON strategy", e);
        }
    }

    private ObjectNode convertActionToJson(Action action) {
        ObjectNode actionNode = this.objectMapper.createObjectNode();

        if (action instanceof ResourceAction resourceAction) {
            actionNode.put("entityId", resourceAction.getEntityId().toString());
            actionNode.put("actionClass", ActionClass.Resource.name());
            actionNode.put("actionType", resourceAction.getActionType().name());

            switch (resourceAction) {
                case ResourceCreationAction resourceCreationAction ->
                        actionNode.set("params", objectMapper.valueToTree(resourceCreationAction.getResourceToSave()));
                case ResourceUpdateAction resourceUpdateAction ->
                        actionNode.set("params", objectMapper.valueToTree(resourceUpdateAction.getResourceToUpdate()));
                case ResourceCustomAction resourceCustomAction -> {
                    if (resourceCustomAction.getQuery() != null) {
                        actionNode.put("query", ((ResourceCustomAction) resourceAction).getQuery());
                    }
                }
                default -> {
                }
            }
        }
        else if (action instanceof ServiceAction serviceAction) {
            actionNode.put("entityId", serviceAction.getEntityId().toString());
            actionNode.put("actionClass", ActionClass.Service.name());
            actionNode.put("actionType", serviceAction.getActionType().name());

            switch (serviceAction.getActionType()) {
                case CREATE -> {
                    ServiceCreationAction createAction = (ServiceCreationAction) serviceAction;
                    actionNode.set("params", objectMapper.valueToTree(createAction.getServicesToSave()));
                }
                case UPDATE -> {
                    ServiceUpdateAction updateAction = (ServiceUpdateAction) serviceAction;
                    actionNode.set("params", objectMapper.valueToTree(updateAction.getServicesToUpdate()));
                }
                case CUSTOM -> {
                    ServiceCustomAction customAction = (ServiceCustomAction) serviceAction;
                    actionNode.put("query", customAction.getQuery());
                }
            }
        }
        return actionNode;
    }

    private ObjectNode convertEventToJson(Event event) {
        ObjectNode eventNode;

        if (event instanceof ResourceEvent resourceEvent) {
            eventNode = convertActionToJson(resourceEvent.getAction());
            eventNode.put("eventStartDateTime", resourceEvent.getEventStartDateTime().toString());
        }
        else if (event instanceof ServiceEvent serviceEvent) {
            eventNode = convertActionToJson(serviceEvent.getAction());
            eventNode.put("eventStartDateTime", serviceEvent.getEventStartDateTime().toString());
        }
        else {
            throw new StrategyConversionException("Unknown event type");
        }
        return eventNode;
    }
}