package ir.neshane.customer.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable owner-bound candidate collection. Nothing here grants write authority. */
public record ContactInfo(ContactOwner owner, List<ContactPoint> points) {
    public ContactInfo {
        Objects.requireNonNull(owner, "CONTACT_OWNER_REQUIRED");
        Objects.requireNonNull(points, "CONTACT_POINTS_REQUIRED");
        points = List.copyOf(points);
        Set<String> ids = new HashSet<>();
        for (ContactPoint point : points) {
            if (!owner.equals(point.owner())) {
                throw new IllegalArgumentException("CONTACT_POINT_OWNER_MISMATCH");
            }
            if (!ids.add(point.id())) {
                throw new IllegalArgumentException("CONTACT_POINT_ID_DUPLICATE");
            }
        }
    }

    public static ContactInfo empty(ContactOwner owner) {
        return new ContactInfo(owner, List.of());
    }

    public ContactInfo withCandidatePoint(ContactPoint point) {
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
        List<ContactPoint> candidate = new ArrayList<>(points);
        candidate.add(point);
        return new ContactInfo(owner, candidate);
    }

    static ContactInfo forOwner(ContactOwner expected, ContactInfo supplied) {
        if (supplied == null) {
            return empty(expected);
        }
        if (!expected.equals(supplied.owner())) {
            throw new IllegalArgumentException("CONTACT_INFO_OWNER_MISMATCH");
        }
        return supplied;
    }

    @Override
    public String toString() { return "ContactInfo[redacted]"; }
}
