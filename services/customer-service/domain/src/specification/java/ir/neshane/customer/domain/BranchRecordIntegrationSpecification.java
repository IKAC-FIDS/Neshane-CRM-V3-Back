package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.contact.ContactTypeDefinition;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.SystemContactVocabulary;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class BranchRecordIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final BranchTypeCatalog TYPES=BranchTypeCatalog.initial();
    private static final BranchTypeCatalog.Ref TYPE=new BranchTypeCatalog.Ref("BRANCH",Optional.empty());
    private static final ContactTypeRef MOBILE=ContactTypeRef.system("MOBILE");
    private static final ContactTypeCatalog CONTACTS=ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        MOBILE,"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.BRANCH),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final ValidationProfile PROFILE=new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=parent(company);
        var sibling=branch(company,"sibling","other","Other");
        var original=branch(company,"branch","code","Original");
        var initial=CompanyBranches.empty(company).appendCandidate(sibling);
        var registration=BranchRegistration.assess(original,parent,initial,BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(TENANT,BranchRegistration.Authority.REGISTER),TYPES,CONTACTS,Map.of());
        expect(registration.outcome()==BranchRegistration.Outcome.ELIGIBLE_CANDIDATE); expect(registration.auditRequired());
        var registered=registration.proposed().orElseThrow();
        var history=BranchChangeHistory.start(registered,context(1));
        expect(history.current().version().value()==1); expect(history.current().status()==BranchStatus.ACTIVE);
        expect(registered.branches().branches().equals(List.of(sibling,original))); expect(initial.branches().equals(List.of(sibling)));
        var renamed=branch(company,"branch","new-code","Renamed");
        var update=assess(history.current(),history.current().version(),renamed,parent,registered.branches(),TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of());
        expect(update.outcome()==BranchUpdatePolicy.Outcome.ELIGIBLE_CANDIDATE); expect(update.auditRequired());
        var renamedCandidate=update.proposed().orElseThrow();
        var renamedHistory=history.append(renamedCandidate,context(2));
        expect(renamedHistory.current().version().value()==2); expect(history.entries().size()==1);
        expect(renamedCandidate.fields().equals(Set.of(BranchUpdatePolicy.Field.NAME,BranchUpdatePolicy.Field.CODE)));
        var typed=Branch.createCandidate(original.id(),TENANT,company,"new-code","Renamed",null,null,TYPES,Optional.of(TYPE));
        var phone=new ContactPoint("phone",typed.contactInfo().owner(),MOBILE,new ContactValue.Phone("123456",Optional.empty()));
        var info=CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(typed.contactInfo(),phone),phone.id());
        var enriched=typed.withCandidateContactInfo(info);
        var current=renamedHistory.current(); var collection=renamedCandidate.branches();
        blocked(assess(current,history.current().version(),enriched,parent,collection,TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of("phone",PROFILE)),"BRANCH_RECORD_VERSION_STALE");
        blocked(assess(current,current.version(),enriched,parent,collection,TENANT,BranchUpdatePolicy.Authority.NONE,Map.of("phone",PROFILE)),"BRANCH_UPDATE_AUTHORITY_REQUIRED");
        blocked(assess(current,current.version(),enriched,parent,collection,new TenantId("foreign"),BranchUpdatePolicy.Authority.UPDATE,Map.of("phone",PROFILE)),"BRANCH_RECORD_SCOPE_MISMATCH");
        blocked(assess(current,current.version(),enriched,parent,collection,TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of()),"BRANCH_CONTACT_VALIDATION_BLOCKED");
        blocked(assess(current,current.version(),branch(company,"branch","other","Renamed"),parent,collection,TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of()),"BRANCH_CODE_DUPLICATE");
        var foreign=Company.createCandidate(company.id(),new TenantId("foreign"),"Company",null);
        blocked(assess(current,current.version(),branch(foreign,"branch","new-code","Renamed"),parent,collection,TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of()),"BRANCH_RECORD_SCOPE_MISMATCH");
        expect(renamedHistory.entries().size()==2); expect(BranchChangeEventDraft.fromHistory(renamedHistory).size()==2);
        var accepted=assess(current,current.version(),enriched,parent,collection,TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of("phone",PROFILE));
        expect(accepted.outcome()==BranchUpdatePolicy.Outcome.ELIGIBLE_CANDIDATE); expect(accepted.auditRequired());
        var candidate=accepted.proposed().orElseThrow(); var enrichedHistory=renamedHistory.append(candidate,context(3));
        expect(candidate.fields().equals(Set.of(BranchUpdatePolicy.Field.TYPE,BranchUpdatePolicy.Field.CONTACT_INFO)));
        expect(enrichedHistory.current().version().value()==3); expect(candidate.branches().branches().equals(List.of(sibling,enriched)));
        expect(enriched.contactInfo().primary(ContactGroup.TELEPHONE).orElseThrow().equals(phone.id()));
        expect(enriched.type().orElseThrow().equals(TYPE)); expect(renamed.contactInfo().points().isEmpty());
        var noop=assess(enrichedHistory.current(),candidate.record().version(),enriched,parent,candidate.branches(),TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of());
        expect(noop.outcome()==BranchUpdatePolicy.Outcome.UNCHANGED); expect(noop.proposed().isEmpty()); expect(!noop.auditRequired());
        var deactivated=enriched.withCandidateContactInfo(info.deactivateCandidate(phone.id()));
        var removal=assess(enrichedHistory.current(),candidate.record().version(),typed,parent,candidate.branches(),TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of());
        blocked(removal,"BRANCH_CONTACT_HISTORY_REQUIRED");
        var deactivate=assess(enrichedHistory.current(),candidate.record().version(),deactivated,parent,candidate.branches(),TENANT,BranchUpdatePolicy.Authority.UPDATE,Map.of()).proposed().orElseThrow();
        var completed=enrichedHistory.append(deactivate,context(4));
        expect(completed.current().version().value()==4); expect(completed.current().status()==BranchStatus.ACTIVE);
        expect(deactivated.contactInfo().points().size()==1 && deactivated.contactInfo().points().getFirst().isInactiveVersionOf(phone)); expect(deactivated.contactInfo().primary(ContactGroup.TELEPHONE).isEmpty());
        expect(completed.entries().size()==4); expect(enrichedHistory.entries().size()==3);
        var events=BranchChangeEventDraft.fromHistory(completed);
        expect(events.size()==4); expect(events.stream().map(BranchChangeEventDraft::eventId).distinct().count()==4);
        for (int i=0;i<events.size();i++) {
            var event=events.get(i); var entry=completed.entries().get(i);
            expect(event.eventType()==(i==0?BranchChangeEventDraft.Type.BranchRegistered:BranchChangeEventDraft.Type.BranchUpdated));
            expect(event.recordVersion()==i+1); expect(event.previousVersion().equals(entry.previousVersion().map(BranchRecordVersion::value)));
            expect(event.fields().equals(entry.fields())); expect(event.historyId().equals(entry.context().historyId()));
            expect(event.tenantId().equals(TENANT)&&event.companyId().equals(company.id())&&event.branchId().equals(original.id()));
            expect(event.eventId().equals(BranchChangeEventDraft.fromCandidate(entry).eventId()));
            expect(!event.toString().contains("123456"));
        }
        expect(parent.version().value()==1); expect(parent.status()==CustomerStatus.ACTIVE); expect(parent.identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(parent.identity().candidate()==company); expect(initial.branches().size()==1);
        System.out.println("PASS: "+checks+" integrated isolated branch record assertions");
    }
    private static CustomerUpdatePolicy.Current parent(Company c) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(c)); }
    private static Branch branch(Company c,String id,String code,String name) { return Branch.createCandidate(new BranchId(id),c.tenantId(),c,code,name,null,null); }
    private static LifecycleHistory.Context context(int i) { return new LifecycleHistory.Context("h"+i,"actor",Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i),"request"+i); }
    private static BranchUpdatePolicy.Assessment assess(BranchUpdatePolicy.Current current,BranchRecordVersion expected,Branch proposed,CustomerUpdatePolicy.Current parent,CompanyBranches branches,TenantId tenant,BranchUpdatePolicy.Authority authority,Map<String,ValidationProfile> profiles) {
        return BranchUpdatePolicy.assess(current,expected,proposed,parent,branches,BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchUpdatePolicy.Context(tenant,authority,current.status()),TYPES,CONTACTS,profiles);
    }
    private static void blocked(BranchUpdatePolicy.Assessment result,String code) { expect(result.outcome()==BranchUpdatePolicy.Outcome.BLOCKED); expect(result.code().equals(code)); expect(result.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch record integration assertion "+(checks+1)); checks++; }
}
