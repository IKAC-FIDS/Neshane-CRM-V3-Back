package ir.neshane.contract.routing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReferenceCellIdentityValidatorTest {

    @TempDir
    Path tempDir;

    private Path contracts() {
        return Path.of(System.getProperty("routingContractRoot"));
    }

    private Path fixtures() {
        return contracts().toAbsolutePath().getParent().getParent()
                .resolve("deployment/shared/fixtures/v1");
    }

    private ReferenceCellIdentityValidator validator(
            Path directory, Path registry, String identity) {
        return new ReferenceCellIdentityValidator(
                directory, registry, contracts(), identity);
    }

    @Test
    void bothReferenceCellsPassStartupAndRuntimeChecks() {
        for (String cellId : new String[] {"cell-a", "cell-b"}) {
            ReferenceCellIdentityValidator cell = validator(
                    fixtures().resolve("global-directory.json"),
                    fixtures().resolve(cellId + ".json"),
                    cellId);

            assertThatCode(() -> cell.requireMatchingCell(cellId))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void wrongCellIdentityFailsAtStartup() {
        assertThatThrownBy(() -> validator(
                fixtures().resolve("global-directory.json"),
                fixtures().resolve("cell-a.json"),
                "cell-b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity mismatch");
    }

    @Test
    void unknownCellFailsAtStartup() {
        assertThatThrownBy(() -> validator(
                fixtures().resolve("global-directory.json"),
                fixtures().resolve("cell-a.json"),
                "cell-unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown reference Cell");
    }

    @Test
    void runtimeDestinationMismatchFailsClosed() {
        ReferenceCellIdentityValidator cell = validator(
                fixtures().resolve("global-directory.json"),
                fixtures().resolve("cell-a.json"),
                "cell-a");

        assertThatThrownBy(() -> cell.requireMatchingCell("cell-b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Runtime Cell identity mismatch");
    }

    @Test
    void modifiedRegistryFailsClosedAtRuntime() throws Exception {
        Path registry = tempDir.resolve("cell-a.json");
        Files.copy(fixtures().resolve("cell-a.json"), registry);

        ReferenceCellIdentityValidator cell = validator(
                fixtures().resolve("global-directory.json"),
                registry, "cell-a");

        Files.writeString(registry,
                Files.readString(registry).replace(
                        "\"registryVersion\"", "\"modifiedRegistryVersion\""));

        assertThatThrownBy(() -> cell.requireMatchingCell("cell-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed after startup");
    }

    @Test
    void directoryUnavailableAtRuntimeFailsClosed() throws Exception {
        Path directory = tempDir.resolve("global-directory.json");
        Files.copy(fixtures().resolve("global-directory.json"), directory);

        ReferenceCellIdentityValidator cell = validator(
                directory,
                fixtures().resolve("cell-a.json"),
                "cell-a");

        Files.delete(directory);

        assertThatThrownBy(() -> cell.requireMatchingCell("cell-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unavailable");
    }
}
