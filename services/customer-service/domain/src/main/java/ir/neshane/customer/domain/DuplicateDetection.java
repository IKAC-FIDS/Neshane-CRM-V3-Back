package ir.neshane.customer.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure aggregation of synthetic comparison evidence; no lookup, matching or authorization. */
public final class DuplicateDetection {
    public enum Operation { CREATE, UPDATE }
    public enum Coverage { COMPLETE_CANDIDATE, UNKNOWN }
    public enum Outcome { NO_MATCH, POTENTIAL_DUPLICATE, DEFINITE_CONFLICT, BLOCKED }
    public enum Signal { VERIFIED_VALID_IDENTIFIER, UNVERIFIED_IDENTIFIER, NAME, PHONE, EMAIL }
    public record Request(TenantId tenant, CustomerId customerId, IdentityOwner.Kind kind, Operation operation,
                          String requestId) {
        public Request {
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(customerId);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(operation);
            IdentityIdentifier.required(requestId, "DUPLICATE_REQUEST_ID_REQUIRED");
        }
        public static Request of(Customer candidate, Operation operation, String requestId) {
            return new Request(candidate.tenantId(), candidate.id(), candidate.identityInfo().owner().kind(), operation, requestId);
        }
        @Override public String toString() { return "DuplicateRequest[redacted]"; }
    }
    /** One comparison per subject; raw values are intentionally absent. */
    public record Comparison(Request request, TenantId tenant, CustomerId customerId, IdentityOwner.Kind kind,
                             Set<Signal> signals) {
        public Comparison {
            Objects.requireNonNull(request);
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(customerId);
            Objects.requireNonNull(kind);
            signals = Set.copyOf(signals);
        }
        @Override public String toString() { return "DuplicateComparison[redacted]"; }
    }
    public enum WarningPriority { NONE, STANDARD, HIGH }
    public record Assessment(Outcome outcome, String code, WarningPriority warningPriority) {
        public Assessment(Outcome outcome, String code) { this(outcome, code, WarningPriority.NONE); }
    }
    private DuplicateDetection() {}
    public static Assessment assess(Customer candidate, Request request, Coverage coverage, List<Comparison> comparisons) {
        if (!Request.of(candidate, request.operation(), request.requestId()).equals(request))
            return new Assessment(Outcome.BLOCKED, "DUPLICATE_REQUEST_SCOPE_MISMATCH");
        if (coverage != Coverage.COMPLETE_CANDIDATE)
            return new Assessment(Outcome.BLOCKED, "DUPLICATE_EVIDENCE_MISSING");
        Set<CustomerId> seen = new HashSet<>();
        boolean definite = false;
        boolean potential = false;
        boolean multipleSignals = false;
        for (var comparison : List.copyOf(comparisons)) {
            if (!request.equals(comparison.request()) || !request.tenant().equals(comparison.tenant()))
                return new Assessment(Outcome.BLOCKED, "DUPLICATE_EVIDENCE_SCOPE_MISMATCH");
            if (!seen.add(comparison.customerId()))
                return new Assessment(Outcome.BLOCKED, "DUPLICATE_EVIDENCE_DUPLICATE");
            if (request.customerId().equals(comparison.customerId())) {
                if (request.operation() == Operation.CREATE)
                    return new Assessment(Outcome.BLOCKED, "DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS");
                if (request.kind() != comparison.kind())
                    return new Assessment(Outcome.BLOCKED, "DUPLICATE_EVIDENCE_KIND_MISMATCH");
                continue;
            }
            if (comparison.signals().contains(Signal.VERIFIED_VALID_IDENTIFIER)) definite = true;
            if (!comparison.signals().isEmpty()) potential = true;
            if (comparison.signals().size() > 1) multipleSignals = true;
        }
        if (definite) return new Assessment(Outcome.DEFINITE_CONFLICT, "DUPLICATE_DEFINITE_CONFLICT");
        if (potential) return new Assessment(Outcome.POTENTIAL_DUPLICATE, "DUPLICATE_WARNING",
            multipleSignals ? WarningPriority.HIGH : WarningPriority.STANDARD);
        return new Assessment(Outcome.NO_MATCH, "DUPLICATE_NO_MATCH_CANDIDATE");
    }
}
