package ir.neshane.customer.application.relationship;

import ir.neshane.customer.application.relationship.assignment.command.*;
import ir.neshane.customer.application.relationship.assignment.usecase.AssessRelationshipAssignment;
import ir.neshane.customer.application.relationship.lifecycle.command.*;
import ir.neshane.customer.application.relationship.lifecycle.usecase.CreateRelationshipCandidate;
import ir.neshane.customer.application.relationship.lifecycle.usecase.EndRelationshipCandidate;
import ir.neshane.customer.application.relationship.status.query.*;
import ir.neshane.customer.application.relationship.status.usecase.AssessRelationshipEffectiveStatus;
import ir.neshane.customer.application.port.in.*;

import ir.neshane.customer.domain.relationship.*;
import ir.neshane.customer.domain.branch.model.*;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.shared.identifier.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ir.neshane.customer.domain.relationship.RelationshipAssignment.Authority.*;
import static ir.neshane.customer.domain.relationship.RelationshipAssignment.Coverage.*;
import static ir.neshane.customer.application.relationship.assignment.command.RelationshipAssignmentResult.Status.*;
import static ir.neshane.customer.application.relationship.status.query.RelationshipEffectiveStatusResult.EffectiveState.*;

public final class RelationshipApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("synthetic-tenant");

    private static final LocalDate DAY =
            LocalDate.of(2030, 1, 1);

    private static final BusinessRoleCatalog ROLES =
            BusinessRoleCatalog.initial();

    private static final BusinessRoleCatalog.Ref ROLE =
            new BusinessRoleCatalog.Ref(
                    "TECHNICAL_CONTACT", Optional.empty());

    private static final RelationshipAssignmentUseCase ASSIGNMENT =
            new AssessRelationshipAssignment();

    private static final CreateRelationshipCandidateUseCase CREATE =
            new CreateRelationshipCandidate();

    private static final EndRelationshipCandidateUseCase END =
            new EndRelationshipCandidate();

    private static final RelationshipEffectiveStatusUseCase STATUS =
            new AssessRelationshipEffectiveStatus();

    private RelationshipApplicationSpecification() {}

    public static void run() {

        checks = 0;

        var person = Person.createCandidate(
                new CustomerId("synthetic-person"),
                TENANT, "A", "B", null);

        var company = Company.createCandidate(
                new CustomerId("synthetic-company"),
                TENANT, "Company", null);

        var branch = Branch.createCandidate(
                new BranchId("synthetic-branch"),
                TENANT, company, null,
                "Branch", null, null);

        var pc = current(person, CustomerStatus.ACTIVE);
        var cc = current(company, CustomerStatus.ACTIVE);

        var bc = new BranchUpdatePolicy.Current(
                branch, BranchStatus.ACTIVE,
                BranchRecordVersion.initial(branch));

        var parties = parties(pc, cc, Optional.empty());

        var branchParties =
                parties(pc, cc, Optional.of(bc));

        var relationship = Relationship.createCandidate(
                new RelationshipId("synthetic-rel"),
                person, company, ROLE.id(),
                DAY, DAY.plusDays(10));

        var historical = Relationship.createCandidate(
                new RelationshipId("private-conflict-id"),
                person, company, ROLE.id(),
                DAY.minusDays(5), DAY.plusDays(2));

        var conflict = RelationshipAssignment.Fact.of(
                historical, 2);

        // Assignment assessment.

        var clean = assignment(
                relationship, ASSIGN,
                COMPLETE_CANDIDATE, List.of());

        expect(clean.status() == ELIGIBLE_CANDIDATE);
        expect(clean.auditRequired());

        expect(clean.code().equals(
                "RELATIONSHIP_ASSIGNMENT_CANDIDATE"));

        var warning = assignment(
                relationship, ASSIGN,
                COMPLETE_CANDIDATE, List.of(conflict));

        expect(warning.status() == WARNING);
        expect(!warning.auditRequired());

        expect(warning.code().equals(
                "RELATIONSHIP_OVERLAP_WARNING"));

        expect(!warning.toString().contains(
                "private-conflict-id"));

        assignmentBlocked(
                assignment(relationship, NONE,
                        COMPLETE_CANDIDATE, List.of()),
                "RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED");

        assignmentBlocked(
                assignment(relationship, ASSIGN,
                        UNKNOWN, List.of()),
                "RELATIONSHIP_OVERLAP_EVIDENCE_MISSING");

        assignmentBlocked(
                assignment(relationship, ASSIGN,
                        COMPLETE_CANDIDATE,
                        List.of(conflict, conflict)),
                "RELATIONSHIP_EVIDENCE_DUPLICATE");

        assignmentBlocked(
                assignment(relationship, ASSIGN,
                        COMPLETE_CANDIDATE,
                        List.of(RelationshipAssignment.Fact.of(
                                relationship, 1))),
                "RELATIONSHIP_ID_ALREADY_EXISTS");

        var foreignTenant = new TenantId("foreign");

        var foreignPerson = Person.createCandidate(
                person.id(), foreignTenant, "A", "B", null);

        var foreignCompany = Company.createCandidate(
                company.id(), foreignTenant, "Company", null);

        var foreignRelationship = Relationship.createCandidate(
                new RelationshipId("foreign-rel"),
                foreignPerson, foreignCompany,
                ROLE.id(), DAY, null);

        assignmentBlocked(
                assignment(relationship, ASSIGN,
                        COMPLETE_CANDIDATE,
                        List.of(RelationshipAssignment.Fact.of(
                                foreignRelationship, 1))),
                "RELATIONSHIP_EVIDENCE_SCOPE_MISMATCH");

        assignmentBlocked(
                ASSIGNMENT.execute(new RelationshipAssignmentCommand(
                        relationship, ROLES,
                        new BusinessRoleCatalog.Ref(
                                "CEO", Optional.empty()),
                        ASSIGN, COMPLETE_CANDIDATE, List.of())),
                "RELATIONSHIP_ROLE_MISMATCH");

        var hidden = ROLES.withSystemHidden(
                TENANT, ROLE, true);

        assignmentBlocked(
                ASSIGNMENT.execute(new RelationshipAssignmentCommand(
                        relationship, hidden, ROLE,
                        ASSIGN, COMPLETE_CANDIDATE, List.of())),
                "RELATIONSHIP_ASSIGNMENT_ASSESSMENT_BLOCKED");

        // Defensive copy of evidence.

        var mutable = new ArrayList<
                RelationshipAssignment.Fact>();

        var assignmentCommand = new RelationshipAssignmentCommand(
                relationship, ROLES, ROLE,
                ASSIGN, COMPLETE_CANDIDATE, mutable);

        mutable.add(conflict);

        expect(assignmentCommand.evidence().isEmpty());
        expect(ASSIGNMENT.execute(assignmentCommand).status()
                == ELIGIBLE_CANDIDATE);

        try {
            assignmentCommand.evidence().add(conflict);
            throw new AssertionError("Mutable evidence.");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }

        // Half-open temporal interval.

        var adjacent = Relationship.createCandidate(
                new RelationshipId("adjacent"),
                person, company, ROLE.id(),
                DAY.minusDays(5), DAY);

        expect(assignment(relationship, ASSIGN,
                COMPLETE_CANDIDATE,
                List.of(RelationshipAssignment.Fact.of(adjacent, 1)))
                .status() == ELIGIBLE_CANDIDATE);

        // Creation candidate.

        var created = create(
                relationship, parties, ASSIGN,
                List.of(), null, null);

        eligible(created,
                RelationshipLifecycleCandidateResult.Operation.CREATE,
                1);

        lifecycleBlocked(
                create(relationship, parties, NONE,
                        List.of(), null, null),
                "RELATIONSHIP_ASSIGNMENT_AUTHORITY_REQUIRED", true);

        // Overlap requires review, exception authority and reason.

        var reviewed = RelationshipAssignment.assess(
                relationship, ROLES, ROLE,
                ASSIGN, COMPLETE_CANDIDATE,
                List.of(conflict));

        expect(reviewed.outcome() ==
                RelationshipAssignment.Outcome.WARNING);

        lifecycleBlocked(
                createWithCoverage(
                        relationship, parties, UNKNOWN),
                "RELATIONSHIP_OVERLAP_EVIDENCE_MISSING", false);

        var unreviewed = create(
                relationship, parties, ASSIGN,
                List.of(conflict), null, null);

        expect(unreviewed.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        expect(unreviewed.candidateDetails().isEmpty());

        var deniedException = create(
                relationship, parties, ASSIGN,
                List.of(conflict), reviewed, "private reason");

        expect(deniedException.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        var noReason = create(
                relationship, parties,
                ASSIGN_WITH_OVERLAP_EXCEPTION,
                List.of(conflict), reviewed, " ");

        expect(noReason.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        eligible(create(
                relationship, parties,
                ASSIGN_WITH_OVERLAP_EXCEPTION,
                List.of(conflict), reviewed, "private reason"),
                RelationshipLifecycleCandidateResult.Operation.CREATE,
                1);

        var changed = RelationshipAssignment.Fact.of(
                historical, 3);

        var staleReview = create(
                relationship, parties,
                ASSIGN_WITH_OVERLAP_EXCEPTION,
                List.of(changed), reviewed, "private reason");

        expect(staleReview.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        expect(staleReview.candidateDetails().isEmpty());

        expect(!staleReview.toString().contains("private reason"));
        expect(!staleReview.toString().contains("private-conflict-id"));

        // Party lifecycle and ownership.

        var inactivePerson = parties(
                current(person, CustomerStatus.INACTIVE),
                cc, Optional.empty());

        var inactiveCompany = parties(
                pc, current(company, CustomerStatus.INACTIVE),
                Optional.empty());

        lifecycleBlocked(
                create(relationship, inactivePerson, ASSIGN,
                        List.of(), null, null),
                "RELATIONSHIP_PERSON_NOT_ACTIVE", false);

        lifecycleBlocked(
                create(relationship, inactiveCompany, ASSIGN,
                        List.of(), null, null),
                "RELATIONSHIP_TARGET_NOT_ACTIVE", false);

        lifecycleBlocked(
                create(relationship, branchParties, ASSIGN,
                        List.of(), null, null),
                "RELATIONSHIP_TARGET_SCOPE_MISMATCH", false);

        var foreignParties = parties(
                current(foreignPerson, CustomerStatus.ACTIVE),
                cc, Optional.empty());

        lifecycleBlocked(
                create(relationship, foreignParties, ASSIGN,
                        List.of(), null, null),
                "RELATIONSHIP_PERSON_SCOPE_MISMATCH", false);

        // Branch relationship.

        var branchRelationship = Relationship.createCandidate(
                new RelationshipId("branch-rel"),
                person, branch, company,
                ROLE.id(), DAY, null);

        eligible(create(
                branchRelationship, branchParties,
                ASSIGN, List.of(), null, null),
                RelationshipLifecycleCandidateResult.Operation.CREATE,
                1);

        var inactiveBranch = parties(
                pc, cc,
                Optional.of(new BranchUpdatePolicy.Current(
                        branch, BranchStatus.INACTIVE,
                        bc.version())));

        lifecycleBlocked(
                create(branchRelationship, inactiveBranch,
                        ASSIGN, List.of(), null, null),
                "RELATIONSHIP_TARGET_NOT_ACTIVE", false);

        lifecycleBlocked(
                create(branchRelationship, parties,
                        ASSIGN, List.of(), null, null),
                "RELATIONSHIP_TARGET_SCOPE_MISMATCH", false);

        // Synthetic domain facts for ending and status tests.

        var domainCreated = RelationshipLifecycleIntegration.create(
                relationship, parties, ROLES, ROLE,
                ASSIGN, COMPLETE_CANDIDATE,
                List.of(), null, null, meta(1), "event1")
                .proposed().orElseThrow();

        var fact = domainCreated.fact();
        var history = domainCreated.history();

        // End candidate.

        eligible(end(fact, parties, history, 1,
                RelationshipLifecycle.Authority.END_RELATIONSHIP,
                "private reason"),
                RelationshipLifecycleCandidateResult.Operation.END,
                2);

        lifecycleBlocked(
                end(fact, parties, history, 2,
                        RelationshipLifecycle.Authority.END_RELATIONSHIP,
                        "reason"),
                "RELATIONSHIP_VERSION_STALE", false);

        lifecycleBlocked(
                end(fact, parties, history, 1,
                        RelationshipLifecycle.Authority.NONE,
                        "reason"),
                "RELATIONSHIP_END_AUTHORITY_REQUIRED", true);

        lifecycleBlocked(
                end(fact, parties, history, 1,
                        RelationshipLifecycle.Authority.END_RELATIONSHIP,
                        " "),
                "RELATIONSHIP_END_REASON_REQUIRED", false);

        // Ending remains possible with inactive parties.

        eligible(end(fact, inactiveCompany, history, 1,
                RelationshipLifecycle.Authority.END_RELATIONSHIP,
                "reason"),
                RelationshipLifecycleCandidateResult.Operation.END,
                2);

        var domainEnded = RelationshipLifecycleIntegration.end(
                fact, parties, history, ROLES,
                TENANT, 1,
                RelationshipLifecycle.Authority.END_RELATIONSHIP,
                "reason", meta(2), "event2")
                .proposed().orElseThrow();

        lifecycleBlocked(
                end(domainEnded.fact(), parties,
                        domainEnded.history(), 2,
                        RelationshipLifecycle.Authority.END_RELATIONSHIP,
                        "reason"),
                "RELATIONSHIP_ALREADY_ENDED", false);

        // Effective status.

        state(fact, DAY.minusDays(1), parties, SCHEDULED);
        state(fact, DAY, parties, EFFECTIVE);
        state(fact, DAY.plusDays(9), parties, EFFECTIVE);
        state(fact, DAY.plusDays(10), parties, EXPIRED);
        state(fact, DAY, inactivePerson, UNAVAILABLE);
        state(fact, DAY, inactiveCompany, UNAVAILABLE);
        state(domainEnded.fact(), DAY, parties, ENDED);

        var branchFact =
                RelationshipAssignment.Fact.of(branchRelationship, 1);

        state(branchFact, DAY, branchParties, EFFECTIVE);
        state(branchFact, DAY, inactiveBranch, UNAVAILABLE);

        state(branchFact, DAY.plusYears(100),
                branchParties, EFFECTIVE);

        var invalidScope = STATUS.execute(
                new RelationshipEffectiveStatusQuery(
                        fact, DAY, foreignParties));

        expect(invalidScope.status() ==
                RelationshipEffectiveStatusResult.Status.BLOCKED);

        expect(invalidScope.effectiveState().isEmpty());

        expect(invalidScope.code().equals(
                "RELATIONSHIP_EFFECTIVE_STATUS_BLOCKED"));

        // Immutability and synthetic-only behavior.

        expect(relationship.status() == Relationship.Status.ACTIVE);
        expect(fact.status() == Relationship.Status.ACTIVE);
        expect(fact.version() == 1);
        expect(history.entries().size() == 1);
        expect(domainEnded.history().entries().size() == 2);
        expect(domainEnded.fact().version() == 2);
        expect(domainEnded.fact().start().equals(fact.start()));
        expect(domainEnded.fact().end().equals(fact.end()));
        expect(pc.version().value() == 1);
        expect(cc.version().value() == 1);
        expect(bc.version().value() == 1);

        expect(domainCreated.event().eventType() ==
                RelationshipEventDraft.Type.RelationshipCreated);

        expect(domainEnded.event().eventType() ==
                RelationshipEventDraft.Type.RelationshipEnded);

        expect(domainCreated.event().aggregateVersion() == 1);
        expect(domainEnded.event().aggregateVersion() == 2);

        // Redacted contracts.

        expect(assignmentCommand.toString().equals(
                "RelationshipAssignmentCommand[redacted]"));

        expect(warning.toString().equals(
                "RelationshipAssignmentResult[redacted]"));

        expect(created.toString().equals(
                "RelationshipLifecycleCandidateResult[redacted]"));

        expect(created.candidateDetails().orElseThrow()
                .toString().equals("CandidateDetails[redacted]"));

        expect(new RelationshipEffectiveStatusQuery(
                fact, DAY, parties).toString().equals(
                "RelationshipEffectiveStatusQuery[redacted]"));

        // Result invariants and null handling.

        reject(() -> ASSIGNMENT.execute(null));
        reject(() -> CREATE.execute(null));
        reject(() -> END.execute(null));
        reject(() -> STATUS.execute(null));

        reject(() -> new RelationshipAssignmentResult(
                BLOCKED, " ", false));

        reject(() -> new RelationshipLifecycleCandidateResult(
                RelationshipLifecycleCandidateResult.Status.BLOCKED,
                "BLOCKED", false, created.candidateDetails()));

        reject(() -> new RelationshipLifecycleCandidateResult(
                RelationshipLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE,
                "CANDIDATE", true, Optional.empty()));

        reject(() -> new RelationshipLifecycleCandidateResult.CandidateDetails(
                RelationshipLifecycleCandidateResult.Operation.CREATE, 0));

        reject(() -> new RelationshipEffectiveStatusResult(
                RelationshipEffectiveStatusResult.Status.ASSESSED,
                "ASSESSED", Optional.empty()));

        reject(() -> new RelationshipEffectiveStatusResult(
                RelationshipEffectiveStatusResult.Status.BLOCKED,
                "BLOCKED", Optional.of(EFFECTIVE)));

        System.out.println(
                "AP-08.5: " + checks
                        + " relationship application checks PASS");
    }

    private static CustomerUpdatePolicy.Current current(
            Customer customer, CustomerStatus status) {

        return new CustomerUpdatePolicy.Current(
                IdentityRevision.snapshot(
                        customer, "r1", VerificationStatus.VERIFIED),
                status, CustomerRecordVersion.initial(customer));
    }

    private static RelationshipLifecycleIntegration.Parties parties(
            CustomerUpdatePolicy.Current person,
            CustomerUpdatePolicy.Current company,
            Optional<BranchUpdatePolicy.Current> branch) {

        return new RelationshipLifecycleIntegration.Parties(
                CustomerUpdatePolicy.lifecycleSnapshot(person),
                CustomerUpdatePolicy.lifecycleSnapshot(company),
                branch);
    }

    private static LifecycleHistory.Context meta(int i) {

        return new LifecycleHistory.Context(
                "h" + i, "actor",
                Instant.ofEpochSecond(i), "request" + i);
    }

    private static RelationshipAssignmentResult assignment(
            Relationship relationship,
            RelationshipAssignment.Authority authority,
            RelationshipAssignment.Coverage coverage,
            List<RelationshipAssignment.Fact> evidence) {

        return ASSIGNMENT.execute(
                new RelationshipAssignmentCommand(
                        relationship, ROLES, ROLE,
                        authority, coverage, evidence));
    }

    private static RelationshipLifecycleCandidateResult create(
            Relationship relationship,
            RelationshipLifecycleIntegration.Parties parties,
            RelationshipAssignment.Authority authority,
            List<RelationshipAssignment.Fact> evidence,
            RelationshipAssignment.Assessment reviewed,
            String reason) {

        return CREATE.execute(
                new CreateRelationshipCandidateCommand(
                        relationship, parties, ROLES, ROLE,
                        authority, COMPLETE_CANDIDATE, evidence,
                        reviewed, reason, meta(1), "event1"));
    }

    private static RelationshipLifecycleCandidateResult createWithCoverage(
            Relationship relationship,
            RelationshipLifecycleIntegration.Parties parties,
            RelationshipAssignment.Coverage coverage) {

        return CREATE.execute(
                new CreateRelationshipCandidateCommand(
                        relationship, parties, ROLES, ROLE,
                        ASSIGN, coverage, List.of(),
                        null, null, meta(1), "event1"));
    }

    private static RelationshipLifecycleCandidateResult end(
            RelationshipAssignment.Fact fact,
            RelationshipLifecycleIntegration.Parties parties,
            RelationshipHistory history,
            long version,
            RelationshipLifecycle.Authority authority,
            String reason) {

        return END.execute(
                new EndRelationshipCandidateCommand(
                        fact, parties, history, ROLES,
                        TENANT, version, authority,
                        reason, meta(2), "event2"));
    }

    private static void assignmentBlocked(
            RelationshipAssignmentResult result,
            String code) {

        expect(result.status() == BLOCKED);
        expect(result.code().equals(code));
        expect(!result.auditRequired());
    }

    private static void eligible(
            RelationshipLifecycleCandidateResult result,
            RelationshipLifecycleCandidateResult.Operation operation,
            long version) {

        expect(result.status() ==
                RelationshipLifecycleCandidateResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "RELATIONSHIP_LIFECYCLE_CANDIDATE"));

        expect(result.auditRequired());
        expect(result.candidateDetails().isPresent());

        var details = result.candidateDetails().orElseThrow();

        expect(details.operation() == operation);
        expect(details.proposedVersion() == version);
    }

    private static void lifecycleBlocked(
            RelationshipLifecycleCandidateResult result,
            String code, boolean audit) {

        expect(result.status() ==
                RelationshipLifecycleCandidateResult.Status.BLOCKED);

        if (!result.code().equals(code)) {
            throw new AssertionError(
                    "Expected " + code
                    + " but received " + result.code());
        }

        checks++;

        expect(result.auditRequired() == audit);
        expect(result.candidateDetails().isEmpty());
    }

    private static void state(
            RelationshipAssignment.Fact fact,
            LocalDate day,
            RelationshipLifecycleIntegration.Parties parties,
            RelationshipEffectiveStatusResult.EffectiveState expected) {

        var result = STATUS.execute(
                new RelationshipEffectiveStatusQuery(
                        fact, day, parties));

        expect(result.status() ==
                RelationshipEffectiveStatusResult.Status.ASSESSED);

        expect(result.code().equals(
                "RELATIONSHIP_EFFECTIVE_STATUS_ASSESSED"));

        expect(result.effectiveState().orElseThrow() == expected);
    }

    private static void reject(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException |
                 NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected input rejection.");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "Relationship application assertion "
                    + (checks + 1));
        }

        checks++;
    }
}