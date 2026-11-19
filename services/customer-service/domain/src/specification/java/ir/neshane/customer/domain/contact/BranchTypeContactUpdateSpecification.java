package ir.neshane.customer.domain.contact;

import ir.neshane.customer.domain.branch.state.BranchRegistration;
import ir.neshane.customer.domain.branch.model.BranchStatus;
import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.state.CompanyBranches;
import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;
import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.state.IdentityRevision;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.shared.validation.ValidationProfile;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.branch.state.BranchUpdatePolicy.Outcome.*;

public final class BranchTypeContactUpdateSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final BranchTypeCatalog TYPES=BranchTypeCatalog.initial();
    private static final BranchTypeCatalog.Ref TYPE=new BranchTypeCatalog.Ref("BRANCH",Optional.empty());
    private static final ContactTypeRef MOBILE=ContactTypeRef.system("MOBILE");
    private static final ContactTypeCatalog CONTACTS=ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        MOBILE,"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.BRANCH),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final ValidationProfile PROFILE=new ValidationProfile("p",Optional.empty(),List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    public static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(company,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(company));
        var base=Branch.createCandidate(new BranchId("b"),TENANT,company,"code","Branch",null,null);
        var typed=Branch.createCandidate(base.id(),TENANT,company,"code","Branch",null,null,TYPES,Optional.of(TYPE));
        var phone=new ContactPoint("p1",base.contactInfo().owner(),MOBILE,new ContactValue.Phone("123",Optional.empty()));
        var info=CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(base.contactInfo(),phone),phone.id());
        var combined=typed.withCandidateContactInfo(info);
        var result=assess(company,parent,base,combined,TYPES,CONTACTS,Map.of("p1",PROFILE));
        expect(result.outcome()==ELIGIBLE_CANDIDATE); expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow();
        expect(candidate.fields().equals(Set.of(BranchUpdatePolicy.Field.TYPE,BranchUpdatePolicy.Field.CONTACT_INFO)));
        expect(candidate.record().version().value()==2); expect(candidate.record().branch()==combined);
        expect(candidate.record().status()==BranchStatus.ACTIVE); expect(candidate.branches().branches().equals(List.of(combined)));
        expect(parent.version().value()==1); expect(base.contactInfo().points().isEmpty()); expect(base.type().isEmpty());
        blocked(assess(company,parent,base,combined,TYPES.withHidden(TENANT,TYPE,true),CONTACTS,Map.of("p1",PROFILE)),"BRANCH_TYPE_NOT_SELECTABLE");
        blocked(assess(company,parent,base,combined,TYPES,CONTACTS,Map.of()),"BRANCH_CONTACT_VALIDATION_BLOCKED");
        blocked(assess(company,parent,base,combined,TYPES,CONTACTS.withSystemHidden(TENANT,MOBILE,true),Map.of("p1",PROFILE)),"CONTACT_TYPE_NOT_SELECTABLE");
        blocked(assess(company,parent,base,combined,null,CONTACTS,Map.of("p1",PROFILE)),"BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED");
        blocked(assess(company,parent,base,combined,TYPES,null,Map.of("p1",PROFILE)),"BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED");
        expect(assess(company,parent,typed,base,TYPES,CONTACTS,Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(company,parent,typed,combined,TYPES.withHidden(TENANT,TYPE,true),CONTACTS,Map.of("p1",PROFILE)).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(company,parent,combined,combined,TYPES.withHidden(TENANT,TYPE,true),CONTACTS,Map.of()).outcome()==UNCHANGED);
        blocked(assess(company,parent,combined,typed,TYPES,CONTACTS,Map.of()),"BRANCH_CONTACT_HISTORY_REQUIRED");
        var inactive=typed.withCandidateContactInfo(info.deactivateCandidate("p1"));
        var deactivation=assess(company,parent,combined,inactive,TYPES,CONTACTS.withSystemHidden(TENANT,MOBILE,true),Map.of());
        expect(deactivation.outcome()==ELIGIBLE_CANDIDATE); expect(deactivation.auditRequired());
        expect(inactive.contactInfo().points().equals(List.of(phone.inactive()))); expect(inactive.contactInfo().primary(ContactGroup.TELEPHONE).isEmpty());
        blocked(assess(company,parent,typed,inactive,TYPES,CONTACTS,Map.of()),"BRANCH_CONTACT_INACTIVE_CHANGE_INVALID");
        blocked(assess(company,parent,inactive,combined,TYPES,CONTACTS,Map.of("p1",PROFILE)),"BRANCH_CONTACT_INACTIVE_CHANGE_INVALID");
        var edited=new ContactPoint("p1",phone.owner(),MOBILE,new ContactValue.Phone("456",Optional.empty()));
        var edit=typed.withCandidateContactInfo(new ContactInfo(phone.owner(),List.of(edited)).selectPrimary(ContactGroup.TELEPHONE,"p1"));
        expect(assess(company,parent,combined,edit,TYPES,CONTACTS,Map.of("p1",PROFILE)).outcome()==ELIGIBLE_CANDIDATE);
        var shortPhone=new ContactPoint("p1",phone.owner(),MOBILE,new ContactValue.Phone("1",Optional.empty()));
        blocked(assess(company,parent,combined,typed.withCandidateContactInfo(new ContactInfo(phone.owner(),List.of(shortPhone))),TYPES,CONTACTS,Map.of("p1",PROFILE)),"BRANCH_CONTACT_VALIDATION_FAIL");
        var second=new ContactPoint("p2",phone.owner(),MOBILE,new ContactValue.Phone("789",Optional.empty()));
        var added=typed.withCandidateContactInfo(info.withCandidatePoint(second));
        expect(assess(company,parent,combined,added,TYPES,CONTACTS,Map.of("p2",PROFILE)).outcome()==ELIGIBLE_CANDIDATE);
        var swap=typed.withCandidateContactInfo(added.contactInfo().selectPrimary(ContactGroup.TELEPHONE,"p2"));
        expect(assess(company,parent,added,swap,TYPES,CONTACTS,Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        blocked(assess(company,parent,added,swap,TYPES,CONTACTS.withSystemHidden(TENANT,MOBILE,true),Map.of()),"CONTACT_TYPE_NOT_SELECTABLE");
        expect(assess(company,parent,combined,typed.withCandidateContactInfo(info.clearPrimary(ContactGroup.TELEPHONE)),TYPES,CONTACTS,Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        expect(assess(company,parent,inactive,typed.withCandidateContactInfo(inactive.contactInfo().withCandidatePoint(second)),TYPES,CONTACTS,Map.of("p2",PROFILE)).outcome()==ELIGIBLE_CANDIDATE);
        System.out.println("PASS: "+checks+" isolated branch type and contact update assertions");
    }
    private static BranchUpdatePolicy.Assessment assess(Company company,CustomerUpdatePolicy.Current parent,Branch original,Branch proposed,BranchTypeCatalog types,ContactTypeCatalog contacts,Map<String,ValidationProfile> profiles) {
        var version=BranchRecordVersion.initial(original);
        return BranchUpdatePolicy.assess(new BranchUpdatePolicy.Current(original,BranchStatus.ACTIVE,version),version,proposed,parent,
            CompanyBranches.empty(company).appendCandidate(original),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchUpdatePolicy.Context(TENANT,BranchUpdatePolicy.Authority.UPDATE,BranchStatus.ACTIVE),types,contacts,profiles);
    }
    private static void blocked(BranchUpdatePolicy.Assessment a,String code) { expect(a.outcome()==BLOCKED); expect(a.code().equals(code)); expect(a.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch type/contact update assertion "+(checks+1)); checks++; }
}
