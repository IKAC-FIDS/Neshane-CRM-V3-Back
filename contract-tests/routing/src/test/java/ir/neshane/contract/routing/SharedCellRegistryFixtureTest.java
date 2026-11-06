package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SharedCellRegistryFixtureTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Path fixturePath() {
        return Path.of(System.getProperty("routingContractRoot"))
                .toAbsolutePath()
                .getParent()
                .getParent()
                .resolve("deployment/shared/fixtures/v1/cell-registry.json");
    }

    private JsonNode fixture() throws Exception {
        return mapper.readTree(fixturePath().toFile());
    }

    private CellRegistryPolicy.CellRecord record(JsonNode node) {
        JsonNode authority = node.path("authority");

        DeploymentProfile profile = DeploymentProfile.valueOf(
                node.path("deploymentProfile").asText());

        return new CellRegistryPolicy.CellRecord(
                new CellId(node.path("cellId").asText()),
                node.path("deploymentId").asText(),
                profile,
                node.path("registryVersion").asLong(),
                CellRegistryPolicy.LifecycleState.valueOf(
                        node.path("lifecycleState").asText()),
                new AuthorityProvenance(
                        profile,
                        node.path("deploymentId").asText(),
                        AuthorityScope.CELL_REGISTRY,
                        AuthorityKind.valueOf(
                                authority.path("authorityKind").asText()),
                        authority.path("authorityRef").asText(),
                        authority.path("provenanceVersion").asLong()));
    }

    @Test
    void fixtureMatchesRoutingContract() throws Exception {
        JsonNode node = fixture();

        assertThat(RoutingContractVerifier.validateInstance(
                Path.of(System.getProperty("routingContractRoot")),
                "cell-registry",
                "1.0.0",
                node)).isEmpty();
    }

    @Test
    void initialFixtureIsAcceptedByLifecyclePolicy() throws Exception {
        JsonNode node = fixture();

        assertThat(node.path("deploymentProfile").asText())
                .isEqualTo("SHARED_SAAS");
        assertThat(node.path("registryVersion").asLong())
                .isEqualTo(1);
        assertThat(node.path("lifecycleState").asText())
                .isEqualTo("REGISTERED");

        CellRegistryPolicy policy = new CellRegistryPolicy();
        CellRegistryPolicy.CellRecord candidate = record(node);

        assertThat(policy.apply(candidate)).isEqualTo(candidate);
    }

    @Test
    void directActiveRegistrationIsRejected() throws Exception {
        JsonNode node = fixture().deepCopy();

        ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                .put("lifecycleState", "ACTIVE");

        CellRegistryPolicy policy = new CellRegistryPolicy();

        assertThatThrownBy(() -> policy.apply(record(node)))
                .isInstanceOf(RoutingContractException.class)
                .hasMessageContaining("start REGISTERED");
    }
}
