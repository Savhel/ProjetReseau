package yowyob.resource.management.helpers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class LogbackHighlighter extends CompositeConverter<ILoggingEvent> {
    private static final String CYAN_COLOR = "\033[36m";        // Purple
    private static final String RED_COLOR = "\u001B[31m";       // Red
    private static final String RESET_COLOR = "\u001B[0m";      // Reset
    private static final String YELLOW_COLOR = "\033[33m";      // Yellow
    private static final String PURPLE_COLOR = "\033[35m";      // Purple
    private static final String BLUE_COLOR = "\u001B[34m";      // Blue
    private static final String GREEN_COLOR = "\u001B[32m";     // Green

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("\\b(ALLOWED)\\b");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile("\\b(FORBIDDEN)\\b");
    private static final Pattern PRODUCT_PATTERN = Pattern.compile("\\b(Resource|Service)\\b");
    private static final Pattern ACTION_TYPE_PATTERN = Pattern.compile("\\b(CREATE|READ|UPDATE|DELETE|CUSTOM)\\b");
    private static final Pattern UUID_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

    @Override
    protected String transform(ILoggingEvent event, String in) {
        String message = in;

        message = this.HashAndReplaceUUIDs(message, 5);
        message = replacePattern(message, UUID_PATTERN, CYAN_COLOR + "$0" + RESET_COLOR);
        message = replacePattern(message, ALLOWED_PATTERN, GREEN_COLOR + "$1" + GREEN_COLOR);
        message = replacePattern(message, FORBIDDEN_PATTERN, RED_COLOR + "$1" + RESET_COLOR);
        message = replacePattern(message, PRODUCT_PATTERN, YELLOW_COLOR + "$1" + RESET_COLOR);
        message = replacePattern(message, ACTION_TYPE_PATTERN, PURPLE_COLOR + "$1" + RESET_COLOR);
        return message + "\n";
    }

    private String replacePattern(String input, Pattern pattern, String replacement) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    private String HashAndReplaceUUIDs(String input, int limit) {
        return UUID_PATTERN
                .matcher(input)
                .replaceAll(matchResult -> CYAN_COLOR + hashUUID(matchResult.group(), limit) + RESET_COLOR);
    }

    private String hashUUID(String uuid, int limit) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uuid.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.substring(0, limit);
        } catch (NoSuchAlgorithmException e) {
            throw  new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}