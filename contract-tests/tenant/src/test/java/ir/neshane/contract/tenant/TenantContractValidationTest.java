package ir.neshane.contract.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenantContractValidationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void proposedCandidateInventoryAndSchemasAreValid() {
        TenantContractVerifier.validateCandidate(contractRoot());
    }

    @Test
    void canonicalLowercaseUuidV4IsAccepted() {
        TenantId tenantId = new TenantId("550e8400-e29b-41d4-a716-446655440000");

        assertThat(tenantId.value()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void nonV4UppercaseAndTopologyBearingIdentifiersAreRejected() {
        assertThatThrownBy(() -> new TenantId("550e8400-e29b-11d4-a716-446655440000"))
                .isInstanceOf(TenantContractException.class);
        assertThatThrownBy(() -> new TenantId("550E8400-E29B-41D4-A716-446655440000"))
                .isInstanceOf(TenantContractException.class);
        assertThatThrownBy(() -> new TenantId("cell-a-550e8400-e29b-41d4-a716-446655440000"))
                .isInstanceOf(TenantContractException.class);
    }

    @Test
    void unknownContractVersionFailsClosed() {
        assertThatThrownBy(() -> new TenantContractCatalog(contractRoot(), new JsonSupport())
                        .find("tenant-registry", "2.0.0"))
                .isInstanceOf(TenantContractException.class)
                .hasMessageContaining("Unknown tenant contract or version");
    }

    @Test
    void malformedContractVersionFailsStructuralValidation() {
        ObjectNode registry = validRegistryCandidate();
        registry.put("contractVersion", "v1");

        assertThat(TenantContractVerifier.validateInstance(
                        contractRoot(), "tenant-registry", "1.0.0", registry))
                .isNotEmpty();
    }

    @Test
    void incompleteAuthorityMetadataFailsStructuralValidation() {
        ObjectNode registry = mapper.createObjectNode();
        registry.put("contractVersion", "1.0.0");
        registry.put("tenantId", PolicyFixtures.TENANT_ONE.value());
        registry.put("registryVersion", 1);
        registry.put("registryStatus", "ACTIVE");
        registry.set("authorityMetadata", mapper.createObjectNode()
                .put("deploymentProfile", "SHARED_SAAS")
                .put("authorityScope", "REGISTRY"));

        assertThat(TenantContractVerifier.validateInstance(
                        contractRoot(), "tenant-registry", "1.0.0", registry))
                .isNotEmpty();
    }

    @Test
    void validRegistryCandidatePassesStructuralValidation() {
        ObjectNode registry = validRegistryCandidate();

        assertThat(TenantContractVerifier.validateInstance(
                        contractRoot(), "tenant-registry", "1.0.0", registry))
                .isEmpty();
    }

    @Test
    void sovereignProfileCannotClaimManagedAuthorityInSchema() {
        ObjectNode registry = validRegistryCandidate();
        ObjectNode authority = (ObjectNode) registry.get("authorityMetadata");
        authority.put("deploymentProfile", "DEDICATED_SOVEREIGN");
        authority.put("authorityKind", "GLOBAL_MANAGED_TENANT_REGISTRY_AUTHORITY");
        assertThat(TenantContractVerifier.validateInstance(
                contractRoot(), "tenant-registry", "1.0.0", registry)).isNotEmpty();
    }

    @Test
    void sharedProfileCannotClaimSovereignAuthorityInSchema() {
        ObjectNode registry = validRegistryCandidate();
        ObjectNode authority = (ObjectNode) registry.get("authorityMetadata");
        authority.put("authorityKind", "SOVEREIGN_TENANT_REGISTRY_AUTHORITY");
        assertThat(TenantContractVerifier.validateInstance(
                contractRoot(), "tenant-registry", "1.0.0", registry)).isNotEmpty();
    }

    private ObjectNode validRegistryCandidate() {
        ObjectNode registry = mapper.createObjectNode();
        registry.put("contractVersion", "1.0.0");
        registry.put("tenantId", PolicyFixtures.TENANT_ONE.value());
        registry.put("registryVersion", 1);
        registry.put("registryStatus", "ACTIVE");
        registry.set("authorityMetadata", mapper.createObjectNode()
                .put("deploymentProfile", "SHARED_SAAS")
                .put("authorityScope", "REGISTRY")
                .put("authorityKind", "PROVIDER_TENANT_REGISTRY_AUTHORITY")
                .put("authorityRef", "provider-registry")
                .put("provenanceVersion", 1)
                .put("issuedAt", "2026-11-06T10:00:00Z"));
        return registry;
    }

    private Path contractRoot() {
        return Path.of(System.getProperty("tenantContractRoot"));
    }
}
