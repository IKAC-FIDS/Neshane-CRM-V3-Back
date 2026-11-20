package ir.neshane.customer.application.verification.lifecycle.usecase;

import ir.neshane.customer.application.port.in.VerificationLifecycleUseCase;
import ir.neshane.customer.application.verification.lifecycle.query.VerificationLifecycleQuery;
import ir.neshane.customer.application.verification.lifecycle.query.VerificationLifecycleResult;
import ir.neshane.customer.domain.verification.model.VerificationLifecycle;

import java.util.Objects;
import java.util.Optional;

public final class AssessVerificationLifecycle
        implements VerificationLifecycleUseCase {

    @Override
    public VerificationLifecycleResult execute(
            VerificationLifecycleQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = VerificationLifecycle.assess(
                query.current(),
                query.trigger());

        var status = switch (assessment.outcome()) {

            case ELIGIBLE_CANDIDATE ->
                VerificationLifecycleResult.Status.ELIGIBLE_CANDIDATE;

            case UNCHANGED ->
                VerificationLifecycleResult.Status.UNCHANGED;

            case BLOCKED ->
                VerificationLifecycleResult.Status.BLOCKED;
        };

        Optional<VerificationLifecycleResult.IdentityState> proposed =
                status == VerificationLifecycleResult.Status.BLOCKED
                ? Optional.empty()
                : assessment.proposed().map(state -> switch (state) {

                    case NOT_VERIFIED ->
                        VerificationLifecycleResult.IdentityState.NOT_VERIFIED;

                    case VERIFIED ->
                        VerificationLifecycleResult.IdentityState.VERIFIED;

                    case REQUIRES_REVERIFICATION ->
                        VerificationLifecycleResult.IdentityState.REQUIRES_REVERIFICATION;
                });

        return new VerificationLifecycleResult(
                status,
                safeCode(assessment.code()),
                assessment.auditRequired(),
                proposed);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "VERIFICATION_LIFECYCLE_ASSESSMENT_BLOCKED";
        }

        return switch (code) {

            case "VERIFICATION_TRUSTED_WORKFLOW_REQUIRED",
                 "VERIFICATION_REVERIFICATION_CANDIDATE",
                 "VERIFICATION_INVALIDATION_CANDIDATE",
                 "VERIFICATION_STATUS_UNCHANGED" ->
                    code;

            default ->
                "VERIFICATION_LIFECYCLE_ASSESSMENT_BLOCKED";
        };
    }
}