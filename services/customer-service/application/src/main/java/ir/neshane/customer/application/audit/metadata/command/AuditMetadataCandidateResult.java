package ir.neshane.customer.application.audit.metadata.command;

import ir.neshane.customer.domain.access.AuditRequirementsPolicy;

import java.util.Objects;
import java.util.Optional;

/**
 * Protected process-local audit metadata candidate.
 * Not a durable audit record.
 */
public record AuditMetadataCandidateResult(
        Status status,
        String code,
        Optional<AuditRequirementsPolicy.Metadata> metadata) {

    public enum Status {
        PREPARED_CANDIDATE,
        BLOCKED
    }

    public AuditMetadataCandidateResult {

        Objects.requireNonNull(status);
        Objects.requireNonNull(code);
        Objects.requireNonNull(metadata);

        if ((status == Status.PREPARED_CANDIDATE)
                != metadata.isPresent()) {
            throw new IllegalArgumentException(
                    "AUDIT_METADATA_RESULT_INCONSISTENT");
        }

        if ((status == Status.PREPARED_CANDIDATE)
                != code.equals("AUDIT_METADATA_PREPARED_CANDIDATE")) {
            throw new IllegalArgumentException(
                    "AUDIT_METADATA_CODE_INCONSISTENT");
        }

        if (code.isBlank()) {
            throw new IllegalArgumentException(
                    "AUDIT_METADATA_CODE_REQUIRED");
        }
    }

    @Override
    public String toString() {
        return "AuditMetadataCandidateResult[redacted]";
    }
}
