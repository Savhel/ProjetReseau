package yowyob.resource.management.services.strategy;


import com.fasterxml.jackson.databind.JsonNode;
import yowyob.resource.management.commons.Command;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.actions.Action;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.actions.enums.ActionType;
import yowyob.resource.management.actions.enums.ActionClass;
import yowyob.resource.management.events.service.ServiceEvent;
import yowyob.resource.management.actions.service.operations.*;
import yowyob.resource.management.actions.resource.operations.*;
import yowyob.resource.management.events.resource.ResourceEvent;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.actions.resource.ResourceAction;
import yowyob.resource.management.exceptions.MissingParameterException;
import yowyob.resource.management.exceptions.StrategyConversionException;
import yowyob.resource.management.exceptions.invalid.InvalidJsonFormatException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@org.springframework.stereotype.Service
public class StrategyConverter {
    private final ObjectMapper objectMapper;

    public StrategyConverter() {
        this.objectMapper = new ObjectMapper();
    }

    private Command convertJsonNodeToCommand(JsonNode node) {

        try{
            if (!node.has("entityId") || !node.has("actionType")) {
                throw new InvalidJsonFormatException("Missing 'entityId' or 'actionType' fields in the input JSON.");
            }

            UUID entityId = UUID.fromString(node.get("entityId").asText());

            ActionType actionType;
            try {
                actionType = ActionType.valueOf(node.get("actionType").asText());
            } catch (IllegalArgumentException e) {
                throw new StrategyConversionException("Invalid ActionType specified : " + node.get("actionType").asText(), e);
            }

            ActionClass actionClass = node.has("actionClass")
                    ? ActionClass.valueOf(node.get("actionClass").asText())
                    : ActionClass.Resource;

            LocalDateTime eventStartDateTime = node.has("eventStartDateTime")
                    ? LocalDateTime.parse(node.get("eventStartDateTime").asText())
                    : null;

            JsonNode paramsNode = node.has("params") ? node.get("params") : null;
            String query = node.has("query") ? node.get("query").asText() : null;

            if (eventStartDateTime != null) {
                if (actionClass == ActionClass.Resource) {
                    return new ResourceEvent(this, buildResourceAction(entityId, actionType, paramsNode, query), eventStartDateTime);
                } else {
                    return new ServiceEvent(this, buildServiceAction(entityId, actionType, paramsNode, query), eventStartDateTime);
                }
            } else {
                if (actionClass == ActionClass.Resource) {
                    return buildResourceAction(entityId, actionType, paramsNode, query);
                }else {
                    return buildServiceAction(entityId, actionType, paramsNode, query);
                }
            }

        }catch (IllegalArgumentException e) {
            throw new StrategyConversionException("Error converting action: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StrategyConversionException("Unknown error during JSON → Action conversion.", e);
        }

    }

    public ServiceAction buildServiceAction(UUID entityId, ActionType actionType, JsonNode paramsNode, String query) {
        return switch (actionType) {
            case CREATE -> {
                if (paramsNode == null) {
                    throw new MissingParameterException("Parameters are required for the CREATE action.");
                }
                yield new ServiceCreationAction(parseServiceFromParams(paramsNode));
            }
            case READ -> new ServiceReadingAction(entityId);
            case UPDATE -> {
                if (paramsNode == null) {
                    throw new MissingParameterException("Parameters are required for the UPDATE action.");
                }
                yield new ServiceUpdateAction(parseServiceFromParams(paramsNode));
            }
            case DELETE -> new ServiceDeletionAction(entityId);
            case CUSTOM -> {
                if (query == null) {
                    throw new MissingParameterException("Query is required for the CUSTOM action.");
                }
                yield new ServiceCustomAction(entityId, query);
            }
        };
    }

    public ResourceAction buildResourceAction(UUID entityId, ActionType actionType, JsonNode paramsNode, String query) {
        return switch (actionType) {
            case CREATE -> {
                if (paramsNode == null) {
                    throw new MissingParameterException("Parameters are required for the CREATE action.");
                }
                yield new ResourceCreationAction(parseResourceFromParams(paramsNode));
            }
            case READ -> new ResourceReadingAction(entityId);
            case UPDATE -> {
                if (paramsNode == null) {
                    throw new MissingParameterException("Parameters are required for the UPDATE action.");
                }
                yield new ResourceUpdateAction(parseResourceFromParams(paramsNode));
            }
            case DELETE -> new ResourceDeletionAction(entityId);
            case CUSTOM -> {
                if (query == null) {
                    throw new MissingParameterException("Parameters are required for the UPDATE action.");
                }
                yield new ResourceCustomAction(entityId, query);
            }
        };
    }

    private Resource parseResourceFromParams(JsonNode paramsNode) {
        try {
            return objectMapper.treeToValue(paramsNode, Resource.class);
        } catch (IOException e) {
            throw new InvalidJsonFormatException("Error converting JSON → Resource: " + e.getMessage(), e);
        }
    }

    private Services parseServiceFromParams(JsonNode paramsNode) {
        try {
            return objectMapper.treeToValue(paramsNode, Services.class);
        } catch (IOException e) {
            throw new InvalidJsonFormatException("Error converting JSON → Services: " + e.getMessage(), e);
        }
    }

    public Map<String, ArrayList<Command>> convertFromJson(String jsonString) throws StrategyConversionException, InvalidJsonFormatException {
        ArrayList<Command> strategyEvents = new ArrayList<>();
        ArrayList<Command> strategyActions = new ArrayList<>();

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonString);
        } catch (IOException ioe) {
            throw new InvalidJsonFormatException(ioe.getMessage());
        }

        if (!rootNode.has("strategies") || !rootNode.get("strategies").isArray()) {
            throw new InvalidJsonFormatException("Missing or invalid 'strategies' array in JSON.");
        }

        JsonNode strategiesNode = rootNode.get("strategies");
        for (JsonNode strategyNode : strategiesNode) {
            try {
                Object result = convertJsonNodeToCommand(strategyNode);

                if (result instanceof Event event) {
                    strategyEvents.add(event);
                } else if (result instanceof Action action) {
                    strategyActions.add(action);
                } else {
                    throw new StrategyConversionException("Unknown type during JSON → Strategy conversion.");
                }
            } catch (Exception e){
                throw new StrategyConversionException("Error during JSON → Strategy conversion.", e);
            }
        }

        Map<String, ArrayList<Command>> converted = new HashMap<>();
        converted.put("actions", strategyActions);
        converted.put("events", strategyEvents);

        return converted;
    }

    public List<Command> convertToCommandListFromJson(String jsonString) throws StrategyConversionException, InvalidJsonFormatException {
        List<Command> convertedList = new ArrayList<>();

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonString);
        } catch (IOException ioe) {
            throw new InvalidJsonFormatException(ioe.getMessage());
        }

        if (!rootNode.has("strategies") || !rootNode.get("strategies").isArray()) {
            throw new InvalidJsonFormatException("Missing or invalid 'strategies' array in JSON.");
        }

        JsonNode strategiesNode = rootNode.get("strategies");
        for (JsonNode strategyNode : strategiesNode) {
            try {
                Object result = convertJsonNodeToCommand(strategyNode);

                if (result instanceof Event event) {
                    convertedList.add(event);
                } else if (result instanceof Action action) {
                    convertedList.add(action);
                } else {
                    throw new StrategyConversionException("Unknown type during JSON → Strategy conversion.");
                }
            } catch (Exception e){
                throw new StrategyConversionException("Error during JSON → Strategy conversion.", e);
            }
        }

        return convertedList;
    }
}
