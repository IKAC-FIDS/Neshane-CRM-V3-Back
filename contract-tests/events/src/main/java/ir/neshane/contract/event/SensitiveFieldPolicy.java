package ir.neshane.contract.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SensitiveFieldPolicy {

    private static final Set<String> FORBIDDEN_PARTS = Set.of(
            "password",
            "accesstoken",
            "refreshtoken",
            "privatekey",
            "otp",
            "otpsecret",
            "recoverycode",
            "credential",
            "credentials",
            "authorization",
            "authorizationcontext",
            "identityclaims");

    List<String> inspectSchema(JsonNode schema, String location) {
        List<String> violations = new ArrayList<>();
        inspectSchemaNode(schema, location, violations);
        return violations;
    }

    List<String> inspectPayload(JsonNode payload, String location) {
        List<String> violations = new ArrayList<>();
        inspectPayloadNode(payload, location, violations);
        return violations;
    }

    private void inspectSchemaNode(
            JsonNode node, String location, List<String> violations) {
        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            properties.properties().forEach(entry -> {
                reject(entry.getKey(), location + ".properties", violations);
                inspectSchemaNode(
                        entry.getValue(), location + ".properties." + entry.getKey(), violations);
            });
        }
        if (node.path("items").isObject()) {
            inspectSchemaNode(node.path("items"), location + ".items", violations);
        }
        for (String keyword : List.of("allOf", "oneOf", "anyOf")) {
            JsonNode values = node.get(keyword);
            if (values != null && values.isArray()) {
                for (int index = 0; index < values.size(); index++) {
                    inspectSchemaNode(values.get(index), location + "." + keyword + "[" + index + "]", violations);
                }
            }
        }
    }

    private void inspectPayloadNode(
            JsonNode node, String location, List<String> violations) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                reject(entry.getKey(), location, violations);
                inspectPayloadNode(entry.getValue(), location + "." + entry.getKey(), violations);
            });
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                inspectPayloadNode(node.get(index), location + "[" + index + "]", violations);
            }
        }
    }

    private void reject(String field, String location, List<String> violations) {
        String normalized = field.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        if (FORBIDDEN_PARTS.stream().anyMatch(normalized::contains)) {
            violations.add(location + ": forbidden sensitive field name " + field);
        }
    }
}
