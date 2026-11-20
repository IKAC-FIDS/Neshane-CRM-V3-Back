package ir.neshane.customer.application.access;

import ir.neshane.customer.application.access.disclosure.identity.query.*;
import ir.neshane.customer.application.access.disclosure.identity.usecase.AssessIdentityDisclosure;
import ir.neshane.customer.application.access.disclosure.audit.query.AuditReadAccessQuery;
import ir.neshane.customer.application.access.disclosure.audit.usecase.AssessAuditReadAccess;
import ir.neshane.customer.application.access.disclosure.role.query.SelectableRoleAccessQuery;
import ir.neshane.customer.application.access.disclosure.role.usecase.AssessSelectableRoleAccess;
import ir.neshane.customer.application.access.disclosure.relationship.query.RelationshipReadAccessQuery;
import ir.neshane.customer.application.access.disclosure.relationship.usecase.AssessRelationshipReadAccess;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.customer.model.*;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.relationship.*;
import ir.neshane.customer.domain.verification.model.VerificationStatus;
import ir.neshane.customer.domain.shared.identifier.*;
import ir.neshane.customer.domain.shared.localization.LocalizedText;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import static ir.neshane.customer.domain.access.LogicalAccessPolicy.Operation.*;

public final class ReadDisclosureApplicationSpecification {

    private static int checks;

    private static final TenantId TENANT =
            new TenantId("protected-tenant");

    private static final TenantId FOREIGN =
            new TenantId("protected-foreign");

    private static final Person PERSON = Person.createCandidate(
            new CustomerId("protected-person"),
            TENANT, "A", "B", null);

    private static final Person OTHER = Person.createCandidate(
            new CustomerId("protected-other"),
            TENANT, "C", "D", null);

    private static final Person FOREIGN_PERSON = Person.createCandidate(
            PERSON.id(), FOREIGN, "A", "B", null);

    private static final Company COMPANY = Company.createCandidate(
            new CustomerId("protected-company"),
            TENANT, "Company", null);

    private static final Company OTHER_COMPANY = Company.createCandidate(
            new CustomerId("protected-other-company"),
            TENANT, "Other Company", null);

    private static final Company FOREIGN_COMPANY = Company.createCandidate(
            COMPANY.id(), FOREIGN, "Company", null);

    private static final Branch BRANCH = Branch.createCandidate(
            new BranchId("protected-branch"),
            TENANT, COMPANY, null, "Branch", null, null);

    private static final Branch OTHER_BRANCH = Branch.createCandidate(
            new BranchId("protected-other-branch"),
            TENANT, COMPANY, null, "Other Branch", null, null);

    private static final AssessIdentityDisclosure IDENTITY =
            new AssessIdentityDisclosure();

    private static final AssessAuditReadAccess AUDIT =
            new AssessAuditReadAccess();

    private static final AssessSelectableRoleAccess ROLE =
            new AssessSelectableRoleAccess();

    private static final AssessRelationshipReadAccess RELATIONSHIP =
            new AssessRelationshipReadAccess();

    private ReadDisclosureApplicationSpecification() {}

    public static void run() {

        checks = 0;

        identityDisclosure();
        auditRead();
        selectableRoles();
        relationshipRead();
        securityAndInvariants();

        System.out.println(
                "AP-12.7.4: " + checks
                + " read disclosure application checks PASS");
    }

