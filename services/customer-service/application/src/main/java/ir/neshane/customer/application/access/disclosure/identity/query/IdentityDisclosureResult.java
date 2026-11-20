package ir.neshane.customer.application.access.disclosure.identity.query;

import java.util.Objects;

/**
 * Logical disclosure disposition only.
 * Never contains identity values or masked representations.
 */
public record IdentityDisclosureResult(
        Status status,
        String code) {

    public enum Status {
        DENIED,
        MASK_REQUIRED,
        ELIGIBLE_CANDIDATE
    }

    public IdentityDisclosureResult {

        Objects.requireNonNull(status);
        Objects.requireNonNull(code);

        String expected = switch (status) {
            case DENIED ->
                "IDENTITY_DISCLOSURE_DENIED";

            case MASK_REQUIRED ->
                "IDENTITY_DISCLOSURE_MASK_REQUIRED";

            case ELIGIBLE_CANDIDATE ->
                "IDENTITY_DISCLOSURE_ELIGIBLE_CANDIDATE";
        };

        if (!expected.equals(code)) {
            throw new IllegalArgumentException(
                    "IDENTITY_DISCLOSURE_RESULT_INCONSISTENT");
        }
    }

    @Override
    public String toString() {
        return "IdentityDisclosureResult[redacted]";
    }
}
