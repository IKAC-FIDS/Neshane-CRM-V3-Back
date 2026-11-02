package ir.neshane.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ReferenceServiceStartupFailureTest {

    @TempDir Path temporaryDirectory;

    @Test
    void actualProductionStartupFailsWithoutLeakingInvalidSecret(CapturedOutput output)
            throws IOException {
        String marker = "synthetic-startup-secret-marker";
        Path secretFile = temporaryDirectory.resolve("sensitive-reference.secret");
        Files.writeString(secretFile, marker + '\0');
        SpringApplication application = new SpringApplication(ReferenceServiceApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        assertThatThrownBy(() -> application.run(
                        "--spring.profiles.active=production",
                        "--neshane.reference.security.required-secret.source=FILE",
                        "--neshane.reference.security.required-secret.file=" + secretFile))
                .hasRootCauseMessage(
                        "Configuration validation failed [invalid-secret-material] for "
                                + "neshane.reference.security.required-secret");

        assertThat(output.getAll())
                .doesNotContain(marker)
                .doesNotContain(secretFile.toString());
    }
}
