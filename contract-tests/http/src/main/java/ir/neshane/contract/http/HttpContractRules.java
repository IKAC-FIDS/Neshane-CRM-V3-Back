package ir.neshane.contract.http;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class HttpContractRules {

    private static final Pattern PUBLIC_PATH =
            Pattern.compile("^/api/v[1-9][0-9]*(?:/[a-z0-9][a-z0-9-]*|/\\{[a-z][A-Za-z0-9]*})+$");
    private static final Pattern CONTRACT_VERSION = Pattern.compile("^v[1-9][0-9]*$");
    private static final List<String> COMMON_SCHEMAS =
            List.of("ProblemDetails", "ValidationProblemDetails", "PageMetadata");

    public List<String> validate(OpenAPI api) {
        List<String> violations = new ArrayList<>();
        requireExtension(api, "x-neshane-contract-owner", violations);
        requireExtension(api, "x-neshane-contract-version", violations);
        requireExtension(api, "x-neshane-contract-status", violations);
        Object version = extension(api, "x-neshane-contract-version");
        if (version != null && !CONTRACT_VERSION.matcher(version.toString()).matches()) {
            violations.add("x-neshane-contract-version must use v<major>");
        }
        if (!"3.1.0".equals(api.getOpenapi())) {
            violations.add("OpenAPI version must be pinned to 3.1.0");
        }

        if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
            Map<String, Schema<?>> schemas = schemaMap(api.getComponents().getSchemas());
            if (schemas.keySet().stream().anyMatch(COMMON_SCHEMAS::contains)) {
                COMMON_SCHEMAS.stream()
                        .filter(name -> !schemas.containsKey(name))
                        .forEach(name -> violations.add("common component missing schema " + name));
                validateProblemDetails(schemas.get("ProblemDetails"), violations);
            }
        }

        if (api.getPaths() == null) {
            return violations;
        }
        api.getPaths().forEach((path, item) -> validatePath(api, path, item, violations));
        return violations;
    }

    private void validatePath(
            OpenAPI api, String path, PathItem item, List<String> violations) {
        if (!PUBLIC_PATH.matcher(path).matches()) {
            violations.add(path + ": public path must use lowercase /api/v<major>/<plural-resource>");
        }
        item.readOperationsMap().forEach((method, operation) -> {
            String location = method.name() + " " + path;
            if (operation.getOperationId() == null || operation.getOperationId().isBlank()) {
                violations.add(location + ": operationId is required");
            }
            if (!hasSecurity(api, operation)
                    && !Boolean.TRUE.equals(extension(operation, "x-public-access-approved"))) {
                violations.add(location + ": security or x-public-access-approved is required");
            }
            validateResponses(location, operation, violations);
            if (Boolean.TRUE.equals(extension(operation, "x-neshane-paginated"))) {
                requireParameters(api, item, operation, location, violations, "page", "size", "sort");
            }
            if (Boolean.TRUE.equals(extension(operation, "x-neshane-idempotent"))) {
                requireParameters(api, item, operation, location, violations, "Idempotency-Key");
                if (operation.getResponses() == null
                        || !operation.getResponses().containsKey("409")) {
                    violations.add(location + ": idempotent operation requires a 409 response");
                }
            }
        });
    }

    private void validateResponses(
            String location, Operation operation, List<String> violations) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            violations.add(location + ": at least one response is required");
            return;
        }
        operation.getResponses().forEach((status, response) -> {
            if (!hasHeader(response, "X-Correlation-ID")) {
                violations.add(location + " response " + status + ": X-Correlation-ID is required");
            }
            if (isErrorStatus(status)
                    && (response.getContent() == null
                            || !response.getContent().containsKey("application/problem+json"))) {
                violations.add(location + " response " + status
                        + ": errors require application/problem+json");
            }
        });
    }

    private void requireParameters(
            OpenAPI api,
            PathItem item,
            Operation operation,
            String location,
            List<String> violations,
            String... names) {
        List<Parameter> parameters = new ArrayList<>();
        parameters.addAll(item.getParameters() == null ? List.of() : item.getParameters());
        parameters.addAll(operation.getParameters() == null ? List.of() : operation.getParameters());
        for (String name : names) {
            boolean present = parameters.stream()
                    .map(parameter -> resolveParameter(api, parameter))
                    .filter(Objects::nonNull)
                    .anyMatch(parameter -> name.equalsIgnoreCase(parameter.getName()));
            if (!present) {
                violations.add(location + ": required convention parameter missing " + name);
            }
        }
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

    private void validateProblemDetails(Schema<?> schema, List<String> violations) {
        if (schema == null) {
            return;
        }
        List<String> required = schema.getRequired() == null ? List.of() : schema.getRequired();
        for (String field : List.of(
                "type", "title", "status", "detail", "instance", "code", "correlationId")) {
            if (!required.contains(field)) {
                violations.add("ProblemDetails must require " + field);
            }
        }
    }

    private Map<String, Schema<?>> schemaMap(Map<?, ?> source) {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        source.forEach((name, schema) -> {
            if (name instanceof String schemaName && schema instanceof Schema<?> typedSchema) {
                schemas.put(schemaName, typedSchema);
            }
        });
        return schemas;
    }

    private boolean hasSecurity(OpenAPI api, Operation operation) {
        return operation.getSecurity() != null && !operation.getSecurity().isEmpty()
                || api.getSecurity() != null && !api.getSecurity().isEmpty();
    }

    private boolean hasHeader(ApiResponse response, String expected) {
        return response.getHeaders() != null
                && response.getHeaders().keySet().stream().anyMatch(expected::equalsIgnoreCase);
    }

    private boolean isErrorStatus(String status) {
        try {
            return Integer.parseInt(status) >= 400;
        } catch (NumberFormatException ignored) {
            return "default".equals(status.toLowerCase(Locale.ROOT));
        }
    }

    private void requireExtension(OpenAPI api, String name, List<String> violations) {
        Object value = extension(api, name);
        if (value == null || value.toString().isBlank()) {
            violations.add(name + " is required");
        }
    }

    private Object extension(OpenAPI api, String name) {
        return api.getExtensions() == null ? null : api.getExtensions().get(name);
    }

    private Object extension(Operation operation, String name) {
        return operation.getExtensions() == null ? null : operation.getExtensions().get(name);
    }
}
