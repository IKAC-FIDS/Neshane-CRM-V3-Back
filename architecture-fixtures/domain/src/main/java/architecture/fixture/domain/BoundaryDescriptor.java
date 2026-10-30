package architecture.fixture.domain;

import java.util.Objects;

public record BoundaryDescriptor(String name) {

    public BoundaryDescriptor {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
