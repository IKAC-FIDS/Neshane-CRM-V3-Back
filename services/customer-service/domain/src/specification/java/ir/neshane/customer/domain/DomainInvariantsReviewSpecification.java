package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;
import static ir.neshane.customer.domain.ReadDisclosurePolicy.IdentityFields.*;

final class DomainInvariantsReviewSpecification {
    private static int checks;
    static void run() {
        var tenant=new TenantId("tenant");
        var foreign=new TenantId("foreign");
        var person=Person.createCandidate(new CustomerId("person"),tenant,"A","B",null);
        var company=Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var branch=Branch.createCandidate(new BranchId("branch"),tenant,company,null,"Branch",null,null);
        var reader=actor(tenant,CUSTOMER_READ);
        expect(ReadDisclosurePolicy.identityFields(reader,person)==MASK_REQUIRED);
        expect(ReadDisclosurePolicy.identityFields(actor(tenant,CUSTOMER_READ,IDENTITY_SENSITIVE_READ),person)==ReadDisclosurePolicy.IdentityFields.ELIGIBLE_CANDIDATE);
        expect(ReadDisclosurePolicy.identityFields(actor(tenant,IDENTITY_SENSITIVE_READ),person)==ReadDisclosurePolicy.IdentityFields.DENIED);
        expect(ReadDisclosurePolicy.identityFields(actor(foreign,CUSTOMER_READ,IDENTITY_SENSITIVE_READ),person)==ReadDisclosurePolicy.IdentityFields.DENIED);
        expect(ReadDisclosurePolicy.identityFields(null,person)==ReadDisclosurePolicy.IdentityFields.DENIED);
        expect(ReadDisclosurePolicy.identityFields(reader,null)==ReadDisclosurePolicy.IdentityFields.DENIED);
        for (var op:Operation.values()) {
            expect(eligible(ReadDisclosurePolicy.audit(actor(tenant,op),tenant))==(op==AUDIT_READ));
            expect(!eligible(ReadDisclosurePolicy.audit(actor(foreign,op),tenant)));
        }
        expect(!eligible(ReadDisclosurePolicy.audit(null,tenant)));
        expect(!eligible(ReadDisclosurePolicy.audit(actor(tenant,AUDIT_READ),null)));
        expect(AuditRequirementsPolicy.requirement(AUDIT_READ,Outcome.ELIGIBLE_CANDIDATE,false,false).auditRequired());
        var catalog=BusinessRoleCatalog.initial();
        var ref=new BusinessRoleCatalog.Ref("CEO",Optional.empty());
        var roleReader=actor(tenant,CATALOG_SELECTABLE_READ);
        expect(eligible(ReadDisclosurePolicy.selectableRole(roleReader,tenant,catalog,ref,RelationshipTarget.Kind.COMPANY)));
        expect(!eligible(ReadDisclosurePolicy.selectableRole(roleReader,tenant,catalog,ref,RelationshipTarget.Kind.BRANCH)));
        expect(!eligible(ReadDisclosurePolicy.selectableRole(roleReader,tenant,catalog.withSystemHidden(tenant,ref,true),ref,RelationshipTarget.Kind.COMPANY)));
        expect(!eligible(ReadDisclosurePolicy.selectableRole(actor(tenant,CATALOG_MANAGE),tenant,catalog,ref,RelationshipTarget.Kind.COMPANY)));
        expect(!eligible(ReadDisclosurePolicy.selectableRole(roleReader,foreign,catalog,ref,RelationshipTarget.Kind.COMPANY)));
        expect(!eligible(ReadDisclosurePolicy.selectableRole(roleReader,tenant,catalog,new BusinessRoleCatalog.Ref("private",Optional.of(foreign)),RelationshipTarget.Kind.COMPANY)));
        var day=LocalDate.of(2026,11,19);
        var fact=RelationshipAssignment.Fact.of(Relationship.createCandidate(new RelationshipId("rel"),person,branch,company,"CEO",day,null),1);
        var pc=current(person,CustomerStatus.INACTIVE);
        var cc=current(company,CustomerStatus.ARCHIVED);
        var bc=new BranchUpdatePolicy.Current(branch,BranchStatus.INACTIVE,BranchRecordVersion.initial(branch));
        var parties=new RelationshipLifecycleIntegration.Parties(pc,cc,Optional.of(bc));
        var relationshipReader=actor(tenant,RELATIONSHIP_READ);
        expect(eligible(ReadDisclosurePolicy.relationship(relationshipReader,fact,parties)));
        expect(!eligible(ReadDisclosurePolicy.relationship(reader,fact,parties)));
        expect(!eligible(ReadDisclosurePolicy.relationship(actor(foreign,RELATIONSHIP_READ),fact,parties)));
        expect(!eligible(ReadDisclosurePolicy.relationship(relationshipReader,fact,new RelationshipLifecycleIntegration.Parties(pc,cc,Optional.empty()))));
        var wrong=current(Company.createCandidate(new CustomerId("other"),tenant,"Other",null),CustomerStatus.ACTIVE);
        expect(!eligible(ReadDisclosurePolicy.relationship(relationshipReader,fact,new RelationshipLifecycleIntegration.Parties(pc,wrong,Optional.of(bc)))));
        var ended=new RelationshipAssignment.Fact(fact.id(),fact.tenant(),fact.person(),fact.target(),fact.roleCode(),day,Optional.empty(),Relationship.Status.ENDED,2);
        expect(eligible(ReadDisclosurePolicy.relationship(relationshipReader,ended,parties)));
        expect(fact.status()==Relationship.Status.ACTIVE && fact.version()==1 && bc.status()==BranchStatus.INACTIVE && cc.status()==CustomerStatus.ARCHIVED);
        expect(AuditRequirementsPolicy.logicalInformation().equals(Set.of(AuditRequirementsPolicy.Information.EVENT_ID,
            AuditRequirementsPolicy.Information.APPLICABLE_TENANT,AuditRequirementsPolicy.Information.ACTOR_ID_AND_TYPE,
            AuditRequirementsPolicy.Information.RESOURCE_TYPE_AND_AVAILABLE_ID,AuditRequirementsPolicy.Information.OPERATION,
            AuditRequirementsPolicy.Information.APPLICABLE_OLD_NEW_VALUES,AuditRequirementsPolicy.Information.REQUIRED_OR_PROVIDED_REASON,
            AuditRequirementsPolicy.Information.TIMESTAMP,AuditRequirementsPolicy.Information.OUTCOME,
            AuditRequirementsPolicy.Information.CORRELATION_ID,AuditRequirementsPolicy.Information.APPLICABLE_EVIDENCE_REFERENCE)));
        try { AuditRequirementsPolicy.logicalInformation().clear(); throw new AssertionError("Mutable audit obligations"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: "+checks+" isolated domain invariant review assertions");
    }
    private static boolean eligible(Assessment a) { return a.outcome()==Outcome.ELIGIBLE_CANDIDATE; }
    private static ActorContext actor(TenantId tenant,Operation... operations) {
        var grants=new HashSet<Grant>();
        for(var op:operations) grants.add(new Grant(tenant,op));
        return new ActorContext("actor",tenant,grants);
    }
    private static CustomerUpdatePolicy.Current current(Customer c,CustomerStatus s) {
        return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.NOT_VERIFIED),s,CustomerRecordVersion.initial(c));
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Domain invariant review assertion "+(checks+1)); checks++; }
}