    private static LogicalAccessPolicy.ActorContext actor(
            TenantId tenant,
            LogicalAccessPolicy.Operation... operations) {

        Set<LogicalAccessPolicy.Grant> grants =
                new HashSet<>();

        for (var operation : operations) {

            grants.add(new LogicalAccessPolicy.Grant(
                    tenant, operation));
        }

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                grants);
    }

    private static LogicalAccessPolicy.ActorContext allGrants(
            TenantId tenant) {

        var grants = Arrays.stream(
                        LogicalAccessPolicy.Operation.values())
                .map(operation ->
                        new LogicalAccessPolicy.Grant(
                                tenant, operation))
                .collect(Collectors.toUnmodifiableSet());

        return new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                tenant,
                grants);
    }

    private static IdentityDisclosureResult identity(
            LogicalAccessPolicy.ActorContext actor,
            Customer customer) {

        return IDENTITY.execute(
                new IdentityDisclosureQuery(actor, customer));
    }

    private static AccessAssessmentResult audit(
            LogicalAccessPolicy.ActorContext actor,
            TenantId tenant) {

        return AUDIT.execute(
                new AuditReadAccessQuery(actor, tenant));
    }

    private static AccessAssessmentResult role(
            LogicalAccessPolicy.ActorContext actor,
            TenantId tenant,
            BusinessRoleCatalog catalog,
            BusinessRoleCatalog.Ref reference,
            RelationshipTarget.Kind kind) {

        return ROLE.execute(
                new SelectableRoleAccessQuery(
                        actor, tenant, catalog,
                        reference, kind));
    }

    private static AccessAssessmentResult relationship(
            LogicalAccessPolicy.ActorContext actor,
            RelationshipAssignment.Fact fact,
            RelationshipLifecycleIntegration.Parties parties) {

        return RELATIONSHIP.execute(
                new RelationshipReadAccessQuery(
                        actor, fact, parties));
    }

    private static void identityDisclosure() {

        var denied = IdentityDisclosureResult.Status.DENIED;
        var masked = IdentityDisclosureResult.Status.MASK_REQUIRED;
        var eligible = IdentityDisclosureResult.Status.ELIGIBLE_CANDIDATE;

        for (var customer : new Customer[] {
                PERSON, COMPANY }) {

            expectDisclosure(
                    identity(actor(TENANT), customer),
                    denied);

            expectDisclosure(
                    identity(actor(TENANT, CUSTOMER_READ), customer),
                    masked);

            expectDisclosure(
                    identity(actor(TENANT, IDENTITY_SENSITIVE_READ),
                            customer),
                    denied);

            expectDisclosure(
                    identity(actor(TENANT,
                            CUSTOMER_READ,
                            IDENTITY_SENSITIVE_READ), customer),
                    eligible);

            expectDisclosure(
                    identity(allGrants(TENANT), customer),
                    eligible);

            expectDisclosure(
                    identity(allGrants(FOREIGN), customer),
                    denied);

            expectDisclosure(
                    identity(null, customer),
                    denied);

            for (var operation :
                    LogicalAccessPolicy.Operation.values()) {

                var result = identity(
                        actor(TENANT, operation),
                        customer);

                expectDisclosure(
                        result,
                        operation == CUSTOMER_READ
                                ? masked : denied);
            }
        }

        expectDisclosure(
                identity(allGrants(TENANT), FOREIGN_PERSON),
                denied);

        expectDisclosure(
                identity(allGrants(TENANT), null),
                denied);

        var mixed = new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                TENANT,
                Set.of(
                        new LogicalAccessPolicy.Grant(
                                TENANT, CUSTOMER_READ),
                        new LogicalAccessPolicy.Grant(
                                FOREIGN, IDENTITY_SENSITIVE_READ)));

        expectDisclosure(
                identity(mixed, PERSON),
                masked);

        var reverse = new LogicalAccessPolicy.ActorContext(
                "protected-actor",
                TENANT,
                Set.of(
                        new LogicalAccessPolicy.Grant(
                                FOREIGN, CUSTOMER_READ),
                        new LogicalAccessPolicy.Grant(
                                TENANT, IDENTITY_SENSITIVE_READ)));

        expectDisclosure(
                identity(reverse, PERSON),
                denied);
    }

    private static void auditRead() {

        for (var operation :
                LogicalAccessPolicy.Operation.values()) {

            var result = audit(
                    actor(TENANT, operation),
                    TENANT);

            if (operation == AUDIT_READ) {

                expectEligible(result);

            } else {

                expectDenied(
                        result,
                        "ACCESS_OPERATION_NOT_GRANTED");
            }
        }

        expectEligible(
                audit(allGrants(TENANT), TENANT));

        expectDenied(
                audit(actor(TENANT, HISTORY_READ), TENANT),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                audit(actor(TENANT, CATALOG_MANAGE), TENANT),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                audit(actor(TENANT, AUDIT_READ), FOREIGN),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                audit(actor(FOREIGN, AUDIT_READ), TENANT),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                audit(null, TENANT),
                "ACCESS_ACTOR_CONTEXT_REQUIRED");

        expectDenied(
                audit(allGrants(TENANT), null),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                audit(actor(TENANT), TENANT),
                "ACCESS_OPERATION_NOT_GRANTED");
    }

    private static BusinessRoleDefinition definition(
            TenantId tenant,
            String id,
            String code,
            BusinessRoleDefinition.Scope scope,
            BusinessRoleDefinition.Status status) {

        return new BusinessRoleDefinition(
                id,
                Optional.of(tenant),
                code,
                new LocalizedText(Map.of(
                        "fa", "\u0646\u0642\u0634",
                        "en", "Role")),
                new LocalizedText(Map.of()),
                scope,
                status);
    }

    private static void selectableRoles() {

        var active = definition(
                TENANT,
                "protected-role",
                "CUSTOM",
                BusinessRoleDefinition.Scope.BOTH,
                BusinessRoleDefinition.Status.ACTIVE);

        var catalog = BusinessRoleCatalog.initial()
                .addCandidate(TENANT, active);

        var reference = new BusinessRoleCatalog.Ref(
                active.id(),
                active.tenantId());

        var kind = RelationshipTarget.Kind.COMPANY;

        var reader = actor(
                TENANT, CATALOG_SELECTABLE_READ);

        expectEligible(
                role(reader, TENANT, catalog, reference, kind));

        expectEligible(
                role(reader, TENANT, catalog,
                        reference, RelationshipTarget.Kind.BRANCH));

        for (var operation :
                LogicalAccessPolicy.Operation.values()) {

            var result = role(
                    actor(TENANT, operation),
                    TENANT,
                    catalog,
                    reference,
                    kind);

            if (operation == CATALOG_SELECTABLE_READ) {

                expectEligible(result);

            } else {

                expectDenied(
                        result,
                        "ACCESS_OPERATION_NOT_GRANTED");
            }
        }

        expectDenied(
                role(actor(TENANT, CATALOG_MANAGE),
                        TENANT, catalog, reference, kind),
                "ACCESS_OPERATION_NOT_GRANTED");

        expectDenied(
                role(actor(FOREIGN, CATALOG_SELECTABLE_READ),
                        TENANT, catalog, reference, kind),
                "ACCESS_TENANT_MISMATCH");

        var unknown = new BusinessRoleCatalog.Ref(
                "unknown-role", Optional.of(TENANT));

        expectDenied(
                role(reader, TENANT, catalog, unknown, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        var inactive = definition(
                TENANT,
                active.id(),
                active.code(),
                active.scope(),
                BusinessRoleDefinition.Status.INACTIVE);

        var revised = catalog.reviseCandidate(
                TENANT, inactive);

        expectDenied(
                role(reader, TENANT, revised, reference, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectEligible(
                role(reader, TENANT, catalog, reference, kind));

        var foreignReader = actor(
                FOREIGN, CATALOG_SELECTABLE_READ);

        expectDenied(
                role(foreignReader, FOREIGN,
                        catalog, reference, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                role(null, TENANT, catalog, reference, kind),
                "ACCESS_ACTOR_CONTEXT_REQUIRED");

        expectDenied(
                role(reader, null, catalog, reference, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                role(reader, TENANT, null, reference, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                role(reader, TENANT, catalog, null, kind),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                role(reader, TENANT, catalog, reference, null),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expect(active.status()
                == BusinessRoleDefinition.Status.ACTIVE);
    }

    private static CustomerUpdatePolicy.Current current(
            Customer customer,
            CustomerStatus status) {

        return new CustomerUpdatePolicy.Current(
                IdentityRevision.snapshot(
                        customer,
                        "protected-revision",
                        VerificationStatus.VERIFIED),
                status,
                CustomerRecordVersion.initial(customer));
    }

    private static RelationshipLifecycleIntegration.Parties parties(
            Person person,
            Company company,
            Branch branch,
            CustomerStatus status) {

        var personSnapshot = CustomerUpdatePolicy.lifecycleSnapshot(
                current(person, status));

        var companySnapshot = CustomerUpdatePolicy.lifecycleSnapshot(
                current(company, status));

        var branchSnapshot = Optional.ofNullable(branch)
                .map(value -> new BranchUpdatePolicy.Current(
                        value,
                        BranchStatus.ACTIVE,
                        BranchRecordVersion.initial(value)));

        return new RelationshipLifecycleIntegration.Parties(
                personSnapshot,
                companySnapshot,
                branchSnapshot);
    }

    private static void relationshipRead() {

        var day = LocalDate.of(2026, 11, 20);

        var companyRelationship = Relationship.createCandidate(
                new RelationshipId("protected-company-rel"),
                PERSON,
                COMPANY,
                "CUSTOM",
                day,
                null);

        var branchRelationship = Relationship.createCandidate(
                new RelationshipId("protected-branch-rel"),
                PERSON,
                BRANCH,
                COMPANY,
                "CUSTOM",
                day,
                null);

        var companyFact = RelationshipAssignment.Fact.of(
                companyRelationship, 1);

        var branchFact = RelationshipAssignment.Fact.of(
                branchRelationship, 1);

        var companyParties = parties(
                PERSON, COMPANY, null, CustomerStatus.ACTIVE);

        var branchParties = parties(
                PERSON, COMPANY, BRANCH, CustomerStatus.ACTIVE);

        var reader = actor(TENANT, RELATIONSHIP_READ);

        expectEligible(
                relationship(reader, companyFact, companyParties));

        expectEligible(
                relationship(reader, branchFact, branchParties));

        for (var operation :
                LogicalAccessPolicy.Operation.values()) {

            var actor = actor(TENANT, operation);

            var companyResult = relationship(
                    actor, companyFact, companyParties);

            var branchResult = relationship(
                    actor, branchFact, branchParties);

            if (operation == RELATIONSHIP_READ) {

                expectEligible(companyResult);
                expectEligible(branchResult);

            } else {

                expectDenied(
                        companyResult,
                        "ACCESS_OPERATION_NOT_GRANTED");

                expectDenied(
                        branchResult,
                        "ACCESS_OPERATION_NOT_GRANTED");
            }
        }

        expectDenied(
                relationship(reader, companyFact, branchParties),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(reader, branchFact, companyParties),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(
                        reader,
                        companyFact,
                        parties(OTHER, COMPANY,
                                null, CustomerStatus.ACTIVE)),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(
                        reader,
                        companyFact,
                        parties(PERSON, OTHER_COMPANY,
                                null, CustomerStatus.ACTIVE)),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(
                        reader,
                        branchFact,
                        parties(PERSON, COMPANY,
                                OTHER_BRANCH, CustomerStatus.ACTIVE)),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(
                        reader,
                        companyFact,
                        parties(FOREIGN_PERSON, FOREIGN_COMPANY,
                                null, CustomerStatus.ACTIVE)),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(
                        actor(FOREIGN, RELATIONSHIP_READ),
                        companyFact,
                        companyParties),
                "ACCESS_TENANT_MISMATCH");

        expectDenied(
                relationship(null, companyFact, companyParties),
                "ACCESS_ACTOR_CONTEXT_REQUIRED");

        expectDenied(
                relationship(reader, null, companyParties),
                "ACCESS_RESOURCE_UNAVAILABLE");

        expectDenied(
                relationship(reader, companyFact, null),
                "ACCESS_RESOURCE_UNAVAILABLE");

        // Historical relationship scope remains readable.
        // Lifecycle eligibility is independent of read access.

        var ended = new RelationshipAssignment.Fact(
                companyFact.id(),
                companyFact.tenant(),
                companyFact.person(),
                companyFact.target(),
                companyFact.roleCode(),
                companyFact.start(),
                companyFact.end(),
                Relationship.Status.ENDED,
                2);

        expectEligible(
                relationship(reader, ended, companyParties));

        var inactiveParties = parties(
                PERSON, COMPANY,
                null, CustomerStatus.INACTIVE);

        expectEligible(
                relationship(reader, ended, inactiveParties));

        expect(companyFact.version() == 1);

        expect(ended.version() == 2);

        expect(companyFact.status()
                == Relationship.Status.ACTIVE);

        expect(ended.status()
                == Relationship.Status.ENDED);
    }

    private static void securityAndInvariants() {

        var full = allGrants(TENANT);

        var identityQuery =
                new IdentityDisclosureQuery(full, PERSON);

        var auditQuery =
                new AuditReadAccessQuery(full, TENANT);

        var identityResult =
                IDENTITY.execute(identityQuery);

        var auditResult =
                AUDIT.execute(auditQuery);

        expect(identityResult.equals(
                IDENTITY.execute(identityQuery)));

        expect(auditResult.equals(
                AUDIT.execute(auditQuery)));

        var roleQuery = new SelectableRoleAccessQuery(
                full, TENANT, null, null, null);

        var relationshipQuery = new RelationshipReadAccessQuery(
                full, null, null);

        for (var value : new Object[] {
                identityQuery,
                auditQuery,
                roleQuery,
                relationshipQuery,
                identityResult,
                auditResult,
                ROLE.execute(roleQuery),
                RELATIONSHIP.execute(relationshipQuery) }) {

            expect(value.toString().contains("redacted"));

            expect(!value.toString().contains("protected-"));
        }

        expectIllegal(() -> new IdentityDisclosureResult(
                IdentityDisclosureResult.Status.DENIED,
                "IDENTITY_DISCLOSURE_ELIGIBLE_CANDIDATE"));

        expectIllegal(() -> new IdentityDisclosureResult(
                IdentityDisclosureResult.Status.MASK_REQUIRED,
                "IDENTITY_DISCLOSURE_DENIED"));

        expectIllegal(() -> new IdentityDisclosureResult(
                IdentityDisclosureResult.Status.ELIGIBLE_CANDIDATE,
                "IDENTITY_DISCLOSURE_MASK_REQUIRED"));

        expectNull(() -> new IdentityDisclosureResult(
                null, "IDENTITY_DISCLOSURE_DENIED"));

        expectNull(() -> new IdentityDisclosureResult(
                IdentityDisclosureResult.Status.DENIED, null));

        expectNull(() -> IDENTITY.execute(null));

        expectNull(() -> AUDIT.execute(null));

        expectNull(() -> ROLE.execute(null));

        expectNull(() -> RELATIONSHIP.execute(null));
    }

    private static void expectDisclosure(
            IdentityDisclosureResult result,
            IdentityDisclosureResult.Status status) {

        expect(result.status() == status);

        var expected = switch (status) {
            case DENIED ->
                "IDENTITY_DISCLOSURE_DENIED";
            case MASK_REQUIRED ->
                "IDENTITY_DISCLOSURE_MASK_REQUIRED";
            case ELIGIBLE_CANDIDATE ->
                "IDENTITY_DISCLOSURE_ELIGIBLE_CANDIDATE";
        };

        expect(result.code().equals(expected));
    }

    private static void expectEligible(
            AccessAssessmentResult result) {

        expect(result.status()
                == AccessAssessmentResult.Status.ELIGIBLE_CANDIDATE);

        expect(result.code().equals(
                "ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));
    }

    private static void expectDenied(
            AccessAssessmentResult result,
            String code) {

        expect(result.status()
                == AccessAssessmentResult.Status.DENIED);

        expect(result.code().equals(code));
    }

    private static void expectNull(Runnable action) {

        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected null rejection");
    }

    private static void expectIllegal(Runnable action) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }

        throw new AssertionError("Expected invariant rejection");
    }

    private static void expect(boolean value) {

        if (!value) {
            throw new AssertionError(
                    "AP-12.7.4 assertion " + (checks + 1));
        }

        checks++;
    }
}
