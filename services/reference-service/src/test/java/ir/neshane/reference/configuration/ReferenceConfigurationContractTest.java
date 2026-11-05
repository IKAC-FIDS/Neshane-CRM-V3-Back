package ir.neshane.reference.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.neshane.reference.configuration.ReferenceSecurityProperties.SecretReference;
import ir.neshane.reference.configuration.ReferenceSecurityProperties.SecretSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;

class ReferenceConfigurationContractTest {
    private static final String SECRET_KEY = "neshane.reference.security.required-secret";
    private final ApplicationContextRunner startupRunner = new ApplicationContextRunner()
            .withUserConfiguration(StartupConfiguration.class)
            .withPropertyValues(
                    "neshane.reference.service-name=reference-service",
                    "neshane.reference.diagnostics.enabled=false");

    @TempDir Path temporaryDirectory;

    @Test
    void productionSpringContextFailsClosedWithoutRequiredSecret() {
        startupRunner
                .withPropertyValues("spring.profiles.active=production")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(ReferenceConfigurationException.class)
                            .rootCause()
                            .hasMessageContaining("missing-reference")
                            .hasMessageContaining(SECRET_KEY);
                });
    }

    @Test
    void developmentSpringContextStartsWithoutOptionalIntegrationSecret() {
        startupRunner
                .withPropertyValues("spring.profiles.active=development")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void acceptsDevelopmentWithoutASecretReference() throws Exception {
        validator("development", properties(null, null, null), source(Optional.empty(), null))
                .afterPropertiesSet();
    }

    @Test
    void acceptsProductionWithAnExternalEnvironmentSecretAndClearsTheBuffer() throws Exception {
        char[] material = "synthetic-secret-value".toCharArray();

        validator(
                        "production",
                        properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                        source(Optional.of(material), null))
                .afterPropertiesSet();

        assertThat(material).containsOnly('\0');
    }

    @Test
    void rejectsProductionWithoutASecretReference() {
        assertThatThrownBy(() -> validator(
                                "production",
                                properties(null, null, null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .isInstanceOf(ReferenceConfigurationException.class)
                .hasMessageContaining("missing-reference")
                .hasMessageContaining(SECRET_KEY);
    }

    @Test
    void rejectsMissingDeploymentProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> validator(
                                environment,
                                properties(null, null, null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("unsupported-deployment-profile")
                .hasMessageContaining("active-count=0");
    }

    @Test
    void rejectsBlankSecretWithoutDisclosingItsValue() {
        String candidate = "   ";

        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                                source(Optional.of(candidate.toCharArray()), null))
                        .afterPropertiesSet())
                .isInstanceOf(ReferenceConfigurationException.class)
                .hasMessageContaining("blank-secret-material")
                .hasMessageNotContaining(candidate);
    }

    @Test
    void rejectsEmptySecretAndMissingEnvironmentVariable() {
        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                                source(Optional.of(new char[0]), null))
                        .afterPropertiesSet())
                .hasMessageContaining("blank-secret-material");

        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("missing-environment-secret");
    }

    @Test
    void rejectsInvalidEnvironmentReference() {
        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.ENVIRONMENT, "invalid-name", null),
                                source(Optional.of("synthetic".toCharArray()), null))
                        .afterPropertiesSet())
                .isInstanceOf(ReferenceConfigurationException.class)
                .hasMessageContaining("invalid-environment-reference")
                .hasMessageNotContaining("invalid-name");
    }

    @Test
    void rejectsMissingOrUnreadableSecretFileWithoutDisclosingItsPath() {
        Path candidate = Path.of("sensitive", "location", "secret.txt");

        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.FILE, null, candidate),
                                source(Optional.empty(), new IOException("operating-system detail")))
                        .afterPropertiesSet())
                .isInstanceOf(ReferenceConfigurationException.class)
                .hasMessageContaining("unavailable-secret-file")
                .hasMessageNotContaining(candidate.toString())
                .hasMessageNotContaining("operating-system detail");
    }

    @Test
    void systemFileSourceRejectsOversizedAndInvalidUtf8Files() throws IOException {
        Path oversized = temporaryDirectory.resolve("oversized.secret");
        Files.write(oversized, new byte[65_537]);
        Path invalidUtf8 = temporaryDirectory.resolve("invalid-utf8.secret");
        Files.write(invalidUtf8, new byte[] {(byte) 0xC3, 0x28});
        SystemSecretMaterialSource source = new SystemSecretMaterialSource();

        assertThatThrownBy(() -> source.readFile(oversized))
                .isInstanceOf(IOException.class)
                .hasMessageNotContaining(oversized.toString());
        assertThatThrownBy(() -> source.readFile(invalidUtf8))
                .isInstanceOf(IOException.class)
                .hasMessageNotContaining(invalidUtf8.toString());
    }

    @Test
    void rejectsConflictingCustomAndSpringDeploymentProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("NESHANE_DEPLOYMENT_PROFILE", "production");
        environment.setActiveProfiles("development");

        assertThatThrownBy(() -> validator(
                                environment,
                                properties(null, null, null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("conflicting-deployment-profile")
                .hasMessageNotContaining("production")
                .hasMessageNotContaining("development");
    }

    @Test
    void rejectsInvalidOtlpEndpointWithoutEchoingCandidate() {
        String candidate = "http://user:synthetic-secret@[invalid";

        assertThatThrownBy(() -> ReferenceObservabilityConfiguration.safeUri(candidate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neshane.observability.otlp-endpoint")
                .hasMessageNotContaining(candidate)
                .hasNoCause();
    }

    @Test
    void springBindingRejectsUnknownSecretSource() {
        startupRunner
                .withPropertyValues(
                        "spring.profiles.active=production",
                        "neshane.reference.security.required-secret.source=UNKNOWN")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("neshane.reference.security");
                });
    }

    @Test
    void rejectsSecretMaterialContainingNullCharactersWithoutLeakingIt() {
        String candidate = "synthetic\0secret";

        assertThatThrownBy(() -> validator(
                                "production",
                                properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                                source(Optional.of(candidate.toCharArray()), null))
                        .afterPropertiesSet())
                .isInstanceOf(ReferenceConfigurationException.class)
                .hasMessageContaining("invalid-secret-material")
                .hasMessageNotContaining(candidate);
    }

    @Test
    void rejectsUnsupportedAndMultipleDeploymentProfiles() {
        assertThatThrownBy(() -> validator(
                                "staging",
                                properties(null, null, null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("unsupported-deployment-profile")
                .hasMessageContaining("active-count=1");

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("development", "test");
        assertThatThrownBy(() -> validator(
                                environment,
                                properties(null, null, null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("active-count=2");
    }

    @Test
    void rejectsDevelopmentMetadataWithoutAnExplicitSource() {
        assertThatThrownBy(() -> validator(
                                "development",
                                properties(null, "REFERENCE_SERVICE_SECRET", null),
                                source(Optional.empty(), null))
                        .afterPropertiesSet())
                .hasMessageContaining("missing-secret-source");
    }

    @Test
    void rejectsProductionDiagnosticsAsAnIncompatibleCombination() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        ReferenceServiceProperties serviceProperties =
                new ReferenceServiceProperties("reference-service", new ReferenceServiceProperties.Diagnostics(true));

        assertThatThrownBy(() -> new ReferenceStartupValidator(
                                environment,
                                serviceProperties,
                                properties(SecretSource.ENVIRONMENT, "REFERENCE_SERVICE_SECRET", null),
                                new ExternalSecretContract(source(
                                        Optional.of("synthetic-secret".toCharArray()), null)))
                        .afterPropertiesSet())
                .hasMessageContaining("incompatible-production-diagnostics");
    }

    private static ReferenceStartupValidator validator(
            String profile,
            ReferenceSecurityProperties properties,
            SecretMaterialSource materialSource) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return validator(environment, properties, materialSource);
    }

    private static ReferenceStartupValidator validator(
            MockEnvironment environment,
            ReferenceSecurityProperties properties,
            SecretMaterialSource materialSource) {
        return new ReferenceStartupValidator(
                environment,
                new ReferenceServiceProperties(
                        "reference-service", new ReferenceServiceProperties.Diagnostics(false)),
                properties,
                new ExternalSecretContract(materialSource));
    }

    private static ReferenceSecurityProperties properties(
            SecretSource source, String environmentVariable, Path file) {
        return new ReferenceSecurityProperties(new SecretReference(source, environmentVariable, file));
    }

    private static SecretMaterialSource source(Optional<char[]> environmentValue, IOException fileFailure) {
        return new SecretMaterialSource() {
            @Override
            public Optional<char[]> readEnvironment(String variableName) {
                return environmentValue;
            }

            @Override
            public char[] readFile(Path path) throws IOException {
                if (fileFailure != null) {
                    throw fileFailure;
                }
                return "synthetic-file-secret".toCharArray();
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ReferenceServiceProperties.class, ReferenceSecurityProperties.class})
    @Import(ReferenceSecurityConfiguration.class)
    static class StartupConfiguration {}
}
