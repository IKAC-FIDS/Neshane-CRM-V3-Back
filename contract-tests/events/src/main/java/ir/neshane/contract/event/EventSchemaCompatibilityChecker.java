package ir.neshane.contract.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EventSchemaCompatibilityChecker {

    private static final List<String> MANUAL_REVIEW_KEYWORDS = List.of(
            "allOf",
            "oneOf",
            "anyOf",
            "not",
            "if",
            "then",
            "else",
            "$dynamicRef",
            "$defs",
            "definitions",
            "patternProperties",
            "propertyNames",
            "prefixItems",
            "contains",
            "minContains",
            "maxContains",
            "unevaluatedProperties",
            "dependentSchemas",
            "dependentRequired",
            "const",
            "default",
            "minProperties",
            "maxProperties",
            "contentEncoding",
            "contentMediaType",
            "contentSchema");
    private static final List<String> CONSTRAINT_KEYWORDS = List.of(
            "minimum",
            "maximum",
            "exclusiveMinimum",
            "exclusiveMaximum",
            "multipleOf",
            "minLength",
            "maxLength",
            "pattern",
            "minItems",
            "maxItems",
            "uniqueItems",
            "additionalProperties");
    private static final Set<String> KNOWN_KEYWORDS = Set.of(
            "$schema",
            "$id",
            "$anchor",
            "$dynamicAnchor",
            "$ref",
            "$dynamicRef",
            "$defs",
            "definitions",
            "$comment",
            "title",
            "description",
            "examples",
            "deprecated",
            "readOnly",
            "writeOnly",
            "type",
            "format",
            "enum",
            "const",
            "default",
            "required",
            "properties",
            "patternProperties",
            "additionalProperties",
            "unevaluatedProperties",
            "propertyNames",
            "dependentSchemas",
            "dependentRequired",
            "dependencies",
            "items",
            "prefixItems",
            "contains",
            "minContains",
            "maxContains",
            "allOf",
            "oneOf",
            "anyOf",
            "not",
            "if",
            "then",
            "else",
            "minimum",
            "maximum",
            "exclusiveMinimum",
            "exclusiveMaximum",
            "multipleOf",
            "minLength",
            "maxLength",
            "pattern",
            "minItems",
            "maxItems",
            "uniqueItems",
            "minProperties",
            "maxProperties",
            "contentEncoding",
            "contentMediaType",
            "contentSchema");

    public List<String> breakingChanges(JsonNode baseline, JsonNode candidate) {
        List<String> changes = new ArrayList<>();
        compareSchema("schema", baseline, candidate, changes);
        return changes;
    }

    private void compareSchema(
            String location, JsonNode baseline, JsonNode candidate, List<String> changes) {
        if (baseline.isBoolean() || candidate.isBoolean()) {
            if (!baseline.equals(candidate)) {
                changes.add(location + ": boolean schema changed; manual review required");
            }
            return;
        }
        if (!baseline.isObject() || !candidate.isObject()) {
            if (!baseline.equals(candidate)) {
                changes.add(location + ": schema shape changed; manual review required");
            }
            return;
        }

        compareValue(location, "$schema", baseline, candidate, changes, "schema dialect changed");
        compareValue(location, "$id", baseline, candidate, changes, "schema identity changed");
        compareValue(location, "$ref", baseline, candidate, changes, "schema reference changed");
        compareValue(location, "type", baseline, candidate, changes, "type changed");
        compareValue(location, "format", baseline, candidate, changes, "format changed");

        JsonNode baselineEnumNode = baseline.get("enum");
        JsonNode candidateEnumNode = candidate.get("enum");
        Set<JsonNode> baselineEnum = values(baselineEnumNode);
        Set<JsonNode> candidateEnum = values(candidateEnumNode);
        if (baselineEnumNode != null && candidateEnumNode == null) {
            changes.add(location + ": enum constraint removed and values widened for existing consumers");
        } else if (baselineEnumNode != null
                && candidateEnumNode != null
                && !baselineEnum.containsAll(candidateEnum)) {
            changes.add(location + ": enum widened for existing consumers");
        }

        Set<String> baselineRequired = strings(baseline.get("required"));
        Set<String> candidateRequired = strings(candidate.get("required"));
        candidateRequired.stream()
                .filter(required -> !baselineRequired.contains(required))
                .forEach(required -> changes.add(location + ": new required property " + required));
        baselineRequired.stream()
                .filter(required -> !candidateRequired.contains(required))
                .forEach(required -> changes.add(location + ": removed required property " + required));

        Map<String, JsonNode> baselineProperties = fields(baseline.get("properties"));
        Map<String, JsonNode> candidateProperties = fields(candidate.get("properties"));
        baselineProperties.forEach((name, property) -> {
            JsonNode next = candidateProperties.get(name);
            if (next == null) {
                changes.add(location + ": removed property " + name);
            } else {
                compareSchema(location + "." + name, property, next, changes);
            }
        });
        candidateProperties.keySet().stream()
                .filter(name -> !baselineProperties.containsKey(name))
                .filter(name -> rejectsNewProperty(baseline))
                .forEach(name -> changes.add(location + ": new property " + name
                        + " is rejected or cannot be safely assessed by the baseline schema"));

        JsonNode baselineItems = baseline.get("items");
        JsonNode candidateItems = candidate.get("items");
        if (baselineItems != null && candidateItems == null) {
            changes.add(location + ": removed array items schema");
        } else if (baselineItems != null && candidateItems != null) {
            compareSchema(location + " items", baselineItems, candidateItems, changes);
        } else if (baselineItems == null && candidateItems != null) {
            changes.add(location + ": array items schema added; manual review required");
        }

        for (String keyword : MANUAL_REVIEW_KEYWORDS) {
            if (!Objects.equals(baseline.get(keyword), candidate.get(keyword))) {
                changes.add(location + ": " + keyword + " changed; manual review required");
            }
        }
        for (String keyword : CONSTRAINT_KEYWORDS) {
            if (!Objects.equals(baseline.get(keyword), candidate.get(keyword))) {
                changes.add(location + ": " + keyword + " changed; manual review required");
            }
        }
        Set<String> encounteredKeywords = new HashSet<>();
        baseline.properties().forEach(entry -> encounteredKeywords.add(entry.getKey()));
        candidate.properties().forEach(entry -> encounteredKeywords.add(entry.getKey()));
        encounteredKeywords.stream()
                .filter(keyword -> !KNOWN_KEYWORDS.contains(keyword))
                .filter(keyword -> !Objects.equals(baseline.get(keyword), candidate.get(keyword)))
                .forEach(keyword -> changes.add(location + ": unsupported keyword " + keyword
                        + " changed; manual review required"));
    }

    private boolean rejectsNewProperty(JsonNode baseline) {
        JsonNode additionalProperties = baseline.get("additionalProperties");
        return (additionalProperties != null && !additionalProperties.isBoolean())
                || (additionalProperties != null && additionalProperties.isBoolean()
                        && !additionalProperties.booleanValue())
                || baseline.path("unevaluatedProperties").isBoolean()
                        && !baseline.path("unevaluatedProperties").booleanValue()
                || baseline.has("patternProperties")
                || baseline.has("propertyNames");
    }

    private void compareValue(
            String location,
            String keyword,
            JsonNode baseline,
            JsonNode candidate,
            List<String> changes,
            String message) {
        if (!Objects.equals(baseline.get(keyword), candidate.get(keyword))) {
            changes.add(location + ": " + message);
        }
    }

    private Set<JsonNode> values(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Set.of();
        }
        Set<JsonNode> values = new HashSet<>();
        node.forEach(values::add);
        return values;
    }

    private Set<String> strings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private Map<String, JsonNode> fields(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, JsonNode> values = new java.util.LinkedHashMap<>();
        node.properties().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        return values;
    }
}
