package ir.neshane.contract.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContractValidationTest {

    @Test
    void committedHttpContractsSatisfyTheConventions() {
        Path contractRoot = Path.of(System.getProperty("contractRoot"));

        HttpContractVerifier.validate(contractRoot);
    }

    @Test
    void validReferenceFixtureHasNoConventionViolations() throws Exception {
        var api = OpenApiLoader.load(resource("valid-baseline.openapi.yaml"));

        assertThat(new HttpContractRules().validate(api)).isEmpty();
    }

    private Path resource(String name) throws Exception {
        return Path.of(getClass().getResource("/" + name).toURI());
    }
}
