package ir.neshane.reference.configuration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

final class SystemSecretMaterialSource implements SecretMaterialSource {
    private static final long MAXIMUM_SECRET_FILE_BYTES = 65_536;

    @Override
    public Optional<char[]> readEnvironment(String variableName) {
        return Optional.ofNullable(System.getenv(variableName)).map(String::toCharArray);
    }

    @Override
    public char[] readFile(Path path) throws IOException {
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(path)) {
            throw new IOException("secret file unavailable");
        }
        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (before.size() > MAXIMUM_SECRET_FILE_BYTES) {
            throw new IOException("secret file exceeds bounded input size");
        }
        byte[] bytes = Files.readAllBytes(path);
        try {
            if (bytes.length > MAXIMUM_SECRET_FILE_BYTES) {
                throw new IOException("secret file exceeds bounded input size");
            }
            BasicFileAttributes after = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (after.size() != before.size()
                    || !Objects.equals(after.fileKey(), before.fileKey())
                    || Files.isSymbolicLink(path)) {
                throw new IOException("secret file changed during read");
            }
            CharBuffer decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            char[] characters = new char[decoded.remaining()];
            decoded.get(characters);
            return characters;
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
