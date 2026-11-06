
package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SharedCellRegistryFixtureTest {

    private static final List<String> CELL_IDS =
        List.of("cell-a", "cell-b");

    private final ObjectMapper mapper = new ObjectMapper();

    private Path fixtureRoot() {
        return Path.of(System.getProperty("routingContractRoot"))
            .toAbsolutePath()
            .getParent()
            .getParent()
            .resolve("deployment/shared/fixtures/v1");
    }

    private JsonNode fixture(String cellId) throws Exception {
        return mapper.readTree(
            fixtureRoot().resolve(cellId + ".json").toFile());
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
    void bothFixturesMatchRoutingContract() throws Exception {
        for (String cellId : CELL_IDS) {
            JsonNode node = fixture(cellId);

            assertThat(RoutingContractVerifier.validateInstance(
                Path.of(System.getProperty("routingContractRoot")),
                "cell-registry",
                "1.0.0",
                node)).as(cellId).isEmpty();
        }
    }

    @Test
    void bothCellsStartRegisteredAndCanActivate() throws Exception {
        CellRegistryPolicy policy = new CellRegistryPolicy();

        for (String cellId : CELL_IDS) {
            JsonNode node = fixture(cellId);

            assertThat(node.path("cellId").asText())
                .isEqualTo(cellId);
            assertThat(node.path("deploymentId").asText())
                .isEqualTo("shared-deployment");
            assertThat(node.path("deploymentProfile").asText())
                .isEqualTo("SHARED_SAAS");
            assertThat(node.path("registryVersion").asLong())
                .isEqualTo(1);
            assertThat(node.path("lifecycleState").asText())
                .isEqualTo("REGISTERED");

            CellRegistryPolicy.CellRecord initial = record(node);
            assertThat(policy.apply(initial)).isEqualTo(initial);

            ObjectNode activeNode = (ObjectNode) node.deepCopy();
            activeNode.put("registryVersion", 2);
            activeNode.put("lifecycleState", "ACTIVE");

            CellRegistryPolicy.CellRecord active =
                record(activeNode);
            assertThat(policy.apply(active)).isEqualTo(active);
        }
    }

    @Test
    void directActiveRegistrationIsRejectedForBothCells()
        throws Exception {
        for (String cellId : CELL_IDS) {
            ObjectNode node = (ObjectNode) fixture(cellId).deepCopy();
            node.put("lifecycleState", "ACTIVE");

            CellRegistryPolicy policy = new CellRegistryPolicy();

            assertThatThrownBy(() -> policy.apply(record(node)))
                .isInstanceOf(RoutingContractException.class)
                .hasMessageContaining("start REGISTERED");
        }
    }

    @Test
    void globalDirectoryMatchesBothRegisteredCells()
        throws Exception {
        JsonNode directory = mapper.readTree(
            fixtureRoot().resolve("global-directory.json").toFile());

        assertThat(directory.path("referenceOnly").asBoolean()).isTrue();
        assertThat(directory.path("directoryVersion").asInt())
            .isEqualTo(1);
        assertThat(directory.path("deploymentId").asText())
            .isEqualTo("shared-deployment");
        assertThat(directory.path("deploymentProfile").asText())
            .isEqualTo("SHARED_SAAS");

        assertThat(directory.path("cells").size()).isEqualTo(2);
        assertThat(directory.path("tenants").size()).isEqualTo(2);

        for (String cellId : CELL_IDS) {
            assertThat(directory.path("cells").findValuesAsText("cellId"))
                .contains(cellId);
            assertThat(directory.path("tenants").findValuesAsText("cellId"))
                .contains(cellId);
        }

        assertThat(directory.path("tenants")
            .findValuesAsText("tenantId"))
            .containsExactlyInAnyOrder(
                "550e8400-e29b-41d4-a716-446655440000",
                "550e8400-e29b-41d4-a716-446655440001");
    }
}
