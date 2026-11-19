package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;

import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Protected, immutable candidate journal; not persisted Audit or execution evidence. */
public final class DuplicateDecisionHistory {
    public enum Action { REGISTER, REUSE }
    public static final class Entry {
        private final DuplicateDetection.Request request;
        private final long sequence;
        private final Action action;
        private final LifecycleHistory.Context context;
        private final String evidenceRevision;
        private final DuplicateExceptionPolicy.Decision decision;
        private final Optional<String> exceptionReason;
        private Entry(DuplicateDetection.Request request, long sequence, Action action,
                LifecycleHistory.Context context, String revision, DuplicateExceptionPolicy.Decision decision, String reason) {
            this.request = request;
            this.sequence = sequence;
            this.action = action;
            this.context = context;
            this.evidenceRevision = revision;
            this.decision = decision;
            this.exceptionReason = decision.outcome() == DuplicateExceptionPolicy.Outcome.EXCEPTION_CANDIDATE
                ? Optional.of(reason) : Optional.empty();
        }
        public DuplicateDetection.Request request() { return request; }
        public long sequence() { return sequence; }
        public Action action() { return action; }
        public LifecycleHistory.Context context() { return context; }
        public String evidenceRevision() { return evidenceRevision; }
        public DuplicateExceptionPolicy.Decision decision() { return decision; }
        public Optional<String> exceptionReason() { return exceptionReason; }
        public boolean auditRequired() {
            return decision.auditRequired() || decision.code().equals("DUPLICATE_REGISTRATION_AUTHORITY_REQUIRED")
                || decision.code().equals("DUPLICATE_EXCEPTION_AUTHORITY_REQUIRED");
        }
        @Override public String toString() { return "DuplicateDecisionHistory.Entry[protected, redacted]"; }
    }
    private final DuplicateDetection.Request request;
    private final List<Entry> entries;
    private DuplicateDecisionHistory(DuplicateDetection.Request request, List<Entry> entries) {
        this.request = Objects.requireNonNull(request);
        this.entries = List.copyOf(entries);
    }
    public static DuplicateDecisionHistory empty(Customer candidate, DuplicateDetection.Request request) {
        validateScope(candidate, request);
        return new DuplicateDecisionHistory(request, List.of());
    }
    public List<Entry> entries() { return entries; }
    public DuplicateDecisionHistory appendRegistration(Customer candidate, DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> evidence, String revision, DuplicateExceptionPolicy.Authority authority,
            DuplicateExceptionPolicy.Review review, String reason, LifecycleHistory.Context context) {
        validate(candidate, context);
        var decision = DuplicateExceptionPolicy.assessRegistration(candidate, request, coverage, evidence, revision, authority, review, reason);
        return append(Action.REGISTER, context, revision, decision, reason);
    }
    public DuplicateDecisionHistory appendReuse(Customer candidate, DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> evidence, String revision, DuplicateExceptionPolicy.Review review,
            Optional<CustomerId> selected, Set<CustomerId> visible, LifecycleHistory.Context context) {
        validate(candidate, context);
        var decision = DuplicateExceptionPolicy.assessReuse(candidate, request, coverage, evidence, revision, review, selected, visible);
        return append(Action.REUSE, context, revision, decision, null);
    }
    private static void validateScope(Customer candidate, DuplicateDetection.Request request) {
        if (!DuplicateDetection.Request.of(candidate, request.operation(), request.requestId()).equals(request))
            throw new IllegalArgumentException("DUPLICATE_HISTORY_SCOPE_MISMATCH");
    }
    private void validate(Customer candidate, LifecycleHistory.Context context) {
        validateScope(candidate, request);
        Objects.requireNonNull(context, "HISTORY_CONTEXT_REQUIRED");
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty() && context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
    }
    private DuplicateDecisionHistory append(Action action, LifecycleHistory.Context context, String revision,
            DuplicateExceptionPolicy.Decision decision, String reason) {
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(request, entries.size() + 1L, action, context, revision, decision, reason));
        return new DuplicateDecisionHistory(request, next);
    }
    @Override public String toString() { return "DuplicateDecisionHistory[protected, redacted]"; }
}
