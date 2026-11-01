package ir.neshane.platform.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TelemetrySanitizer {
    public static final String REDACTED = "[REDACTED]";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_PARTS = Set.of(
            "authorization", "cookie", "credential", "password", "secret", "token", "api_key",
            "apikey", "private_key", "session");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}(?![A-Za-z0-9_-])");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)[\\\"']?(authorization|set[-_]?cookie|cookie|password|secret|token|api[-_]?key|credential|session)"
                    + "[\\\"']?\\s*[:=]\\s*(?:\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;}]+)");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9._%+-])");

    private TelemetrySanitizer() {
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace('-', '_');
        return SENSITIVE_PARTS.stream().anyMatch(normalized::contains);
    }

    public static String sanitizeValue(String key, String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        return truncate(redactSecrets(value), maximumLength);
    }

    public static String sanitizeMessage(String message, int maximumLength) {
        return message == null ? "" : truncate(redactSecrets(redactStructuredValue(message)), maximumLength);
    }

    private static String redactStructuredValue(String value) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            if (root == null || !(root.isObject() || root.isArray())) {
                return value;
            }
            redactNode(root);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException ignored) {
            return value;
        }
    }

    private static void redactNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            var names = new ArrayList<String>();
            object.fieldNames().forEachRemaining(names::add);
            names.forEach(name -> {
                if (isSensitiveKey(name)) {
                    object.set(name, TextNode.valueOf(REDACTED));
                } else {
                    redactNode(object.get(name));
                }
            });
        } else if (node.isArray()) {
            node.forEach(TelemetrySanitizer::redactNode);
        }
    }

    private static String redactSecrets(String value) {
        String withoutBearer = BEARER.matcher(value).replaceAll(REDACTED);
        String withoutJwt = JWT.matcher(withoutBearer).replaceAll(REDACTED);
        String withoutAssignments = SENSITIVE_ASSIGNMENT.matcher(withoutJwt).replaceAll(REDACTED);
        return EMAIL.matcher(withoutAssignments).replaceAll(REDACTED);
    }

    private static String truncate(String value, int maximumLength) {
        if (maximumLength < 1) {
            throw new IllegalArgumentException("maximumLength must be positive");
        }
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength) + "…";
    }
}
