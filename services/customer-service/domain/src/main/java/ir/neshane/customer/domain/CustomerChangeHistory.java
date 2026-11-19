package ir.neshane.customer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable, protected registration and factual-update history candidates. */
public final class CustomerChangeHistory {
    public enum Action { REGISTER, UPDATE }
    public enum Classification { ORDINARY, CRITICAL, EXCEPTIONAL }
    public static final class Entry {
        private final Optional<CustomerUpdatePolicy.Current> previous;
        private final CustomerUpdatePolicy.Current next;
        private final Action action;
        private final Classification classification;
        private final Set<CriticalFieldChangeAssessment.Field> fields;
        private final LifecycleHistory.Context context;
        private final Optional<String> reason;
        private Entry(CustomerUpdatePolicy.Current previous,CustomerUpdatePolicy.Current next,Action action,
                Classification classification,Set<CriticalFieldChangeAssessment.Field> fields,LifecycleHistory.Context context,Optional<String> reason) {
            this.previous = Optional.ofNullable(previous);
            this.next = next;
            this.action = action;
            this.classification = classification;
            this.fields = Set.copyOf(fields);
            this.context = context;
            this.reason = reason;
        }
        public IdentityOwner owner() { return next.version().owner(); }
        public Optional<CustomerRecordVersion> previousVersion() { return previous.map(CustomerUpdatePolicy.Current::version); }
        public CustomerRecordVersion nextVersion() { return next.version(); }
        public Optional<String> previousIdentityRevision() { return previous.map(c -> c.identity().value()); }
        public String nextIdentityRevision() { return next.identity().value(); }
        public Optional<VerificationStatus> previousVerification() { return previous.map(c -> c.identity().verificationStatus()); }
        public VerificationStatus nextVerification() { return next.identity().verificationStatus(); }
        public Action action() { return action; }
        public Classification classification() { return classification; }
        public Set<CriticalFieldChangeAssessment.Field> fields() { return fields; }
        public LifecycleHistory.Context context() { return context; }
        public Optional<String> reason() { return reason; }
        @Override public String toString() { return "CustomerChangeHistory.Entry[protected, redacted]"; }
    }
    private final List<Entry> entries;
    private CustomerChangeHistory(List<Entry> entries) { this.entries = List.copyOf(entries); }
    public static CustomerChangeHistory start(CustomerRegistration.Candidate registration,String initialRevision,LifecycleHistory.Context context) {
        Objects.requireNonNull(registration,"CUSTOMER_REGISTRATION_CANDIDATE_REQUIRED");
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        var current = new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(registration.customer(),initialRevision,VerificationStatus.NOT_VERIFIED),
            CustomerStatus.ACTIVE,registration.version());
        var classification = registration.exceptionReason().isPresent() ? Classification.EXCEPTIONAL : Classification.ORDINARY;
        return new CustomerChangeHistory(List.of(new Entry(null,current,Action.REGISTER,classification,Set.of(),context,registration.exceptionReason())));
    }
    public List<Entry> entries() { return entries; }
    public CustomerUpdatePolicy.Current current() { return entries.getLast().next; }
    public CustomerChangeHistory append(CustomerUpdateVerification.Candidate update) {
        Objects.requireNonNull(update,"CUSTOMER_UPDATE_CANDIDATE_REQUIRED");
        var previous = update.previous();
        var next = update.record();
        var last = current();
        if (!last.version().owner().equals(previous.version().owner())) throw new IllegalArgumentException("CUSTOMER_HISTORY_SCOPE_MISMATCH");
        if (!last.version().equals(previous.version()) || next.version().value() != previous.version().value()+1)
            throw new IllegalArgumentException("CUSTOMER_HISTORY_VERSION_DISCONTINUITY");
        // Lifecycle/verification-only operations can occur between factual updates.
        if (!last.identity().value().equals(previous.identity().value()) || CustomerUpdatePolicy.changed(last.identity().candidate(),previous.identity().candidate()))
            throw new IllegalArgumentException("CUSTOMER_HISTORY_IDENTITY_DISCONTINUITY");
        var change = update.history().entries().getLast();
        var context = change.context();
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        var fields = update.fields();
        var classification = fields.contains(CriticalFieldChangeAssessment.Field.EXCEPTIONAL_CHANGE) ? Classification.EXCEPTIONAL :
            fields.stream().anyMatch(CriticalFieldChangeAssessment::critical) ? Classification.CRITICAL : Classification.ORDINARY;
        var copy = new ArrayList<>(entries);
        copy.add(new Entry(previous,next,Action.UPDATE,classification,fields,context,change.reason()));
        return new CustomerChangeHistory(copy);
    }
    @Override public String toString() { return "CustomerChangeHistory[protected, redacted]"; }
}
