package ir.neshane.customer.domain;

import ir.neshane.customer.domain.contact.ContactGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable owner-bound candidate collection. Nothing here grants write authority. */
public final class ContactInfo {
    private final ContactOwner owner;
    private final List<ContactPoint> points;
    private final Map<ContactGroup, String> primary;

    public ContactInfo(ContactOwner owner, List<ContactPoint> points) {
        this(owner, points, Map.of());
    }

    private ContactInfo(ContactOwner owner, List<ContactPoint> points, Map<ContactGroup, String> primary) {
        this.owner = Objects.requireNonNull(owner, "CONTACT_OWNER_REQUIRED");
        this.points = List.copyOf(Objects.requireNonNull(points, "CONTACT_POINTS_REQUIRED"));
        this.primary = Map.copyOf(primary);
        Set<String> ids = new HashSet<>();
        for (ContactPoint point : this.points) {
            if (!owner.equals(point.owner())) {
                throw new IllegalArgumentException("CONTACT_POINT_OWNER_MISMATCH");
            }
            if (!ids.add(point.id())) {
                throw new IllegalArgumentException("CONTACT_POINT_ID_DUPLICATE");
            }
        }
    }

    public ContactOwner owner() { return owner; }
    public List<ContactPoint> points() { return points; }
    public Optional<String> primary(ContactGroup group) {
        return Optional.ofNullable(primary.get(Objects.requireNonNull(group, "CONTACT_GROUP_REQUIRED")));
    }

    public static ContactInfo empty(ContactOwner owner) {
        return new ContactInfo(owner, List.of());
    }

    public ContactInfo withCandidatePoint(ContactPoint point) {
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
        List<ContactPoint> candidate = new ArrayList<>(points);
        candidate.add(point);
        return new ContactInfo(owner, candidate, primary);
    }

    ContactPoint requirePoint(String id) {
        return points.stream().filter(point -> point.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("CONTACT_POINT_UNKNOWN"));
    }

    ContactInfo selectPrimary(ContactGroup group, String id) {
        ContactPoint point = requirePoint(id);
        if (point.status() != ContactPoint.Status.ACTIVE) {
            throw new IllegalArgumentException("CONTACT_POINT_NOT_ACTIVE");
        }
        Map<ContactGroup, String> next = new HashMap<>(primary);
        next.put(Objects.requireNonNull(group, "CONTACT_GROUP_REQUIRED"), id);
        return new ContactInfo(owner, points, next);
    }

    public ContactInfo clearPrimary(ContactGroup group) {
        Map<ContactGroup, String> next = new HashMap<>(primary);
        next.remove(Objects.requireNonNull(group, "CONTACT_GROUP_REQUIRED"));
        return new ContactInfo(owner, points, next);
    }

    public ContactInfo deactivateCandidate(String id) {
        ContactPoint point = requirePoint(id);
        List<ContactPoint> next = points.stream().map(existing -> existing.id().equals(id)
            ? point.inactive() : existing).toList();
        Map<ContactGroup, String> nextPrimary = new HashMap<>(primary);
        nextPrimary.values().removeIf(id::equals);
        return new ContactInfo(owner, next, nextPrimary);
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

    @Override
    public boolean equals(Object other) {
        return other instanceof ContactInfo info && owner.equals(info.owner)
            && points.equals(info.points) && primary.equals(info.primary);
    }

    @Override
    public int hashCode() { return Objects.hash(owner, points, primary); }
}
