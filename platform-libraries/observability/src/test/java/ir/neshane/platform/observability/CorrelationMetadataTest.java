package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorrelationMetadataTest {
    @Test
    void acceptsOnlyUuidCorrelationIdentifiers() {
        assertThat(CorrelationMetadata.validate("67C34EA4-810C-4A33-8F97-1A2B47026891"))
                .contains("67c34ea4-810c-4a33-8f97-1a2b47026891");
        assertThat(CorrelationMetadata.validate("tenant-42/customer-7")).isEmpty();
    }

    @Test
    void replacesUntrustedCorrelationInput() {
        assertThat(CorrelationMetadata.normalizeOrGenerate("Bearer sensitive-value"))
                .matches("[0-9a-f-]{36}");
    }
}
