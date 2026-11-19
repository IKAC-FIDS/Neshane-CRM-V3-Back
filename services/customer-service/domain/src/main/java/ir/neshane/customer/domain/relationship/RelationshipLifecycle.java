package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.temporal.TemporalPolicy;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Pure calendar-day and manual END assessment; no mutation or trusted authority. */
public final class RelationshipLifecycle {
    public enum EffectiveStatus { SCHEDULED, EFFECTIVE, EXPIRED, UNAVAILABLE, ENDED }
    public enum Authority { NONE, END_RELATIONSHIP }
    public enum Outcome { ELIGIBLE_CANDIDATE, BLOCKED, REJECTED }

    public static final class EndAssessment {
        private final RelationshipAssignment.Fact source;
        private final Outcome outcome;
        private final String code;
        private final Optional<String> reason;
        private EndAssessment(RelationshipAssignment.Fact source, Outcome outcome, String code, Optional<String> reason) {
            this.source = source;
            this.outcome = outcome;
            this.code = code;
            this.reason = reason;
        }
        public RelationshipAssignment.Fact source() { return source; }
        public Outcome outcome() { return outcome; }
        public String code() { return code; }
        public Optional<String> reason() { return reason; }
        public Optional<Relationship.Status> proposedStatus() {
            return outcome == Outcome.ELIGIBLE_CANDIDATE ? Optional.of(Relationship.Status.ENDED) : Optional.empty();
        }
        public boolean auditRequired() { return outcome == Outcome.ELIGIBLE_CANDIDATE; }
        @Override public String toString() { return "EndAssessment[" + outcome + ", " + code + ", redacted]"; }
    }

    private RelationshipLifecycle() {}

    /** Caller supplies business date explicitly; no timezone or clock inferred. */
    public static EffectiveStatus effectiveStatus(RelationshipAssignment.Fact fact, LocalDate day,
            CustomerStatus personStatus, boolean targetAvailable) {
        Objects.requireNonNull(fact);
        Objects.requireNonNull(day);
        Objects.requireNonNull(personStatus);
        if (fact.status() == Relationship.Status.ENDED) return EffectiveStatus.ENDED;
        var position=RelationshipTemporalPolicy.position(fact,day);
        if (position==TemporalPolicy.Position.BEFORE_START) return EffectiveStatus.SCHEDULED;
        if (position==TemporalPolicy.Position.AFTER_END) return EffectiveStatus.EXPIRED;
        if (personStatus != CustomerStatus.ACTIVE || !targetAvailable) return EffectiveStatus.UNAVAILABLE;
        return EffectiveStatus.EFFECTIVE;
    }

    public static EffectiveStatus effectiveStatus(RelationshipAssignment.Fact fact, LocalDate day, Person person, Company target) {
        requirePerson(fact, person);
        if (!fact.target().equals(RelationshipTarget.of(target)))
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH");
        return effectiveStatus(fact, day, person.status(), target.status() == CustomerStatus.ACTIVE);
    }

    public static EffectiveStatus effectiveStatus(RelationshipAssignment.Fact fact, LocalDate day, Person person, Branch branch, Company parent) {
        requirePerson(fact, person);
        if (!fact.target().equals(RelationshipTarget.of(branch)))
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_SCOPE_MISMATCH");
        return effectiveStatus(fact, day, person.status(), BranchLifecycle.effectiveStatus(branch, parent) == BranchStatus.ACTIVE);
    }

    private static void requirePerson(RelationshipAssignment.Fact fact, Person person) {
        if (!fact.tenant().equals(person.tenantId()) || !fact.person().equals(person.id()))
            throw new IllegalArgumentException("RELATIONSHIP_PERSON_SCOPE_MISMATCH");
    }

    /** END proposes stored status only. Exact effective END date semantics remain open. */
    public static EndAssessment assessEnd(RelationshipAssignment.Fact fact, TenantId tenant,
            long expectedVersion, Authority authority, String reason) {
        Objects.requireNonNull(fact);
        if (!fact.tenant().equals(tenant))
            return reject(fact, Outcome.BLOCKED, "RELATIONSHIP_TENANT_MISMATCH");
        if (authority != Authority.END_RELATIONSHIP)
            return reject(fact, Outcome.BLOCKED, "RELATIONSHIP_END_AUTHORITY_REQUIRED");
        if (expectedVersion != fact.version())
            return reject(fact, Outcome.BLOCKED, "RELATIONSHIP_VERSION_STALE");
        if (fact.status() != Relationship.Status.ACTIVE)
            return reject(fact, Outcome.REJECTED, "RELATIONSHIP_ALREADY_ENDED");
        if (reason == null || reason.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp)))
            return reject(fact, Outcome.REJECTED, "RELATIONSHIP_END_REASON_REQUIRED");
        return new EndAssessment(fact, Outcome.ELIGIBLE_CANDIDATE, "RELATIONSHIP_END_CANDIDATE", Optional.of(reason));
    }
    private static EndAssessment reject(RelationshipAssignment.Fact fact, Outcome outcome, String code) {
        return new EndAssessment(fact, outcome, code, Optional.empty());
    }
}
