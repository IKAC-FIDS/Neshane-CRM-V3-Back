package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;
import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.*;
import java.util.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;

/** One connected synthetic scenario; no application execution or durable acceptance. */
final class FinalDomainIntegrationSpecification {
    private static final TenantId T=new TenantId("final-tenant");
    private static final LocalDate DAY=LocalDate.of(2026,11,19);
    private static final ContactTypeCatalog CONTACTS=ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        ContactTypeRef.system("MOBILE"),"MOBILE",new LocalizedText(Map.of("en","Mobile")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.PERSON),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final IdentifierTypeCatalog IDS=IdentifierTypeCatalog.empty().withSystemCandidate(new IdentifierTypeDefinition(
        "passport","PASSPORT",SystemIdentifierVocabulary.PASSPORT.title(),Set.of(IdentityOwner.Kind.PERSON),Set.of(),Optional.of("p"),true,Optional.empty(),IdentifierTypeDefinition.Status.ACTIVE));
    private static final ValidationProfile PROFILE=new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    private static int checks;
    static void run() {
        var base=Person.createCandidate(new CustomerId("person"),T,"A","B",null);
        var phone=new ContactPoint("phone",base.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123456",Optional.empty()));
        var contacts=CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(base.contactInfo(),phone),phone.id());
        var person=person(base,contacts,"abc");
        var company=Company.createCandidate(new CustomerId("company"),T,"Company",null);
        var type=new DocumentTypeDefinition(T,"identity","IDENTITY",new LocalizedText(Map.of("fa","مدرک هویت","en","Identity")),new LocalizedText(Map.of()),
            DocumentTypeDefinition.Applicability.PERSON,DocumentTypeDefinition.RequirementStage.REGISTRATION,DocumentTypeDefinition.FileMultiplicity.SINGLE,
            Set.of("application/pdf"),100,true,DocumentTypeDefinition.Status.ACTIVE,0);
        var policy=DocumentRequirementPolicy.initial(DocumentTypeCatalog.empty(T,1000).addCandidate(type,DocumentTypeCatalog.Authority.MANAGE),"policy");
        var doc=CustomerDocument.createCandidate(person,new CustomerDocumentId("doc"),type.id(),List.of(new DocumentFileReference(T,"private-file","application/pdf",50)),
            Optional.empty(),Optional.empty(),Optional.of(DAY.plusDays(30)),Optional.empty(),Optional.empty(),"operator",Instant.EPOCH);
        var documents=DocumentHistory.upload(doc,policy.catalog(),DocumentVersionHistory.Authority.UPLOAD,null,meta("doc",1));
        var docs=List.of(documents.versions());
        var request=DuplicateDetection.Request.of(person,DuplicateDetection.Operation.CREATE,"request");
        var conflict=new DuplicateDetection.Comparison(request,T,new CustomerId("other"),IdentityOwner.Kind.PERSON,Set.of(DuplicateDetection.Signal.VERIFIED_VALID_IDENTIFIER));
        expect(register(person,policy,docs,List.of(conflict)).proposed().isEmpty());
        expect(register(person,policy,List.of(),List.of()).proposed().isEmpty());
        var ph=CustomerChangeHistory.start(register(person,policy,docs,List.of()).proposed().orElseThrow(),"r1",meta("person",2));
        var ch=CustomerChangeHistory.start(register(company,policy,List.of(),List.of()).proposed().orElseThrow(),"c1",meta("company",2));
        expect(ph.current().version().value()==1 && ch.current().version().value()==1);
        expect(ph.current().identity().candidate().contactInfo().primary(ContactGroup.TELEPHONE).orElseThrow().equals("phone"));
        var branch=Branch.createCandidate(new BranchId("branch"),T,company,null,"Branch",null,null);
        var br=BranchRegistration.assess(branch,ch.current(),CompanyBranches.empty(company),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(T,BranchRegistration.Authority.REGISTER),BranchTypeCatalog.initial(),CONTACTS,Map.of());
        var bh=BranchChangeHistory.start(br.proposed().orElseThrow(),meta("branch",3));
        var roles=BusinessRoleCatalog.initial();
        var role=new BusinessRoleCatalog.Ref("BRANCH_MANAGER",Optional.empty());
        var relationship=Relationship.createCandidate(new RelationshipId("rel"),person,branch,company,"BRANCH_MANAGER",DAY,DAY.plusDays(10));
        var parties=new RelationshipLifecycleIntegration.Parties(ph.current(),ch.current(),Optional.of(bh.current()));
        var rel=RelationshipLifecycleIntegration.create(relationship,parties,roles,role,RelationshipAssignment.Authority.ASSIGN,
            RelationshipAssignment.Coverage.COMPLETE_CANDIDATE,List.of(),null,null,meta("rel",4),"rel-created").proposed().orElseThrow();
        expect(rel.fact().version()==1 && rel.history().entries().size()==1);
        expect(RelationshipLifecycleIntegration.effectiveStatus(rel.fact(),DAY,parties)==RelationshipLifecycle.EffectiveStatus.EFFECTIVE);
        var evidence=evidence(person,"r1",documents,5);
        var verified=verify(VerificationHistory.empty(ph.current().identity()),person,evidence,docs,ManualVerification.Process.ORDINARY,Set.of(),"verifier","verify",6);
        expect(verified.current().verificationStatus()==VerificationStatus.VERIFIED);
        var current=new CustomerUpdatePolicy.Current(verified.current(),ph.current().status(),ph.current().version());
        var corrected=person(base,contacts,"def");
        expect(update(current,new CustomerRecordVersion(current.version().owner(),2),verified,corrected).proposed().isEmpty());
        var change=update(current,current.version(),verified,corrected).proposed().orElseThrow();
        var changed=ph.append(change);
        expect(changed.current().version().value()==2);
        expect(changed.current().identity().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(change.history().excludedVerifiers().equals(Set.of("operator")));
        var fresh=evidence(corrected,"r2",documents,8);
        reject("VERIFICATION_HISTORY_SOD_REQUIRED",()->verify(change.history(),corrected,fresh,docs,ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("operator"),"operator","denied",9));
        reject("VERIFICATION_EVIDENCE_REVISION_MISMATCH",()->verify(change.history(),corrected,evidence,docs,ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("operator"),"independent","stale",9));
        var renewed=verify(change.history(),corrected,fresh,docs,ManualVerification.Process.CORRECTED_VERIFIED_IDENTIFIER,Set.of("operator"),"independent","renew",9);
        expect(renewed.current().verificationStatus()==VerificationStatus.VERIFIED);
        var invalid=documents.invalidate(new DocumentVersionHistory.Invalidation(doc.owner(),doc.id(),1,"operator","reason",
            Instant.ofEpochSecond(10),Instant.ofEpochSecond(10),"corr"),DocumentVersionHistory.Authority.INVALIDATE,meta("invalid",10));
        var lost=renewed.appendDocumentImpact("r2",invalid.versions(),DAY,Instant.ofEpochSecond(10),
            DocumentValidity.ExpiryBoundary.UNRESOLVED,VerificationRevocation.Authority.REVOKE_MANUAL,"reason","proof",meta("lost",10));
        expect(lost.current().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(renewed.current().verificationStatus()==VerificationStatus.VERIFIED);
        expect(invalid.entries().size()==2 && documents.entries().size()==1);
        expect(DocumentEventDraft.fromCandidate(invalid.entries().getLast(),"doc-invalid",Optional.empty()).eventType()==DocumentEventDraft.Type.CustomerDocumentInvalidated);
        var updated=new CustomerUpdatePolicy.Current(renewed.current(),changed.current().status(),changed.current().version());
        var updatedParties=new RelationshipLifecycleIntegration.Parties(updated,ch.current(),Optional.of(bh.current()));
        var snapshot=new CrossAggregateLifecycle.Snapshot(ch.current(),CrossAggregateLifecycle.Coverage.COMPLETE_CANDIDATE,List.of(bh.current()),
            List.of(new CrossAggregateLifecycle.Related(rel.fact(),updatedParties)));
        var impact=CrossAggregateLifecycle.assess(ch.current(),snapshot,CustomerStatus.INACTIVE,DAY);
        expect(impact.afterBranches().get(branch.id())==BranchStatus.INACTIVE);
        expect(impact.afterRelationships().get(rel.fact().id())==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        var inactive=CustomerLifecycleIntegration.assess(ch.current(),ch.current().version(),
            new CustomerLifecycleIntegration.Context(T,CustomerLifecycleIntegration.Authority.TRANSITION,CustomerLifecycle.Transition.DEACTIVATE,true,"CUSTOMER_REQUEST",null,meta("inactive",10),"inactive-event"),
            LifecycleReasonCatalog.initial(),LifecycleHistory.empty(company),ArchiveDependencyCatalog.initial(),List.of()).proposed().orElseThrow();
        var inactiveParties=new RelationshipLifecycleIntegration.Parties(updated,inactive.record(),Optional.of(bh.current()));
        expect(RelationshipLifecycleIntegration.effectiveStatus(rel.fact(),DAY,inactiveParties)==RelationshipLifecycle.EffectiveStatus.UNAVAILABLE);
        var reader=actor(T,CUSTOMER_READ,RELATIONSHIP_READ);
        expect(ReadDisclosurePolicy.identityFields(reader,corrected)==ReadDisclosurePolicy.IdentityFields.MASK_REQUIRED);
        expect(eligible(ReadDisclosurePolicy.relationship(reader,rel.fact(),inactiveParties)));
        expect(!eligible(ReadDisclosurePolicy.relationship(actor(new TenantId("foreign"),RELATIONSHIP_READ),rel.fact(),inactiveParties)));
        expect(!eligible(ReadDisclosurePolicy.audit(reader,T)));
        var audit=ReadDisclosurePolicy.audit(actor(T,AUDIT_READ),T);
        expect(eligible(audit));
        expect(AuditRequirementsPolicy.requirement(AUDIT_READ,audit.outcome(),false,false).auditRequired());
        expect(RelationshipLifecycleIntegration.end(rel.fact(),inactiveParties,rel.history(),roles,T,2,RelationshipLifecycle.Authority.END_RELATIONSHIP,
            "reason",meta("stale-end",11),"stale-end").code().equals("RELATIONSHIP_VERSION_STALE"));
        var ended=RelationshipLifecycleIntegration.end(rel.fact(),inactiveParties,rel.history(),roles,T,1,RelationshipLifecycle.Authority.END_RELATIONSHIP,
            "reason",meta("ended",11),"rel-ended").proposed().orElseThrow();
        expect(ended.fact().status()==Relationship.Status.ENDED && ended.fact().version()==2);
        expect(eligible(ReadDisclosurePolicy.relationship(reader,ended.fact(),inactiveParties)));
        expect(ended.event().eventType()==RelationshipEventDraft.Type.RelationshipEnded);
        expect(rel.event().eventType()==RelationshipEventDraft.Type.RelationshipCreated);
        expect(inactive.event().aggregateVersion()==2);
        expect(CustomerChangeEventDraft.fromHistory(changed).size()==3);
        expect(BranchChangeEventDraft.fromHistory(bh).size()==1);
        expect(documents.entries().size()==1 && documents.current().version()==1);
        expect(ph.current().version().value()==1 && ph.current().identity().verificationStatus()==VerificationStatus.NOT_VERIFIED);
        expect(ch.current().status()==CustomerStatus.ACTIVE && bh.current().status()==BranchStatus.ACTIVE && rel.fact().status()==Relationship.Status.ACTIVE);
        expect(verified.current().verificationStatus()==VerificationStatus.VERIFIED && change.history().current().verificationStatus()==VerificationStatus.REQUIRES_REVERIFICATION);
        expect(!CustomerChangeEventDraft.fromHistory(changed).toString().contains("private-file"));
        System.out.println("PASS: "+checks+" final integrated isolated customer domain assertions");
    }
    private static Person person(Person base,ContactInfo contacts,String value) {
        var id=new IdentityIdentifier("id",base.identityInfo().owner(),"passport",value,Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),IdentityIdentifier.Status.ACTIVE);
        return Person.createCandidate(base.id(),T,base.givenName(),base.familyName(),null,contacts,new IdentityInfo(base.identityInfo().owner(),List.of(id)));
    }
    private static CustomerRegistration.Assessment register(Customer c,DocumentRequirementPolicy p,List<DocumentVersionHistory> docs,List<DuplicateDetection.Comparison> comparisons) {
        return CustomerRegistration.assess(c,new CustomerRegistration.Context(T,"request",DuplicateExceptionPolicy.Authority.REGISTER),CONTACTS,IDS,Map.of("phone",PROFILE),Map.of("id",PROFILE),DuplicateDetection.Request.of(c,DuplicateDetection.Operation.CREATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,comparisons,"e1",null,null,p,p.forOperation(c,DocumentRequirementPolicy.Operation.REGISTRATION,"request"),docs,DAY,DocumentValidity.ExpiryBoundary.UNRESOLVED,DocumentCompleteness.CountingConvention.ANY_CURRENT_PROVIDED);
    }
    private static CustomerUpdateVerification.Assessment update(CustomerUpdatePolicy.Current c,CustomerRecordVersion version,VerificationHistory history,Customer proposed) {
        return CustomerUpdateVerification.assess(c,version,proposed,new CustomerUpdatePolicy.Context(T,"request",CustomerUpdatePolicy.Authority.UPDATE,c.status(),c.identity().verificationStatus()),CONTACTS,IDS,Map.of("phone",PROFILE),Map.of("id",PROFILE),DuplicateDetection.Request.of(proposed,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of(),history,c.identity().value(),"r2",CriticalFieldChangeAssessment.ExceptionScope.ORDINARY,"reason",IdentityRevisionAssessment.Impact.MATERIAL,IdentityRevisionAssessment.IdentifierCorrection.VERIFIED_IDENTIFIER_CORRECTED,meta("corrected",7));
    }
    private static VerificationEvidenceSet evidence(Customer c,String revision,DocumentHistory h,long at) {
        var time=Instant.ofEpochSecond(at); var owner=c.identityInfo().owner();
        var binding=new DocumentVerification.Binding(owner,h.current().id(),h.current().version(),DocumentVerification.Necessity.ESSENTIAL);
        return new VerificationEvidenceSet(owner,revision,List.of(new VerificationEvidence("e-"+revision,owner,revision,1,VerificationEvidence.Method.MANUAL,"source","issuer","private-proof","operator",time,time,Optional.empty(),VerificationEvidence.Status.PROVIDED,Optional.of(binding))));
    }
    private static VerificationHistory verify(VerificationHistory h,Customer c,VerificationEvidenceSet evidence,List<DocumentVersionHistory> docs,ManualVerification.Process process,Set<String> excluded,String actor,String id,long at) {
        var ctx=new ManualVerification.Context(c.identityInfo().owner(),actor,ManualVerification.Authority.MANUAL_VERIFY,process,excluded,"reason",Instant.ofEpochSecond(at),"corr");
        return h.appendManual(id,evidence,ctx,ManualVerification.review(c,evidence,ctx,ManualVerification.Qualification.QUALIFIED_CANDIDATE,docs,DAY,DocumentValidity.ExpiryBoundary.UNRESOLVED),DuplicateDetection.Request.of(c,DuplicateDetection.Operation.UPDATE,"request"),DuplicateDetection.Coverage.COMPLETE_CANDIDATE,List.of());
    }
    private static LifecycleHistory.Context meta(String id,long at) { return new LifecycleHistory.Context(id,"operator",Instant.ofEpochSecond(at),"corr"); }
    private static LogicalAccessPolicy.ActorContext actor(TenantId t,LogicalAccessPolicy.Operation... ops) {
        var grants=new HashSet<LogicalAccessPolicy.Grant>();
        for(var op:ops) grants.add(new LogicalAccessPolicy.Grant(t,op));
        return new LogicalAccessPolicy.ActorContext("reader",t,grants);
    }
    private static boolean eligible(LogicalAccessPolicy.Assessment a) { return a.outcome()==LogicalAccessPolicy.Outcome.ELIGIBLE_CANDIDATE; }
    private static void reject(String code,Runnable r) { try { r.run(); } catch (IllegalArgumentException e) { expect(code.equals(e.getMessage())); return; } throw new AssertionError("Expected "+code); }
    private static void expect(boolean v) { if (!v) throw new AssertionError("Final domain integration assertion "+(checks+1)); checks++; }
}
