package ir.neshane.reference.configuration;

import ir.neshane.reference.configuration.ReferenceSecurityProperties.SecretReference;
import ir.neshane.reference.configuration.ReferenceSecurityProperties.SecretSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

final class ExternalSecretContract {
    private static final String CONFIGURATION_KEY = "neshane.reference.security.required-secret";
    private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final SecretMaterialSource materialSource;

    ExternalSecretContract(SecretMaterialSource materialSource) {
        this.materialSource = materialSource;
    }

    void validate(SecretReference reference, boolean required) {
        if (reference.source() == null) {
            if (required) {
                throw failure("missing-reference");
            }
            rejectDanglingMetadata(reference);
            return;
        }

        char[] material = switch (reference.source()) {
            case ENVIRONMENT -> resolveEnvironment(reference);
            case FILE -> resolveFile(reference);
        };
        try {
            validateMaterial(material);
        } finally {
            Arrays.fill(material, '\0');
        }
    }

    private char[] resolveEnvironment(SecretReference reference) {
        if (reference.file() != null || !hasText(reference.environmentVariable())) {
            throw failure("invalid-environment-reference");
        }
        String variableName = reference.environmentVariable().trim();
        if (!ENVIRONMENT_VARIABLE.matcher(variableName).matches()) {
            throw failure("invalid-environment-reference");
        }
        return materialSource.readEnvironment(variableName)
                .orElseThrow(() -> failure("missing-environment-secret"));
    }

    private char[] resolveFile(SecretReference reference) {
        if (hasText(reference.environmentVariable()) || reference.file() == null) {
            throw failure("invalid-file-reference");
        }
        try {
            return materialSource.readFile(reference.file());
        } catch (IOException exception) {
            throw failure("unavailable-secret-file");
        }
    }

    private static void validateMaterial(char[] material) {
        boolean hasVisibleContent = false;
        for (char character : material) {
            if (character == '\0') {
                throw failure("invalid-secret-material");
            }
            hasVisibleContent |= !Character.isWhitespace(character);
        }
        if (!hasVisibleContent) {
            throw failure("blank-secret-material");
        }
    }

    private static void rejectDanglingMetadata(SecretReference reference) {
        if (hasText(reference.environmentVariable()) || reference.file() != null) {
            throw failure("missing-secret-source");
        }
    }

    private static boolean hasText(String candidate) {
        return candidate != null && !candidate.isBlank();
    }

    private static ReferenceConfigurationException failure(String category) {
        return new ReferenceConfigurationException(category, CONFIGURATION_KEY);
    }
}
