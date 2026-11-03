package ir.neshane.contract.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConventionEnforcementTest {

    @Test
    void reportsSecurityCorrelationErrorPaginationAndIdempotencyViolations() throws Exception {
        var api = OpenApiLoader.load(resource("invalid-conventions.openapi.yaml"));

        assertThat(new HttpContractRules().validate(api))
                .anyMatch(issue -> issue.contains("public path must use"))
                .anyMatch(issue -> issue.contains("security or x-public-access-approved"))
                .anyMatch(issue -> issue.contains("X-Correlation-ID is required"))
                .anyMatch(issue -> issue.contains("application/problem+json"))
                .anyMatch(issue -> issue.contains("required convention parameter missing page"))
                .anyMatch(issue -> issue.contains("required convention parameter missing Idempotency-Key"))
                .anyMatch(issue -> issue.contains("idempotent operation requires a 409 response"));
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
