package ir.neshane.customer.application.duplicate.identity.usecase;

import ir.neshane.customer.application.duplicate.identity.query.IdentityConflictQuery;
import ir.neshane.customer.application.duplicate.identity.query.IdentityConflictResult;
import ir.neshane.customer.application.port.in.IdentityConflictUseCase;
import ir.neshane.customer.domain.duplicate.IdentityConflictDetection;

import java.util.Objects;
import java.util.Optional;

public final class AssessIdentityConflict
        implements IdentityConflictUseCase {

    @Override
    public IdentityConflictResult execute(
            IdentityConflictQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = IdentityConflictDetection.compare(
                query.candidate(),
                query.proposed(),
                query.existing(),
                query.observed(),
                query.catalog(),
                query.day());

        var status = switch (assessment.outcome()) {
            case NO_MATCH ->
                IdentityConflictResult.Status.NO_MATCH;
            case POTENTIAL_DUPLICATE ->
                IdentityConflictResult.Status.POTENTIAL_DUPLICATE;
            case DEFINITE_CONFLICT ->
                IdentityConflictResult.Status.DEFINITE_CONFLICT;
            case BLOCKED ->
                IdentityConflictResult.Status.BLOCKED;
        };

        var signal = switch (status) {
            case NO_MATCH, BLOCKED ->
                Optional.<IdentityConflictResult.Signal>empty();
            case POTENTIAL_DUPLICATE ->
                Optional.of(
                    IdentityConflictResult.Signal.UNVERIFIED_IDENTIFIER);
            case DEFINITE_CONFLICT ->
                Optional.of(
                    IdentityConflictResult.Signal.VERIFIED_VALID_IDENTIFIER);
        };

        return new IdentityConflictResult(
                status, safeCode(assessment.code()), signal);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "IDENTITY_CONFLICT_ASSESSMENT_BLOCKED";
        }

        return switch (code) {
            case "IDENTITY_NO_MATCH_CANDIDATE",
                 "IDENTITY_DEFINITE_CONFLICT",
                 "IDENTITY_POTENTIAL_DUPLICATE",
                 "IDENTITY_CONFLICT_TENANT_MISMATCH",
                 "IDENTITY_CONFLICT_SELF_COMPARISON",
                 "IDENTITY_CONFLICT_EVIDENCE_MISMATCH",
                 "IDENTITY_CONFLICT_TYPE_NOT_APPLICABLE",
                 "IDENTITY_NAMESPACE_OR_VALUE_UNRESOLVED",
                 "IDENTITY_VERIFICATION_UNKNOWN",
                 "IDENTITY_EXPIRY_BOUNDARY_UNRESOLVED" ->
                    code;

            default ->
                "IDENTITY_CONFLICT_ASSESSMENT_BLOCKED";
        };
    }
}