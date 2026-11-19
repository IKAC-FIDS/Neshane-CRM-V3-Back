package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.BranchRegistration.Authority.*;
import static ir.neshane.customer.domain.BranchRegistration.Coverage.*;
import static ir.neshane.customer.domain.BranchRegistration.Outcome.*;

final class BranchRegistrationSpecification {
    private static int checks;
    private static final TenantId TENANT = new TenantId("tenant");
    private static final BranchTypeCatalog TYPES = BranchTypeCatalog.initial();
    private static final BranchTypeCatalog.Ref TYPE = new BranchTypeCatalog.Ref("BRANCH",Optional.empty());
    private static final ContactTypeCatalog CONTACTS = ContactTypeCatalog.empty().withCandidateDefinition(new ContactTypeDefinition(
        ContactTypeRef.system("MOBILE"),"MOBILE",new LocalizedText(Map.of("fa","موبایل")),new LocalizedText(Map.of()),
        SystemContactVocabulary.MOBILE.valueType(),Set.of(ContactOwner.Kind.BRANCH),true,Optional.of("p"),ContactTypeDefinition.Status.ACTIVE,0));
    private static final ValidationProfile PROFILE = new ValidationProfile("p",Optional.empty(),java.util.List.of(new ValidationProfile.Length(ValidationProfile.Field.VALUE,3,10)));
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=current(company,CustomerStatus.ACTIVE);
        var existing=CompanyBranches.empty(company);
        var branch=branch(company,"branch","code");
        var ctx=new BranchRegistration.Context(TENANT,REGISTER);
        var result=assess(branch,parent,existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of());
        expect(result.outcome()==ELIGIBLE_CANDIDATE);
        expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow();
        expect(candidate.branch()==branch);
        expect(candidate.branch().status()==BranchStatus.ACTIVE);
        expect(candidate.version().value()==1);
        expect(candidate.version().owner().equals(BranchRecordVersion.Owner.of(branch)));
        expect(candidate.branches().branches().equals(java.util.List.of(branch)));
        expect(existing.branches().isEmpty());
        expect(parent.version().value()==1);
        expect(parent.identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(!candidate.toString().contains("code"));
        var denied=assess(branch,parent,existing,COMPLETE_CANDIDATE,new BranchRegistration.Context(TENANT,NONE),TYPES,Map.of());
        expect(denied.auditRequired());
        blocked(denied,"BRANCH_REGISTRATION_AUTHORITY_REQUIRED");
        blocked(assess(branch,parent,existing,UNKNOWN,ctx,TYPES,Map.of()),"BRANCH_REGISTRATION_COVERAGE_UNRESOLVED");
        blocked(assess(branch,parent,null,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_REGISTRATION_COVERAGE_UNRESOLVED");
        blocked(assess(branch,null,existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_PARENT_REQUIRED");
        blocked(assess(branch,current(Person.createCandidate(company.id(),TENANT,"A","B",null),CustomerStatus.ACTIVE),existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_PARENT_REQUIRED");
        for (var status : new CustomerStatus[]{CustomerStatus.INACTIVE,CustomerStatus.ARCHIVED})
            blocked(assess(branch,current(company,status),existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_PARENT_NOT_ACTIVE");
        var other=Company.createCandidate(new CustomerId("other"),TENANT,"Other",null);
        blocked(assess(branch,current(other,CustomerStatus.ACTIVE),existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_PARENT_SCOPE_MISMATCH");
        blocked(assess(branch,parent,CompanyBranches.empty(other),COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_PARENT_SCOPE_MISMATCH");
        blocked(assess(branch,parent,existing,COMPLETE_CANDIDATE,new BranchRegistration.Context(new TenantId("foreign"),REGISTER),TYPES,Map.of()),"BRANCH_PARENT_SCOPE_MISMATCH");
        blocked(assess(branch,parent,candidate.branches(),COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_ID_DUPLICATE");
        blocked(assess(branch(company,"other","code"),parent,candidate.branches(),COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_CODE_DUPLICATE");
        expect(assess(branch(company,"other",null),parent,candidate.branches(),COMPLETE_CANDIDATE,ctx,TYPES,Map.of()).outcome()==ELIGIBLE_CANDIDATE);
        blocked(assess(branch,parent,existing,COMPLETE_CANDIDATE,ctx,TYPES.withHidden(TENANT,TYPE,true),Map.of()),"BRANCH_TYPE_NOT_SELECTABLE");
        var phone=new ContactPoint("phone",branch.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        var info=CONTACTS.selectPrimaryCandidate(CONTACTS.appendCandidate(branch.contactInfo(),phone),"phone");
        var withPhone=branch.withCandidateContactInfo(info);
        blocked(assess(withPhone,parent,existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_CONTACT_VALIDATION_BLOCKED");
        var phoneResult=assess(withPhone,parent,existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of("phone",PROFILE));
        expect(phoneResult.outcome()==ELIGIBLE_CANDIDATE);
        expect(phoneResult.proposed().orElseThrow().branch().contactInfo().equals(info));
        expect(phoneResult.proposed().orElseThrow().branch().contactInfo().primary(ContactGroup.TELEPHONE).orElseThrow().equals("phone"));
        blocked(assess(branch.withCandidateContactInfo(info.deactivateCandidate("phone")),parent,existing,COMPLETE_CANDIDATE,ctx,TYPES,Map.of()),"BRANCH_CONTACT_NOT_ACTIVE");
        expect(existing.branches().isEmpty());
        expect(branch.contactInfo().points().isEmpty());
        System.out.println("PASS: "+checks+" isolated branch registration assertions");
    }
    private static Branch branch(Company c,String id,String code) { return Branch.createCandidate(new BranchId(id),c.tenantId(),c,code,"Branch",null,null,TYPES,Optional.of(TYPE)); }
    private static CustomerUpdatePolicy.Current current(Customer c,CustomerStatus s) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),s,CustomerRecordVersion.initial(c)); }
    private static BranchRegistration.Assessment assess(Branch b,CustomerUpdatePolicy.Current p,CompanyBranches e,BranchRegistration.Coverage coverage,BranchRegistration.Context ctx,BranchTypeCatalog t,Map<String,ValidationProfile> profiles) { return BranchRegistration.assess(b,p,e,coverage,ctx,t,CONTACTS,profiles); }
    private static void blocked(BranchRegistration.Assessment a,String code) { expect(a.outcome()==BLOCKED); expect(a.code().equals(code)); expect(a.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch registration assertion "+(checks+1)); checks++; }
}
