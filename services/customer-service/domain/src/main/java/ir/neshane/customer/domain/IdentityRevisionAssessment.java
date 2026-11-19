package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.util.Objects;
import java.util.Optional;

/** Synthetic assessment of actual candidate differences and externally resolved evidence impact. */
public final class IdentityRevisionAssessment {
    public enum Authority { NONE, UPDATE }
    public enum Impact { MATERIAL, NOT_MATERIAL, UNKNOWN }
    public enum IdentifierCorrection { VERIFIED_IDENTIFIER_CORRECTED, NO_VERIFIED_IDENTIFIER_CORRECTION, UNKNOWN }
    public enum Outcome { ELIGIBLE_CANDIDATE, UNCHANGED, BLOCKED }
    public record Assessment(Outcome outcome, String code, Optional<IdentityRevision> proposed,
            boolean criticalChange, boolean auditRequired, boolean differentVerifierRequired) {}
    private IdentityRevisionAssessment() {}

    public static Assessment assess(IdentityRevision current, String expectedRevision, Customer proposed,
            String nextRevision, Authority authority, Impact impact, IdentifierCorrection correction, String reason) {
        Objects.requireNonNull(current);
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(authority);
        Objects.requireNonNull(impact);
        Objects.requireNonNull(correction);
        IdentifierText.requireNonBlank(expectedRevision,"IDENTITY_REVISION_REQUIRED");
        IdentifierText.requireNonBlank(nextRevision,"IDENTITY_REVISION_REQUIRED");
        if (!current.owner().equals(proposed.identityInfo().owner())) return blocked("IDENTITY_REVISION_SCOPE_MISMATCH",false,false);
        if (!current.value().equals(expectedRevision)) return blocked("IDENTITY_REVISION_STALE",false,false);
        var differences = current.differences(proposed);
        boolean identifiers = differences.identifiers();
        boolean critical = differences.critical();
        boolean changed = differences.changed();
        if (authority != Authority.UPDATE) return blocked("IDENTITY_UPDATE_AUTHORITY_REQUIRED",critical,true);
        if (!changed) return new Assessment(Outcome.UNCHANGED,"IDENTITY_REVISION_UNCHANGED",Optional.of(current),false,false,false);
        if (critical && (reason == null || reason.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp))))
            return blocked("IDENTITY_CHANGE_REASON_REQUIRED",true,false);
        if (impact == Impact.UNKNOWN) return blocked("IDENTITY_EVIDENCE_IMPACT_UNRESOLVED",critical,false);
        if (identifiers && correction == IdentifierCorrection.UNKNOWN)
            return blocked("IDENTITY_CORRECTION_SCOPE_UNRESOLVED",critical,false);
        if (!identifiers && correction == IdentifierCorrection.VERIFIED_IDENTIFIER_CORRECTED)
            return blocked("IDENTITY_CORRECTION_SCOPE_MISMATCH",critical,false);
        boolean sensitive = identifiers && correction == IdentifierCorrection.VERIFIED_IDENTIFIER_CORRECTED;
        if (sensitive && impact != Impact.MATERIAL)
            return blocked("IDENTITY_CORRECTION_IMPACT_MISMATCH",critical,false);
        boolean rotate = critical || impact == Impact.MATERIAL;
        if (rotate == current.value().equals(nextRevision)) return blocked("IDENTITY_REVISION_ROTATION_MISMATCH",critical,false);
        var status = current.verificationStatus();
        if (impact == Impact.MATERIAL && status == VerificationStatus.VERIFIED)
            status = VerificationStatus.REQUIRES_REVERIFICATION;
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"IDENTITY_REVISION_CANDIDATE",
            Optional.of(IdentityRevision.snapshot(proposed,nextRevision,status)),critical,true,sensitive);
    }
    private static Assessment blocked(String code,boolean critical,boolean audit) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty(),critical,audit,false);
    }
}
