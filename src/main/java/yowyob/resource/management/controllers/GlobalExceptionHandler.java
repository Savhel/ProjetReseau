package yowyob.resource.management.controllers;

import java.util.Map;
import java.util.HashMap;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import yowyob.resource.management.exceptions.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yowyob.resource.management.exceptions.invalid.*;
import yowyob.resource.management.exceptions.policy.ExecutorPolicyViolationException;
import yowyob.resource.management.exceptions.policy.PolicyViolationException;
import yowyob.resource.management.exceptions.policy.UpdaterPolicyViolationException;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({UpdaterPolicyViolationException.class, ExecutorPolicyViolationException.class})
    public ResponseEntity<Map<String, String>> handlePolicyViolationException(PolicyViolationException ex) {
        logger.error(ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidActionClassException.class, InvalidEventClassException.class, InvalidEventException.class, InvalidJsonFormatException.class, MissingParameterException.class})
    public ResponseEntity<Map<String, String>> handleInvalidInputException(InvalidInputException ex) {
        logger.error(ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(StrategyConversionException.class)
    public ResponseEntity<Map<String, String>> handleStrategyConversionException(StrategyConversionException ex) {
        logger.error(ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error(ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        response.put("status", status.toString());
        return new ResponseEntity<>(response, status);
    }
}
