package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RoutingContractValidationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void proposedInventoryAndSchemasAreValid() {
        RoutingContractVerifier.validateCandidate(contractRoot());
    }

    @Test
    void unknownContractVersionFailsClosed() {
        assertThatThrownBy(() -> new RoutingContractCatalog(contractRoot(), new JsonSupport())
                        .find("cell-registry", "2.0.0"))
                .isInstanceOf(RoutingContractException.class)
                .hasMessageContaining("Unknown routing contract");
    }

    @Test
    void malformedCellRegistryDocumentIsRejected() {
        ObjectNode candidate = validCellRegistry();
        candidate.remove("authority");

        assertThat(RoutingContractVerifier.validateInstance(
                        contractRoot(), "cell-registry", "1.0.0", candidate))
                .isNotEmpty();
    }

    @Test
    void invalidLifecycleStateIsRejectedStructurally() {
        ObjectNode candidate = validCellRegistry();
        candidate.put("lifecycleState", "DELETED");

        assertThat(RoutingContractVerifier.validateInstance(
                        contractRoot(), "cell-registry", "1.0.0", candidate))
                .isNotEmpty();
    }

    @Test
    void validCellRegistryDocumentPasses() {
        assertThat(RoutingContractVerifier.validateInstance(
                        contractRoot(), "cell-registry", "1.0.0", validCellRegistry()))
                .isEmpty();
    }

    private ObjectNode validCellRegistry() {
        ObjectNode candidate = mapper.createObjectNode();
        candidate.put("contractVersion", "1.0.0");
        candidate.put("cellId", "cell-a");
        candidate.put("deploymentId", PolicyFixtures.DEPLOYMENT);
        candidate.put("deploymentProfile", "SHARED_SAAS");
        candidate.put("registryVersion", 1);
        candidate.put("lifecycleState", "REGISTERED");
        candidate.set("authority", mapper.createObjectNode()
                .put("authorityKind", "PROVIDER_CELL_REGISTRY_AUTHORITY")
                .put("authorityRef", "provider-cell-registry")
                .put("provenanceVersion", 1));
        return candidate;
    }

    private Path contractRoot() {
        return Path.of(System.getProperty("routingContractRoot"));
    }
}
