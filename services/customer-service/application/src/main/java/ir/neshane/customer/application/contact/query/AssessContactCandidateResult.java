package ir.neshane.customer.application.contact.query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Internal assessment result; not a transport or authorization contract. */
public record AssessContactCandidateResult(
        Status status,
        List<Issue> issues) {

    public enum Status {
        PASS, FAIL, BLOCKED
    }

    public record Issue(
            String code,
            Optional<String> field) {

        public Issue {
            Objects.requireNonNull(code, "ISSUE_CODE_REQUIRED");
            Objects.requireNonNull(field, "ISSUE_FIELD_REQUIRED");
        }
    }

    public AssessContactCandidateResult {
        Objects.requireNonNull(status, "ASSESSMENT_STATUS_REQUIRED");
        issues = List.copyOf(issues);
    }
}
