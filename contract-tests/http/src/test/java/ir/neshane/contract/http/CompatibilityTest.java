package ir.neshane.contract.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CompatibilityTest {

    private final OpenApiCompatibilityChecker checker = new OpenApiCompatibilityChecker();

    @Test
    void additiveContractChangeIsCompatible() throws Exception {
        var baseline = OpenApiLoader.load(resource("valid-baseline.openapi.yaml"));
        var candidate = OpenApiLoader.load(resource("compatible-candidate.openapi.yaml"));

        assertThat(checker.breakingChanges(baseline, candidate)).isEmpty();
    }

    @Test
    void breakingContractChangesAreRejected() throws Exception {
        var baseline = OpenApiLoader.load(resource("valid-baseline.openapi.yaml"));
        var candidate = OpenApiLoader.load(resource("breaking-candidate.openapi.yaml"));

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("removed operation GET /api/v1/examples"))
                .anyMatch(change -> change.contains("removed property displayName"))
                .anyMatch(change -> change.contains("new required property internalCode"))
                .anyMatch(change -> change.contains("required property became optional id"))
                .anyMatch(change -> change.contains("enum values were removed"))
                .anyMatch(change -> change.contains("schema reference changed"));
    }

    @Test
    void newlyRequiredRequestParameterIsRejected() throws Exception {
        var baseline = OpenApiLoader.load(resource("valid-baseline.openapi.yaml"));
        var candidate = OpenApiLoader.load(resource("compatible-candidate.openapi.yaml"));
        candidate.getPaths()
                .get("/api/v1/examples")
                .getGet()
                .addParametersItem(new Parameter()
                        .name("requiredFilter")
                        .in("query")
                        .required(true)
                        .schema(new StringSchema()));

        assertThat(checker.breakingChanges(baseline, candidate))
                .anyMatch(change -> change.contains("new required parameter query:requiredFilter"));
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
