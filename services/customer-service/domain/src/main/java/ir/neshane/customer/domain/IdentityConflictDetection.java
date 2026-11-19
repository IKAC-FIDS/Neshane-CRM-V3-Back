package ir.neshane.customer.domain;

import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Isolated comparison of explicitly resolved namespace evidence, not legal verification. */
public final class IdentityConflictDetection {
    public enum Verification { VERIFIED_VALID, UNVERIFIED, UNKNOWN }
    public enum Outcome { NO_MATCH, POTENTIAL_DUPLICATE, DEFINITE_CONFLICT, BLOCKED }
    /** Namespace/normalized value are externally supplied synthetic evidence, bound to the exact identifier. */
    public record Evidence(IdentityIdentifier identifier, Optional<String> namespace,
                           Optional<String> comparisonValue, Verification verification) {
        public Evidence {
            Objects.requireNonNull(identifier);
            Objects.requireNonNull(namespace);
            Objects.requireNonNull(comparisonValue);
            Objects.requireNonNull(verification);
            namespace.ifPresent(v -> IdentifierText.requireNonBlank(v, "IDENTITY_NAMESPACE_REQUIRED"));
            comparisonValue.ifPresent(v -> IdentifierText.requireNonBlank(v, "IDENTITY_COMPARISON_VALUE_REQUIRED"));
        }
        @Override public String toString() { return "IdentityConflictEvidence[redacted]"; }
    }
    public record Assessment(Outcome outcome, String code) {
        public Optional<DuplicateDetection.Signal> signal() {
            return switch (outcome) {
                case DEFINITE_CONFLICT -> Optional.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER);
                case POTENTIAL_DUPLICATE -> Optional.of(DuplicateDetection.Signal.UNVERIFIED_IDENTIFIER);
                default -> Optional.empty();
            };
        }
    }
    private IdentityConflictDetection() {}
    public static Assessment compare(IdentityInfo candidate, Evidence proposed, IdentityInfo existing,
                                     Evidence observed, IdentifierTypeCatalog catalog, LocalDate day) {
        Objects.requireNonNull(day);
        if (!candidate.owner().tenantId().equals(existing.owner().tenantId()))
            return blocked("IDENTITY_CONFLICT_TENANT_MISMATCH");
        if (candidate.owner().customerId().equals(existing.owner().customerId()))
            return blocked("IDENTITY_CONFLICT_SELF_COMPARISON");
        if (!candidate.identifiers().contains(proposed.identifier()) || !existing.identifiers().contains(observed.identifier()))
            return blocked("IDENTITY_CONFLICT_EVIDENCE_MISMATCH");
        try {
            if (!applicable(catalog, proposed.identifier()) || !applicable(catalog, observed.identifier()))
                return blocked("IDENTITY_CONFLICT_TYPE_NOT_APPLICABLE");
        } catch (IllegalArgumentException ex) {
            return blocked(ex.getMessage());
        }
        if (proposed.namespace().isEmpty() || observed.namespace().isEmpty()
                || proposed.comparisonValue().isEmpty() || observed.comparisonValue().isEmpty())
            return blocked("IDENTITY_NAMESPACE_OR_VALUE_UNRESOLVED");
        if (!proposed.namespace().equals(observed.namespace()) || !proposed.comparisonValue().equals(observed.comparisonValue()))
            return new Assessment(Outcome.NO_MATCH, "IDENTITY_NO_MATCH_CANDIDATE");
        // Externally resolved namespace may span types; never infer jurisdiction from labels.
        if (proposed.verification() == Verification.UNKNOWN || observed.verification() == Verification.UNKNOWN)
            return blocked("IDENTITY_VERIFICATION_UNKNOWN");
        if (ambiguousExpiry(proposed, day) || ambiguousExpiry(observed, day))
            return blocked("IDENTITY_EXPIRY_BOUNDARY_UNRESOLVED");
        boolean definite = verifiedValid(observed, day) || verifiedValid(proposed, day);
        return definite ? new Assessment(Outcome.DEFINITE_CONFLICT, "IDENTITY_DEFINITE_CONFLICT")
            : new Assessment(Outcome.POTENTIAL_DUPLICATE, "IDENTITY_POTENTIAL_DUPLICATE");
    }
    private static boolean applicable(IdentifierTypeCatalog catalog, IdentityIdentifier identifier) {
        var type = catalog.resolveReference(identifier.typeId());
        return type.applicableTo().contains(identifier.owner().kind())
            && (type.applicableCountries().isEmpty() || identifier.issuingCountry().filter(type.applicableCountries()::contains).isPresent())
            && (type.hasExpiryDate() || identifier.expiryDate().isEmpty());
    }
    private static boolean verifiedValid(Evidence evidence, LocalDate day) {
        var id = evidence.identifier();
        return evidence.verification() == Verification.VERIFIED_VALID
            && id.status() == IdentityIdentifier.Status.ACTIVE
            && id.issueDate().map(d -> !day.isBefore(d)).orElse(true)
            && id.expiryDate().map(d -> day.isBefore(d)).orElse(true);
    }
    private static boolean ambiguousExpiry(Evidence evidence, LocalDate day) {
        return evidence.verification() == Verification.VERIFIED_VALID
            && evidence.identifier().status() == IdentityIdentifier.Status.ACTIVE
            && evidence.identifier().expiryDate().filter(day::equals).isPresent();
    }
    private static Assessment blocked(String code) { return new Assessment(Outcome.BLOCKED, code); }
}
