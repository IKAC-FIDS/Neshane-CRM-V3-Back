package ir.neshane.customer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable factual change history candidates, separate from lifecycle history. */
public final class BranchChangeHistory {
    public enum Action { REGISTER, UPDATE }
    public static final class Entry {
        private final Optional<BranchRecordVersion> previousVersion;
        private final BranchRecordVersion nextVersion;
        private final Action action;
        private final Set<BranchUpdatePolicy.Field> fields;
        private final LifecycleHistory.Context context;
        private Entry(BranchRecordVersion previous,BranchRecordVersion next,Action action,
                Set<BranchUpdatePolicy.Field> fields,LifecycleHistory.Context context) {
            this.previousVersion=Optional.ofNullable(previous);
            this.nextVersion=next;
            this.action=action;
            this.fields=Set.copyOf(fields);
            this.context=context;
        }
        public BranchRecordVersion.Owner owner() { return nextVersion.owner(); }
        public Optional<BranchRecordVersion> previousVersion() { return previousVersion; }
        public BranchRecordVersion nextVersion() { return nextVersion; }
        public Action action() { return action; }
        public Set<BranchUpdatePolicy.Field> fields() { return fields; }
        public LifecycleHistory.Context context() { return context; }
        @Override public String toString() { return "BranchChangeHistory.Entry[protected, redacted]"; }
    }
    private final List<Entry> entries;
    private final BranchUpdatePolicy.Current current;
    private final List<LifecycleHistory.Context> timeline;
    private BranchChangeHistory(List<Entry> entries,BranchUpdatePolicy.Current current) {
        this(entries,current,entries.stream().map(Entry::context).toList());
    }
    private BranchChangeHistory(List<Entry> entries,BranchUpdatePolicy.Current current,List<LifecycleHistory.Context> timeline) {
        this.entries=List.copyOf(entries);
        this.current=current;
        this.timeline=List.copyOf(timeline);
    }
    public static BranchChangeHistory start(BranchRegistration.Candidate registration,LifecycleHistory.Context context) {
        Objects.requireNonNull(registration,"BRANCH_REGISTRATION_CANDIDATE_REQUIRED");
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        var current=new BranchUpdatePolicy.Current(registration.branch(),BranchStatus.ACTIVE,registration.version());
        return new BranchChangeHistory(List.of(new Entry(null,current.version(),Action.REGISTER,Set.of(),context)),current);
    }
    public List<Entry> entries() { return entries; }
    public BranchUpdatePolicy.Current current() { return current; }
    public BranchChangeHistory advanceLifecycle(BranchLifecycleIntegration.Candidate change) {
        Objects.requireNonNull(change,"BRANCH_LIFECYCLE_CANDIDATE_REQUIRED");
        var previous=change.previous();
        if (!current.version().equals(previous.version()) || current.status()!=previous.status() ||
                !BranchUpdatePolicy.differences(current.branch(),previous.branch()).isEmpty())
            throw new IllegalArgumentException("BRANCH_HISTORY_LIFECYCLE_DISCONTINUITY");
        return new BranchChangeHistory(entries,change.record(),advanceTimeline(change.history().entries().getLast().context()));
    }
    private List<LifecycleHistory.Context> advanceTimeline(LifecycleHistory.Context context) {
        if (timeline.stream().anyMatch(e -> e.historyId().equals(context.historyId()))) throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (context.occurredAt().isBefore(timeline.getLast().occurredAt())) throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        var copy=new ArrayList<>(timeline); copy.add(context); return copy;
    }
    public BranchChangeHistory append(BranchUpdatePolicy.Candidate update,LifecycleHistory.Context context) {
        Objects.requireNonNull(update,"BRANCH_UPDATE_CANDIDATE_REQUIRED");
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        var previous=update.previous();
        var next=update.record();
        if (!current.version().owner().equals(previous.version().owner()) || !previous.version().owner().equals(next.version().owner()))
            throw new IllegalArgumentException("BRANCH_HISTORY_SCOPE_MISMATCH");
        if (!current.version().equals(previous.version()) || previous.version().value()==Long.MAX_VALUE || next.version().value()!=previous.version().value()+1)
            throw new IllegalArgumentException("BRANCH_HISTORY_VERSION_DISCONTINUITY");
        if (!BranchUpdatePolicy.differences(current.branch(),previous.branch()).isEmpty())
            throw new IllegalArgumentException("BRANCH_HISTORY_FACT_DISCONTINUITY");
        // Lifecycle-only changes may occur between factual updates; an update itself preserves status.
        var fields=BranchUpdatePolicy.differences(previous.branch(),next.branch());
        if (previous.status()!=next.status() || fields.isEmpty() || !fields.equals(update.fields()))
            throw new IllegalArgumentException("BRANCH_HISTORY_CHANGE_INVALID");
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        var copy=new ArrayList<>(entries);
        copy.add(new Entry(previous.version(),next.version(),Action.UPDATE,fields,context));
        return new BranchChangeHistory(copy,next,advanceTimeline(context));
    }
    @Override public String toString() { return "BranchChangeHistory[protected, redacted]"; }
}
