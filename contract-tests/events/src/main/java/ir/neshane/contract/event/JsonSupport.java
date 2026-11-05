package ir.neshane.contract.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

final class JsonSupport {

    static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(
            SpecificationVersion.DRAFT_2020_12,
            builder -> builder.schemaRegistryConfig(
                    SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build()));

    JsonNode read(Path path) {
        try {
            return mapper.readTree(Files.readString(path));
        } catch (IOException exception) {
            throw new EventContractException("Unable to read JSON document: " + path);
        }
    }

    List<String> validate(JsonNode schemaNode, JsonNode instance) {
        try {
            Schema schema = schemaRegistry.getSchema(schemaNode);
            List<Error> messages = schema.validate(instance);
            return messages.stream()
                    .map(Error::getMessage)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (RuntimeException exception) {
            return List.of("invalid JSON Schema: " + exception.getMessage());
        }
    }

    List<String> validateSchemaDeclaration(JsonNode schema, String location) {
        java.util.ArrayList<String> violations = new java.util.ArrayList<>();
        if (!DRAFT_2020_12.equals(schema.path("$schema").asText())) {
            violations.add(location + ": $schema must be Draft 2020-12");
        }
        if (!schema.path("$id").isTextual() || schema.path("$id").asText().isBlank()) {
            violations.add(location + ": non-empty $id is required");
        }
        inspectSchema(schema, location, violations);
        if (violations.isEmpty()) {
            violations.addAll(validate(schema, mapper.createObjectNode()).stream()
                    .filter(message -> message.startsWith("invalid JSON Schema:"))
                    .map(message -> location + ": " + message)
                    .toList());
        }
        return violations;
    }

    private void inspectSchema(
            JsonNode schema, String location, List<String> violations) {
        JsonNode type = schema.get("type");
        if (type != null && !type.isTextual() && !type.isArray()) {
            violations.add(location + ": type must be a string or array");
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && !properties.isObject()) {
            violations.add(location + ": properties must be an object");
        }
        JsonNode required = schema.get("required");
        if (required != null
                && (!required.isArray()
                        || !java.util.stream.StreamSupport.stream(required.spliterator(), false)
                                .allMatch(JsonNode::isTextual))) {
            violations.add(location + ": required must be an array of strings");
        }
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && (!enumNode.isArray() || enumNode.isEmpty())) {
            violations.add(location + ": enum must be a non-empty array");
        }
        if (schema.has("$ref") && !schema.path("$ref").isTextual()) {
            violations.add(location + ": $ref must be a string");
        }
        if (properties != null && properties.isObject()) {
            properties.properties().forEach(entry -> {
                if (!entry.getValue().isObject() && !entry.getValue().isBoolean()) {
                    violations.add(location + ".properties." + entry.getKey()
                            + ": property schema must be an object or boolean");
                } else if (entry.getValue().isObject()) {
                    inspectSchema(
                            entry.getValue(), location + ".properties." + entry.getKey(), violations);
                }
            });
        }
        JsonNode items = schema.get("items");
        if (items != null && items.isObject()) {
            inspectSchema(items, location + ".items", violations);
        } else if (items != null && !items.isBoolean()) {
            violations.add(location + ": items must be an object or boolean");
        }
        for (String keyword : List.of("allOf", "oneOf", "anyOf")) {
            JsonNode composition = schema.get(keyword);
            if (composition != null && !composition.isArray()) {
                violations.add(location + ": " + keyword + " must be an array");
            }
        }
    }
}
