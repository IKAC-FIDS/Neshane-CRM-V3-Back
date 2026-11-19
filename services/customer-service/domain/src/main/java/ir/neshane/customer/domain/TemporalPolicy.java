package ir.neshane.customer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Explicit evaluation inputs and pure interval semantics; no clock or timezone inference. */
public final class TemporalPolicy {
    public record Context(LocalDate businessDate,Instant asOf) {
        public Context {
            Objects.requireNonNull(businessDate,"BUSINESS_DATE_REQUIRED");
            Objects.requireNonNull(asOf,"EVALUATION_INSTANT_REQUIRED");
        }
    }
    public enum EndBoundary { EXCLUSIVE, INCLUSIVE, UNRESOLVED }
    public enum Position { BEFORE_START, WITHIN, AFTER_END, BOUNDARY_UNRESOLVED }
    private TemporalPolicy() {}

    /** Start is inclusive; missing end is unbounded. The caller selects the end convention explicitly. */
    public static <T extends Comparable<? super T>> Position position(T start,Optional<T> end,T asOf,EndBoundary boundary) {
        Objects.requireNonNull(start,"TEMPORAL_START_REQUIRED");
        Objects.requireNonNull(end,"TEMPORAL_END_OPTIONAL_REQUIRED");
        Objects.requireNonNull(asOf,"TEMPORAL_AS_OF_REQUIRED");
        Objects.requireNonNull(boundary,"TEMPORAL_BOUNDARY_REQUIRED");
        if (end.isPresent()) {
            int comparison=end.orElseThrow().compareTo(start);
            if (comparison<0 || (comparison==0 && boundary!=EndBoundary.INCLUSIVE))
                throw new IllegalArgumentException("TEMPORAL_INTERVAL_INVALID");
        }
        if (asOf.compareTo(start)<0) return Position.BEFORE_START;
        if (end.isEmpty()) return Position.WITHIN;
        int comparison=asOf.compareTo(end.orElseThrow());
        if (comparison<0) return Position.WITHIN;
        if (comparison>0) return Position.AFTER_END;
        return switch (boundary) {
            case EXCLUSIVE -> Position.AFTER_END;
            case INCLUSIVE -> Position.WITHIN;
            case UNRESOLVED -> Position.BOUNDARY_UNRESOLVED;
        };
    }
}
