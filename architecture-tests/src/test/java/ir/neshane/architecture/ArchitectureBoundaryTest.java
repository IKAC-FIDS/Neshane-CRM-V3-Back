package ir.neshane.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import ir.neshane.alpha.application.AllowedContractUse;
import ir.neshane.alpha.domain.AlphaUsesBetaInternal;
import ir.neshane.beta.internal.BetaInternalType;
import ir.neshane.contract.customer.CustomerContract;
import java.util.List;
import org.junit.jupiter.api.Test;
import v3_015.fixture.domain.CleanDomain;
import v3_015.fixture.domain.DomainLeaksToInfrastructure;
import v3_015.fixture.domain.FrameworkCoupledDomain;
import v3_015.fixture.infrastructure.FakeInfrastructure;

class ArchitectureBoundaryTest {

    private final ClassFileImporter productionImporter =
            new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests());
    private final ClassFileImporter fixtureImporter = new ClassFileImporter();

    @Test
    void existingArchitecturePassesAllRulesAndContainsProtectedScopes() {
        JavaClasses classes = productionImporter.importPackages(
                "ir.neshane.reference", "ir.neshane.referencepeer", "ir.neshane.platform.observability");

        ArchitectureScope.requirePackages(
                classes,
                List.of(
                        "ir.neshane.reference",
                        "ir.neshane.referencepeer",
                        "ir.neshane.platform.observability"));
        ArchitecturePolicy.domainIsolation().check(classes);
        ArchitecturePolicy.applicationDependencyDirection().check(classes);
        ArchitecturePolicy.serviceAndPlatformIsolation().check(classes);
    }

    @Test
    void fixtureADomainToInfrastructureIsRejectedForTheIntendedDependency() {
        JavaClasses classes =
                fixtureImporter.importClasses(DomainLeaksToInfrastructure.class, FakeInfrastructure.class);

        var result = ArchitecturePolicy.domainIsolation().evaluate(classes);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().toString())
                .contains("DomainLeaksToInfrastructure")
                .contains("FakeInfrastructure");

        var restored = ArchitecturePolicy.domainIsolation()
                .evaluate(fixtureImporter.importClasses(CleanDomain.class, FakeInfrastructure.class));
        assertThat(restored.hasViolation()).isFalse();
    }

    @Test
    void fixtureBFrameworkCouplingIsRejectedForTheSpringDependency() {
        JavaClasses classes = fixtureImporter.importClasses(FrameworkCoupledDomain.class);

        var result = ArchitecturePolicy.domainIsolation().evaluate(classes);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().toString())
                .contains("FrameworkCoupledDomain")
                .contains("org.springframework.stereotype.Component");

        var restored = ArchitecturePolicy.domainIsolation()
                .evaluate(fixtureImporter.importClasses(CleanDomain.class));
        assertThat(restored.hasViolation()).isFalse();
    }

    @Test
    void fixtureDCrossServiceInternalImportIsRejected() {
        JavaClasses classes = fixtureImporter.importClasses(AlphaUsesBetaInternal.class, BetaInternalType.class);

        var result = ArchitecturePolicy.serviceAndPlatformIsolation().evaluate(classes);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().toString())
                .contains("AlphaUsesBetaInternal")
                .contains("BetaInternalType");

        var restored = ArchitecturePolicy.serviceAndPlatformIsolation()
                .evaluate(fixtureImporter.importClasses(AllowedContractUse.class, CustomerContract.class));
        assertThat(restored.hasViolation()).isFalse();
    }

    @Test
    void fixtureFApprovedContractDependencyPasses() {
        JavaClasses classes = fixtureImporter.importClasses(AllowedContractUse.class, CustomerContract.class);

        var result = ArchitecturePolicy.serviceAndPlatformIsolation().evaluate(classes);

        assertThat(result.hasViolation()).isFalse();
    }

    @Test
    void emptyOrIncompleteProtectedScopeFailsClosed() {
        JavaClasses contractOnly = fixtureImporter.importClasses(CustomerContract.class);

        assertThatThrownBy(() -> ArchitectureScope.requirePackages(
                        contractOnly,
                        List.of(
                                "ir.neshane.reference",
                                "ir.neshane.referencepeer",
                                "ir.neshane.platform.observability")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("protected package is missing");
    }
}
