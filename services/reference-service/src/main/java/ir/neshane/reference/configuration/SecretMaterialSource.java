package ir.neshane.reference.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

interface SecretMaterialSource {
    Optional<char[]> readEnvironment(String variableName);

    char[] readFile(Path path) throws IOException;
}
