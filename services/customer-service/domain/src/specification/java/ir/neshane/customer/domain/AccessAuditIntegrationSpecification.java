package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;
import static ir.neshane.customer.domain.AuditRequirementsPolicy.Reason.*;

/** Composes synthetic access and audit facts without enforcing or executing an operation. */
final class AccessAuditIntegrationSpecification {
    private static int checks;
    private static final Instant AT=Instant.parse("2030-01-01T00:00:00Z");
    private record Scenario(Operation operation,Function<ActorContext,Assessment> action,boolean sensitive) {}
    static void run() {
        var tenant=new TenantId("tenant");
        var foreign=new TenantId("foreign");
        var person=Person.createCandidate(new CustomerId("person"),tenant,"Private","Name",null);
        var company=Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var branch=Branch.createCandidate(new BranchId("branch"),tenant,company,null,"Branch",null,null);
        var outsider=Person.createCandidate(person.id(),foreign,"Other","Name",null);
        var wrong=Person.createCandidate(new CustomerId("wrong"),tenant,"Other","Name",null);
        var owner=person.identityInfo().owner();
        var type=new DocumentTypeDefinition(tenant,"type","TYPE",new LocalizedText(Map.of("fa","مدرک")),new LocalizedText(Map.of()),DocumentTypeDefinition.Applicability.BOTH,DocumentTypeDefinition.RequirementStage.OPTIONAL,DocumentTypeDefinition.FileMultiplicity.SINGLE,Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
        var catalog=DocumentTypeCatalog.empty(tenant,1000).addCandidate(type,DocumentTypeCatalog.Authority.MANAGE);
        var document=CustomerDocument.createCandidate(person,new CustomerDocumentId("doc"),type.id(),List.of(new DocumentFileReference(tenant,"private-file","application/pdf",100)),Optional.of("private-number"),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),"actor",AT);
        var documents=DocumentHistory.upload(document,catalog,DocumentVersionHistory.Authority.UPLOAD,null,new LifecycleHistory.Context("d1","actor",AT,"corr"));
        var item=new VerificationEvidence("e",owner,"rev",1,VerificationEvidence.Method.MANUAL,"source","issuer","private-proof","actor",AT,AT,Optional.empty(),VerificationEvidence.Status.PROVIDED);
        var evidence=new VerificationEvidenceSet(owner,"rev",List.of(item));
        var verification=VerificationHistory.empty(IdentityRevision.snapshot(person,"rev",VerificationStatus.NOT_VERIFIED));
        var duplicate=DuplicateDetection.Request.of(person,DuplicateDetection.Operation.UPDATE,"private-request");
        var reader=actor("reader",tenant,CUSTOMER_READ,BRANCH_READ,DOCUMENT_READ);
        var verifier=actor("verifier",tenant,IDENTITY_SENSITIVE_READ,DOCUMENT_CONTENT_READ,VERIFICATION_EVIDENCE_READ,VERIFY_MANUAL);
        var editor=actor("editor",tenant,CUSTOMER_UPDATE,BRANCH_UPDATE,IDENTITY_CORRECT,DOCUMENT_INVALIDATE,DUPLICATE_EXCEPTION,VERIFICATION_REVOKE,CUSTOMER_LIFECYCLE);
        var allForeign=actor("foreign",foreign,Operation.values());
        var scenarios=List.of(
            new Scenario(CUSTOMER_READ,a->CustomerBranchAccessPolicy.assessCustomer(a,person,CUSTOMER_READ),false),
            new Scenario(BRANCH_READ,a->CustomerBranchAccessPolicy.assessBranch(a,branch,company,BRANCH_READ),false),
            new Scenario(CUSTOMER_UPDATE,a->CustomerBranchAccessPolicy.assessCustomer(a,person,CUSTOMER_UPDATE),true),
            new Scenario(BRANCH_UPDATE,a->CustomerBranchAccessPolicy.assessBranch(a,branch,company,BRANCH_UPDATE),true),
            new Scenario(DOCUMENT_READ,a->SensitiveAccessPolicy.assessDocument(a,person,document,DOCUMENT_READ),false),
            new Scenario(DOCUMENT_CONTENT_READ,a->SensitiveAccessPolicy.assessDocument(a,person,document,DOCUMENT_CONTENT_READ),true),
            new Scenario(IDENTITY_SENSITIVE_READ,a->SensitiveAccessPolicy.assessIdentity(a,person,IDENTITY_SENSITIVE_READ),true),
            new Scenario(IDENTITY_CORRECT,a->SensitiveAccessPolicy.assessIdentity(a,person,IDENTITY_CORRECT),true),
            new Scenario(VERIFICATION_EVIDENCE_READ,a->SensitiveAccessPolicy.assessEvidence(a,person,evidence,VERIFICATION_EVIDENCE_READ),true),
            new Scenario(VERIFY_MANUAL,a->SensitiveAccessPolicy.assessEvidence(a,person,evidence,VERIFY_MANUAL),true),
            new Scenario(VERIFICATION_REVOKE,a->SensitiveAccessPolicy.assessEvidence(a,person,evidence,VERIFICATION_REVOKE),true),
            new Scenario(DOCUMENT_INVALIDATE,a->SensitiveAccessPolicy.assessDocument(a,person,document,DOCUMENT_INVALIDATE),true),
            new Scenario(DUPLICATE_EXCEPTION,a->SensitiveAccessPolicy.assessDuplicateException(a,person,duplicate),true));
        for (var actor : List.of(reader,verifier,editor,allForeign)) {
            for (var scenario : scenarios) {
                var access=scenario.action().apply(actor);
                var expected=actor.tenant().equals(tenant)&&actor.grants().contains(new Grant(tenant,scenario.operation()));
                expect(access.outcome()==(expected?ELIGIBLE_CANDIDATE:DENIED));
                var audit=AuditRequirementsPolicy.requirement(scenario.operation(),access.outcome(),false,false);
                expect(audit.auditRequired()==(!expected||scenario.sensitive()));
                var metadata=AuditRequirementsPolicy.metadata(actor,new Request(tenant,scenario.operation()),access,AT,"corr");
                expect(metadata.operation()==scenario.operation()); expect(metadata.outcome()==access.outcome());
                expect(metadata.targetTenant().equals(tenant)); expect(metadata.actorTenant().orElseThrow().equals(actor.tenant()));
                expect(!metadata.toString().contains("private")); expect(!access.toString().contains("private"));
            }
        }
        var wrongDoc=SensitiveAccessPolicy.assessDocument(verifier,wrong,document,DOCUMENT_CONTENT_READ);
        expect(wrongDoc.code().equals("ACCESS_DOCUMENT_SCOPE_MISMATCH"));
        expect(AuditRequirementsPolicy.requirement(DOCUMENT_CONTENT_READ,wrongDoc.outcome(),false,false).auditRequired());
        expect(SensitiveAccessPolicy.assessEvidence(verifier,outsider,evidence,VERIFY_MANUAL).outcome()==DENIED);
        var missing=CustomerBranchAccessPolicy.assessCustomer(null,person,CUSTOMER_READ);
        expect(AuditRequirementsPolicy.requirement(CUSTOMER_READ,missing.outcome(),false,false).auditRequired());
        expect(AuditRequirementsPolicy.metadata(null,new Request(tenant,CUSTOMER_READ),missing,AT,"corr").actorId().isEmpty());
        // Eligibility does not supply a reason, satisfy concurrency, or perform a domain change.
        var invalidate=SensitiveAccessPolicy.assessDocument(editor,person,document,DOCUMENT_INVALIDATE);
        expect(invalidate.outcome()==ELIGIBLE_CANDIDATE);
        expect(AuditRequirementsPolicy.requirement(DOCUMENT_INVALIDATE,invalidate.outcome(),false,false).reason()==REQUIRED);
        var invalidation=new DocumentVersionHistory.Invalidation(owner,document.id(),1,"editor",null,AT,AT,"corr");
        expect(documents.versions().invalidate(invalidation,DocumentVersionHistory.Authority.INVALIDATE).code().equals("DOCUMENT_INVALIDATION_REASON_REQUIRED"));
        var stale=new DocumentVersionHistory.Invalidation(owner,document.id(),2,"editor","reason",AT,AT,"corr");
        expect(documents.versions().invalidate(stale,DocumentVersionHistory.Authority.INVALIDATE).code().equals("DOCUMENT_VERSION_STALE"));
        var update=CustomerBranchAccessPolicy.assessCustomer(editor,person,CUSTOMER_UPDATE);
        var changed=Person.createCandidate(person.id(),tenant,"Changed","Name",null);
        var fields=CriticalFieldChangeAssessment.assess(person,changed,CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,null);
        expect(fields.reasonRequired()); expect(fields.outcome()==CriticalFieldChangeAssessment.Outcome.BLOCKED);
        expect(AuditRequirementsPolicy.requirement(CUSTOMER_UPDATE,update.outcome(),fields.auditRequired(),fields.reasonRequired()).reason()==REQUIRED);
        expect(CustomerBranchAccessPolicy.assessCustomer(editor,person,CUSTOMER_LIFECYCLE).outcome()==ELIGIBLE_CANDIDATE);
        expect(CustomerLifecycle.assess(person.status(),CustomerLifecycle.Transition.ARCHIVE,"reason").outcome()==CustomerLifecycle.Outcome.BLOCKED);
        expect(person.status()==CustomerStatus.ACTIVE); expect(branch.status()==BranchStatus.ACTIVE);
        expect(CustomerRecordVersion.initial(person).value()==1); expect(BranchRecordVersion.initial(branch).value()==1);
        expect(documents.entries().size()==1); expect(documents.current().version()==1);
        expect(documents.current().state()==CustomerDocument.State.PROVIDED); expect(documents.versions().invalidations().isEmpty());
        expect(verification.entries().isEmpty()); expect(verification.current().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(item.evidenceVersion()==1); expect(person.verificationStatus()==VerificationStatus.NOT_VERIFIED);
        System.out.println("PASS: "+checks+" integrated isolated access and audit assertions");
    }
    private static ActorContext actor(String id,TenantId tenant,Operation... ops) {
        return new ActorContext(id,tenant,java.util.Arrays.stream(ops).map(op->new Grant(tenant,op)).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Access audit integration assertion "+(checks+1)); checks++; }
}
