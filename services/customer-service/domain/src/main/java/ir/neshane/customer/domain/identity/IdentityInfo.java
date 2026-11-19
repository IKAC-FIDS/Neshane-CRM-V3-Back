package ir.neshane.customer.domain.identity;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable collection owned by Person or Company. */
public record IdentityInfo(IdentityOwner owner, List<IdentityIdentifier> identifiers) {
    public IdentityInfo {
        Objects.requireNonNull(owner, "IDENTITY_OWNER_REQUIRED");
        identifiers = List.copyOf(Objects.requireNonNull(identifiers, "IDENTIFIERS_REQUIRED"));
        Set<String> ids = new HashSet<>();
        for (IdentityIdentifier identifier : identifiers) {
            if (!owner.equals(identifier.owner())) {
                throw new IllegalArgumentException("IDENTIFIER_OWNER_MISMATCH");
            }
            if (!ids.add(identifier.id())) {
                throw new IllegalArgumentException("IDENTIFIER_ID_DUPLICATE");
            }
        }
    }

    public static IdentityInfo empty(IdentityOwner owner) {
        return new IdentityInfo(owner, List.of());
    }

    IdentityInfo withCandidateIdentifier(IdentityIdentifier identifier) {
        List<IdentityIdentifier> next = new ArrayList<>(identifiers);
        next.add(Objects.requireNonNull(identifier, "IDENTIFIER_REQUIRED"));
        return new IdentityInfo(owner, next);
    }

    /** Checked owner binding; never changes or reassigns ownership. */
    public static IdentityInfo forOwner(IdentityOwner expected, IdentityInfo supplied) {
        if (supplied == null) {
            return empty(expected);
        }
        if (!expected.equals(supplied.owner())) {
            throw new IllegalArgumentException("IDENTITY_INFO_OWNER_MISMATCH");
        }
        return supplied;
    }

    @Override
    public String toString() { return "IdentityInfo[redacted]"; }
}
