package ir.neshane.customer.domain.customer.state;

import ir.neshane.customer.domain.document.DocumentValidity;
import ir.neshane.customer.domain.verification.DocumentVerification;
import ir.neshane.customer.domain.document.DocumentVersionHistory;
import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.verification.ManualVerification;
import ir.neshane.customer.domain.verification.VerificationEvidence;
import ir.neshane.customer.domain.verification.VerificationEvidenceSet;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.shared.validation.DomainText;
import ir.neshane.customer.domain.customer.model.Customer;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable protected candidate history, not a durable journal. */
public final class VerificationHistory {
    public enum Action { MANUAL_VERIFY, IDENTITY_CHANGE, REVOCATION }
    public static final class Entry {
        private final IdentityRevision previous;
        private final IdentityRevision proposed;
        private final long sequence;
        private final Action action;
        private final LifecycleHistory.Context context;
        private final Instant effectiveAt;
        private final String code;
        private final Optional<String> reason;
        private final List<VerificationEvidence> evidence;
        private final Optional<String> proofReference;
        private Entry(IdentityRevision previous, IdentityRevision proposed, long sequence, Action action,
                LifecycleHistory.Context context, Instant effectiveAt, String code, String reason,
                List<VerificationEvidence> evidence, String proofReference) {
            this.previous = previous;
            this.proposed = proposed;
            this.sequence = sequence;
            this.action = action;
            this.context = context;
            this.effectiveAt = effectiveAt;
            this.code = code;
            this.reason = Optional.ofNullable(reason);
            this.evidence = List.copyOf(evidence);
            this.proofReference = Optional.ofNullable(proofReference);
        }
        public IdentityOwner owner() { return previous.owner(); }
        public String previousRevision() { return previous.value(); }
        public String proposedRevision() { return proposed.value(); }
        public VerificationStatus previousStatus() { return previous.verificationStatus(); }
        public VerificationStatus proposedStatus() { return proposed.verificationStatus(); }
        public long sequence() { return sequence; }
        public Action action() { return action; }
        public LifecycleHistory.Context context() { return context; }
        public Instant effectiveAt() { return effectiveAt; }
        public String code() { return code; }
        public Optional<String> reason() { return reason; }
        public List<VerificationEvidence> evidence() { return evidence; }
        public Optional<String> proofReference() { return proofReference; }
        public boolean auditRequired() { return true; }
        @Override public String toString() { return "VerificationHistory.Entry[protected, redacted]"; }
    }
    private final IdentityRevision current;
    private final List<Entry> entries;
    private final Set<String> excludedVerifiers;
    private VerificationHistory(IdentityRevision current,List<Entry> entries,Set<String> excludedVerifiers) {
        this.current = Objects.requireNonNull(current);
        this.entries = List.copyOf(entries);
        this.excludedVerifiers = Set.copyOf(excludedVerifiers);
    }
    public static VerificationHistory empty(IdentityRevision initial) {
        return new VerificationHistory(initial,List.of(),Set.of());
    }
    public IdentityRevision current() { return current; }
    public List<Entry> entries() { return entries; }
    public Set<String> excludedVerifiers() { return excludedVerifiers; }

