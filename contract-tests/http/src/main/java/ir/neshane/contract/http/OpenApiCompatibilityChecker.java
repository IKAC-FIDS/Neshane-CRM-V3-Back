package ir.neshane.contract.http;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OpenApiCompatibilityChecker {

    public List<String> breakingChanges(OpenAPI baseline, OpenAPI candidate) {
        List<String> changes = new ArrayList<>();
        comparePaths(baseline, candidate, changes);
        compareSchemas(baseline, candidate, changes);
        return changes;
    }

    private void comparePaths(OpenAPI baseline, OpenAPI candidate, List<String> changes) {
        if (baseline.getPaths() == null) {
            return;
        }
        baseline.getPaths().forEach((path, baselineItem) -> {
            PathItem candidateItem = candidate.getPaths() == null ? null : candidate.getPaths().get(path);
            if (candidateItem == null) {
                changes.add("removed path " + path);
                return;
            }
            baselineItem.readOperationsMap().forEach((method, baselineOperation) -> {
                Operation candidateOperation = candidateItem.readOperationsMap().get(method);
                String location = method.name() + " " + path;
                if (candidateOperation == null) {
                    changes.add("removed operation " + location);
                    return;
                }
                compareOperation(
                        location,
                        baseline,
                        candidate,
                        baselineItem,
                        candidateItem,
                        baselineOperation,
                        candidateOperation,
                        changes);
            });
        });
    }

    private void compareOperation(
            String location,
            OpenAPI baselineApi,
            OpenAPI candidateApi,
            PathItem baselineItem,
            PathItem candidateItem,
            Operation baseline,
            Operation candidate,
            List<String> changes) {
        Map<String, Parameter> baselineParameters = parameters(baselineApi, baselineItem, baseline);
        Map<String, Parameter> candidateParameters = parameters(candidateApi, candidateItem, candidate);
        baselineParameters.forEach((key, parameter) -> {
            Parameter next = candidateParameters.get(key);
            if (next == null) {
                changes.add(location + ": removed parameter " + key);
            } else if (!Boolean.TRUE.equals(parameter.getRequired())
                    && Boolean.TRUE.equals(next.getRequired())) {
                changes.add(location + ": optional parameter became required " + key);
            } else if (parameter.getSchema() != null && next.getSchema() != null) {
                compareSchema(
                        location + " parameter " + key,
                        parameter.getSchema(),
                        next.getSchema(),
                        changes);
            }
        });
        candidateParameters.forEach((key, parameter) -> {
            if (!baselineParameters.containsKey(key) && Boolean.TRUE.equals(parameter.getRequired())) {
                changes.add(location + ": new required parameter " + key);
            }
        });
        if (baseline.getRequestBody() != null
                && !Boolean.TRUE.equals(baseline.getRequestBody().getRequired())
                && candidate.getRequestBody() != null
                && Boolean.TRUE.equals(candidate.getRequestBody().getRequired())) {
            changes.add(location + ": optional request body became required");
        }
        if (baseline.getRequestBody() != null && candidate.getRequestBody() == null) {
            changes.add(location + ": removed request body");
        } else if (baseline.getRequestBody() != null) {
            compareContent(
                    location + " request",
                    baseline.getRequestBody().getContent(),
                    candidate.getRequestBody().getContent(),
                    changes);
        }
        if (baseline.getResponses() != null) {
            baseline.getResponses().keySet().forEach(status -> {
                ApiResponse candidateResponse =
                        candidate.getResponses() == null ? null : candidate.getResponses().get(status);
                if (candidateResponse == null) {
                    changes.add(location + ": removed response " + status);
                } else {
                    compareContent(
                            location + " response " + status,
                            baseline.getResponses().get(status).getContent(),
                            candidateResponse.getContent(),
                            changes);
                }
            });
        }
    }

    private void compareContent(
            String location, Content baseline, Content candidate, List<String> changes) {
        if (baseline == null) {
            return;
        }
        baseline.forEach((mediaTypeName, baselineMediaType) -> {
            MediaType candidateMediaType = candidate == null ? null : candidate.get(mediaTypeName);
            if (candidateMediaType == null) {
                changes.add(location + ": removed media type " + mediaTypeName);
            } else if (baselineMediaType.getSchema() != null
                    && candidateMediaType.getSchema() != null) {
                compareSchema(
                        location + " " + mediaTypeName,
                        baselineMediaType.getSchema(),
                        candidateMediaType.getSchema(),
                        changes);
            }
        });
    }

    private Map<String, Parameter> parameters(
            OpenAPI api, PathItem pathItem, Operation operation) {
        Map<String, Parameter> parameters = new HashMap<>();
        addParameters(api, parameters, pathItem.getParameters());
        addParameters(api, parameters, operation.getParameters());
        return parameters;
    }

    private void addParameters(
            OpenAPI api, Map<String, Parameter> target, List<Parameter> source) {
        if (source == null) {
            return;
        }
        source.stream()
                .map(parameter -> resolveParameter(api, parameter))
                .filter(Objects::nonNull)
                .forEach(parameter ->
                        target.put(parameter.getIn() + ":" + parameter.getName(), parameter));
    }

    private Parameter resolveParameter(OpenAPI api, Parameter parameter) {
        if (parameter.get$ref() == null) {
            return parameter;
        }
        String prefix = "#/components/parameters/";
        if (!parameter.get$ref().startsWith(prefix)
                || api.getComponents() == null
                || api.getComponents().getParameters() == null) {
            return null;
        }
        return api.getComponents().getParameters().get(parameter.get$ref().substring(prefix.length()));
    }

    private void compareSchemas(OpenAPI baseline, OpenAPI candidate, List<String> changes) {
        Map<String, Schema<?>> baselineSchemas = schemas(baseline);
        Map<String, Schema<?>> candidateSchemas = schemas(candidate);
        baselineSchemas.forEach((name, schema) -> {
            Schema<?> next = candidateSchemas.get(name);
            if (next == null) {
                changes.add("removed schema " + name);
                return;
            }
            compareSchema("schema " + name, schema, next, changes);
        });
    }

    private Map<String, Schema<?>> schemas(OpenAPI api) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
            return Map.of();
        }
        return schemaMap(api.getComponents().getSchemas());
    }

    private void compareSchema(
            String location, Schema<?> baseline, Schema<?> candidate, List<String> changes) {
        if (!Objects.equals(baseline.get$ref(), candidate.get$ref())) {
            changes.add(location + ": schema reference changed from " + baseline.get$ref() + " to "
                    + candidate.get$ref());
        }
        if (!Objects.equals(typeOf(baseline), typeOf(candidate))) {
            changes.add(location + ": type changed from " + typeOf(baseline) + " to " + typeOf(candidate));
        }
        if (!Objects.equals(baseline.getFormat(), candidate.getFormat())) {
            changes.add(location + ": format changed from " + baseline.getFormat() + " to "
                    + candidate.getFormat());
        }
        Set<?> baselineEnum = baseline.getEnum() == null ? Set.of() : new HashSet<>(baseline.getEnum());
        Set<?> candidateEnum = candidate.getEnum() == null ? Set.of() : new HashSet<>(candidate.getEnum());
        if (!candidateEnum.containsAll(baselineEnum)) {
            changes.add(location + ": enum values were removed");
        }
        if (!Objects.equals(baseline.getDefault(), candidate.getDefault())) {
            changes.add(location + ": default value changed");
        }
        compareComposition(location, "allOf", baseline.getAllOf(), candidate.getAllOf(), changes);
        compareComposition(location, "oneOf", baseline.getOneOf(), candidate.getOneOf(), changes);
        compareComposition(location, "anyOf", baseline.getAnyOf(), candidate.getAnyOf(), changes);
        if (baseline.getItems() != null && candidate.getItems() == null) {
            changes.add(location + ": removed array items schema");
        } else if (baseline.getItems() != null) {
            compareSchema(location + " items", baseline.getItems(), candidate.getItems(), changes);
        }
        if (!Objects.equals(baseline.getDiscriminator(), candidate.getDiscriminator())) {
            changes.add(location + ": discriminator changed");
        }

        Map<String, Schema<?>> baselineProperties = schemaMap(baseline.getProperties());
        Map<String, Schema<?>> candidateProperties = schemaMap(candidate.getProperties());
        baselineProperties.forEach((name, property) -> {
            Schema<?> next = candidateProperties.get(name);
            if (next == null) {
                changes.add(location + ": removed property " + name);
            } else {
                compareSchema(location + "." + name, property, next, changes);
            }
        });

        Set<String> baselineRequired = baseline.getRequired() == null
                ? Set.of()
                : new HashSet<>(baseline.getRequired());
        Set<String> candidateRequired = candidate.getRequired() == null
                ? Set.of()
                : new HashSet<>(candidate.getRequired());
        candidateRequired.stream()
                .filter(required -> !baselineRequired.contains(required))
                .forEach(required -> changes.add(location + ": new required property " + required));
        baselineRequired.stream()
                .filter(required -> !candidateRequired.contains(required))
                .forEach(required -> changes.add(location + ": required property became optional " + required));
    }

    private void compareComposition(
            String location,
            String keyword,
            List<?> baselineSource,
            List<?> candidateSource,
            List<String> changes) {
        List<Schema<?>> baseline = schemaList(baselineSource);
        List<Schema<?>> candidate = schemaList(candidateSource);
        if (baseline.size() != candidate.size()) {
            changes.add(location + ": " + keyword + " member count changed");
            return;
        }
        for (int index = 0; index < baseline.size(); index++) {
            compareSchema(
                    location + " " + keyword + "[" + index + "]",
                    baseline.get(index),
                    candidate.get(index),
                    changes);
        }
    }

    private Set<String> typeOf(Schema<?> schema) {
        if (schema.getTypes() != null && !schema.getTypes().isEmpty()) {
            return schema.getTypes();
        }
        return schema.getType() == null ? Set.of() : Set.of(schema.getType());
    }

    private Map<String, Schema<?>> schemaMap(Map<?, ?> source) {
        if (source == null) {
            return Map.of();
        }
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        source.forEach((name, schema) -> {
            if (name instanceof String schemaName && schema instanceof Schema<?> typedSchema) {
                schemas.put(schemaName, typedSchema);
            }
        });
        return schemas;
    }

    private List<Schema<?>> schemaList(List<?> source) {
        if (source == null) {
            return List.of();
        }
        List<Schema<?>> schemas = new ArrayList<>();
        source.forEach(value -> {
            if (value instanceof Schema<?> schema) {
                schemas.add(schema);
            }
        });
        return schemas;
    }
}
