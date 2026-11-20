package ir.neshane.customer.application.contact.query;

import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import java.util.Objects;

/** Internal, immutable query for synthetic candidate assessment. */
public record AssessContactCandidateQuery(
        ValidationProfile profile,
        ContactPoint point) {

    public AssessContactCandidateQuery {
        Objects.requireNonNull(profile, "VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
    }

    @Override
    public String toString() {
        return "AssessContactCandidateQuery[redacted]";
    }
}