    public VerificationHistory appendManual(String historyId, VerificationEvidenceSet evidence,
            ManualVerification.Context context, ManualVerification.Review review, DuplicateDetection.Request request,
            DuplicateDetection.Coverage coverage,List<DuplicateDetection.Comparison> comparisons) {
        var metadata = new LifecycleHistory.Context(historyId,context.actorId(),context.decisionAt(),context.correlationId());
        validate(metadata);
        if (!context.excludedActors().containsAll(excludedVerifiers) || excludedVerifiers.contains(context.actorId())
                || (!excludedVerifiers.isEmpty() && context.process() == ManualVerification.Process.ORDINARY))
            throw new IllegalArgumentException("VERIFICATION_HISTORY_SOD_REQUIRED");
        var result = current.assessManualVerification(evidence,context,review,request,coverage,comparisons);
        if (result.outcome() != ManualVerification.Outcome.ELIGIBLE_CANDIDATE) throw new IllegalArgumentException(result.code());
        var next = current.withVerificationStatus(result.proposed().orElseThrow());
        return append(next,Action.MANUAL_VERIFY,metadata,context.decisionAt(),result.code(),context.reason(),evidence.evidence(),null,Set.of());
    }
    public VerificationHistory appendIdentityChange(String expectedRevision,Customer proposed,String nextRevision,
            IdentityRevisionAssessment.Authority authority,IdentityRevisionAssessment.Impact impact,
            IdentityRevisionAssessment.IdentifierCorrection correction,String reason,LifecycleHistory.Context context) {
        validate(context);
        var result = IdentityRevisionAssessment.assess(current,expectedRevision,proposed,nextRevision,authority,impact,correction,reason);
        if (result.outcome() != IdentityRevisionAssessment.Outcome.ELIGIBLE_CANDIDATE) throw new IllegalArgumentException(result.code());
        return append(result.proposed().orElseThrow(),Action.IDENTITY_CHANGE,context,context.occurredAt(),result.code(),reason,List.of(),null,
            exclusions(result.differentVerifierRequired(),context.actorId()));
    }
    public VerificationHistory appendRevocation(String historyId,VerificationEvidenceSet basis,
            VerificationRevocation.Context context,VerificationRevocation.Qualification qualification) {
        var metadata = new LifecycleHistory.Context(historyId,context.actorId(),context.recordedAt(),context.correlationId());
        validate(metadata);
        var result = VerificationRevocation.assess(current,basis,context,qualification);
        if (result.outcome() != VerificationRevocation.Outcome.ELIGIBLE_CANDIDATE &&
                !(result.outcome() == VerificationRevocation.Outcome.UNCHANGED && result.auditRequired()))
            throw new IllegalArgumentException(result.code());
        var previousVerification = entries.reversed().stream().filter(e -> e.action() == Action.MANUAL_VERIFY).findFirst();
        if (previousVerification.isPresent() && previousVerification.orElseThrow().evidence().stream().noneMatch(e ->
                e.identityRevision().equals(context.basisRevision()) && e.evidenceId().equals(context.evidenceId()) &&
                basis.evidence().stream().anyMatch(b -> b.evidenceId().equals(e.evidenceId()) &&
                    b.evidenceVersion() == e.evidenceVersion() && b.protectedReference().equals(e.protectedReference())
                    && b.documentBinding().equals(e.documentBinding()))))
            throw new IllegalArgumentException("VERIFICATION_HISTORY_BASIS_MISMATCH");
        return append(result.proposed().orElseThrow(),Action.REVOCATION,metadata,context.effectiveAt(),result.code(),context.reason(),basis.evidence(),context.proofReference(),
            exclusions(result.differentVerifierRequired(),context.actorId()));
    }
    public VerificationHistory appendDocumentImpact(String expectedRevision,DocumentVersionHistory document,
            LocalDate effectiveDate,Instant effectiveAt,DocumentValidity.ExpiryBoundary boundary,
            VerificationRevocation.Authority authority,String reason,String proofReference,LifecycleHistory.Context context) {
        validate(context);
        DomainText.required(reason,"DOCUMENT_IMPACT_REASON",4000);
        IdentifierText.requireNonBlank(proofReference,"VERIFICATION_REVOCATION_EVIDENCE_REQUIRED");
        if (effectiveAt.isAfter(context.occurredAt())) throw new IllegalArgumentException("VERIFICATION_REVOCATION_TIME_UNRESOLVED");
        var result = DocumentVerificationImpact.assessImpact(this,expectedRevision,document,effectiveDate,effectiveAt,boundary,authority);
        if (result.outcome() != DocumentVerification.Outcome.REVERIFICATION_REQUIRED_CANDIDATE)
            throw new IllegalArgumentException(result.code());
        var prior = entries.reversed().stream().filter(e -> e.action() == Action.MANUAL_VERIFY).findFirst().orElseThrow();
        var next = current.withVerificationStatus(result.transition().orElseThrow().proposed().orElseThrow());
        return append(next,Action.REVOCATION,context,effectiveAt,result.code(),reason,prior.evidence(),proofReference,excludedVerifiers);
    }
    private Set<String> exclusions(boolean different,String actor) {
        var next = new HashSet<>(excludedVerifiers);
        if (different) next.add(actor);
        return next;
    }
    private void validate(LifecycleHistory.Context context) {
        Objects.requireNonNull(context,"HISTORY_CONTEXT_REQUIRED");
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty() && context.occurredAt().isBefore(entries.getLast().context().occurredAt()))
            throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
    }
    private VerificationHistory append(IdentityRevision next,Action action,LifecycleHistory.Context context,
            Instant effective,String code,String reason,List<VerificationEvidence> evidence,String proof,Set<String> excluded) {
        var copy = new ArrayList<>(entries);
        copy.add(new Entry(current,next,entries.size()+1L,action,context,effective,code,reason,evidence,proof));
        return new VerificationHistory(next,copy,excluded);
    }
    @Override public String toString() { return "VerificationHistory[protected, redacted]"; }
}
