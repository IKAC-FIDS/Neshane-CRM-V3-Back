package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.shared.temporal.TemporalPolicy;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Calendar-day half-open relationship intervals; END does not infer or rewrite an end date. */
public final class RelationshipTemporalPolicy {
    private RelationshipTemporalPolicy() {}
    static void requireInterval(LocalDate start,Optional<LocalDate> end) {
        try { TemporalPolicy.position(start,end,start,TemporalPolicy.EndBoundary.EXCLUSIVE); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("RELATIONSHIP_INTERVAL_INVALID"); }
    }
    public static TemporalPolicy.Position position(RelationshipAssignment.Fact fact,LocalDate day) {
        Objects.requireNonNull(fact);
        return TemporalPolicy.position(fact.start(),fact.end(),day,TemporalPolicy.EndBoundary.EXCLUSIVE);
    }
    public static boolean overlaps(RelationshipAssignment.Fact first,RelationshipAssignment.Fact second) {
        Objects.requireNonNull(first); Objects.requireNonNull(second);
        return TemporalPolicy.overlapsExclusive(first.start(),first.end(),second.start(),second.end());
    }
}
