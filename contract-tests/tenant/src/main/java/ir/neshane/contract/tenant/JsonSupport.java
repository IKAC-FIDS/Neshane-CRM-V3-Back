package ir.neshane.contract.tenant;

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
            throw new TenantContractException("Unable to read JSON document: " + path);
        }
    }

    List<String> validate(JsonNode schemaNode, JsonNode instance) {
        try {
            Schema schema = schemaRegistry.getSchema(schemaNode);
            return schema.validate(instance).stream()
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
        if (!schema.path("type").isTextual()) {
            violations.add(location + ": root type is required");
        }
        validate(schema, mapper.createObjectNode()).stream()
                .filter(message -> message.startsWith("invalid JSON Schema:"))
                .map(message -> location + ": " + message)
                .forEach(violations::add);
        return violations;
    }
}
