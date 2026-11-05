package ir.neshane.contract.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EventContractVerifier {

    private EventContractVerifier() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new EventContractException("Expected validate, validate-event or diff command");
        }
        switch (args[0]) {
            case "validate" -> validate(Path.of(required(args, 1)));
            case "validate-event" ->
                validateEvent(Path.of(required(args, 1)), Path.of(required(args, 2)));
            case "diff" -> diff(Path.of(required(args, 1)), Path.of(required(args, 2)));
            default -> throw new EventContractException("Unknown command: " + args[0]);
        }
    }

    static void validate(Path root) {
        JsonSupport json = new JsonSupport();
        SensitiveFieldPolicy sensitiveFields = new SensitiveFieldPolicy();
        EventContractCatalog catalog = new EventContractCatalog(root, json);
        List<String> violations = new ArrayList<>();

        Path manifestSchema = root.resolve("manifest.schema.json");
        Path envelopeSchema = root.resolve("envelope/v1/schema.json");
        validateSchema(json, manifestSchema, null, sensitiveFields, false, violations);
        validateSchema(json, envelopeSchema, "urn:neshane:event-envelope:v1", sensitiveFields, false, violations);
        for (EventContractEntry entry : catalog.entries()) {
            validateSchema(
                    json,
                    entry.schemaPath(),
                    entry.schemaId(),
                    sensitiveFields,
                    true,
                    violations);
        }
        if (!violations.isEmpty()) {
            throw new EventContractException("Event contract validation failed:", violations);
        }
        System.out.println("Validated event manifest, envelope and " + catalog.entries().size()
                + " payload schema(s)");
    }

    static void validateEvent(Path root, Path eventPath) {
        JsonSupport json = new JsonSupport();
        EventContractCatalog catalog = new EventContractCatalog(root, json);
        JsonNode event = json.read(eventPath);
        List<String> violations = new ArrayList<>();
        json.validate(json.read(root.resolve("envelope/v1/schema.json")), event).stream()
                .map(message -> eventPath + ": envelope " + message)
                .forEach(violations::add);
        if (!violations.isEmpty()) {
            throw new EventContractException("Invalid event envelope:", violations);
        }

        EventContractEntry entry = catalog.find(
                event.path("type").asText(), event.path("payloadversion").asText());
        if (!entry.schemaId().equals(event.path("dataschema").asText())) {
            violations.add("dataschema does not match the manifest schemaId");
        }
        if ("required".equals(entry.tenantMode())
                && (!event.path("tenantid").isTextual()
                        || event.path("tenantid").asText().isBlank())) {
            violations.add("tenantid is required by the manifest entry");
        } else if ("forbidden".equals(entry.tenantMode()) && event.has("tenantid")) {
            violations.add("tenantid is forbidden by the manifest entry");
        }
        json.validate(json.read(entry.schemaPath()), event.path("data")).stream()
                .map(message -> "data: " + message)
                .forEach(violations::add);
        new SensitiveFieldPolicy().inspectPayload(event.path("data"), "data").forEach(violations::add);
        if (!violations.isEmpty()) {
            throw new EventContractException("Event payload validation failed:", violations);
        }
        System.out.println("Validated event " + entry.key());
    }

    static void diff(Path baselinePath, Path candidatePath) {
        JsonSupport json = new JsonSupport();
        List<String> changes = new EventSchemaCompatibilityChecker()
                .breakingChanges(json.read(baselinePath), json.read(candidatePath));
        if (!changes.isEmpty()) {
            throw new EventContractException("Breaking event schema changes detected:", changes);
        }
        System.out.println("No consumer-backward breaking event schema changes detected");
    }

    private static void validateSchema(
            JsonSupport json,
            Path path,
            String expectedId,
            SensitiveFieldPolicy sensitiveFields,
            boolean inspectSensitiveFields,
            List<String> violations) {
        JsonNode schema = json.read(path);
        json.validateSchemaDeclaration(schema, path.toString()).forEach(violations::add);
        if (expectedId != null && !expectedId.equals(schema.path("$id").asText())) {
            violations.add(path + ": $id does not match the manifest schemaId");
        }
        if (inspectSensitiveFields) {
            sensitiveFields.inspectSchema(schema, path.toString()).forEach(violations::add);
        }
    }

    private static String required(String[] args, int index) {
        if (args.length <= index || args[index].isBlank()) {
            throw new EventContractException("Missing command argument " + index);
        }
        return args[index];
    }
}
