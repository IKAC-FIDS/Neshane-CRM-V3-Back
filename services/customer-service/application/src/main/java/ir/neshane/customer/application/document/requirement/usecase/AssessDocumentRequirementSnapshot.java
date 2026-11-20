package ir.neshane.customer.application.document.requirement.usecase;

import ir.neshane.customer.application.port.in.DocumentRequirementSnapshotUseCase;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementSnapshotQuery;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementSnapshotResult;
import ir.neshane.customer.domain.document.DocumentRequirementPolicy;

import java.util.Objects;

public final class AssessDocumentRequirementSnapshot
        implements DocumentRequirementSnapshotUseCase {

    @Override
    public DocumentRequirementSnapshotResult execute(
            DocumentRequirementSnapshotQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        try {

            var assessment = query.policy().assessSnapshot(
                    query.customer(),
                    query.operation(),
                    query.requestId(),
                    query.snapshot());

            if (assessment.outcome()
                    == DocumentRequirementPolicy.Outcome.ELIGIBLE_CANDIDATE) {

                return new DocumentRequirementSnapshotResult(
                        DocumentRequirementSnapshotResult.Status.ELIGIBLE_CANDIDATE,
                        "DOCUMENT_POLICY_SNAPSHOT_CANDIDATE");
            }

            return blocked(
                    safeCode(assessment.code()));

        } catch (IllegalArgumentException failure) {

            return blocked(
                    safeCode(failure.getMessage()));
        }
    }

    private static DocumentRequirementSnapshotResult blocked(
            String code) {

        return new DocumentRequirementSnapshotResult(
                DocumentRequirementSnapshotResult.Status.BLOCKED,
                code);
    }

    private static String safeCode(String code) {

        if (code == null) {
            return "DOCUMENT_POLICY_SNAPSHOT_BLOCKED";
        }

        return switch (code) {

            case "DOCUMENT_POLICY_SCOPE_MISMATCH",
                 "DOCUMENT_POLICY_SNAPSHOT_REQUIRED",
                 "DOCUMENT_POLICY_SNAPSHOT_SCOPE_MISMATCH",
                 "DOCUMENT_POLICY_SNAPSHOT_STALE",
                 "DOCUMENT_REQUEST_ID_REQUIRED" ->
                    code;

            default ->
                "DOCUMENT_POLICY_SNAPSHOT_BLOCKED";
        };
    }
}