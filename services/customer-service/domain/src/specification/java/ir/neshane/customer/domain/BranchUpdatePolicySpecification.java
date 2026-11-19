package ir.neshane.customer.domain;

import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.BranchUpdatePolicy.Outcome.*;
import static ir.neshane.customer.domain.BranchUpdatePolicy.Authority.*;

final class BranchUpdatePolicySpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=parent(company,CustomerStatus.ACTIVE);
        var branch=branch(company,"b1","one","Original",null,null);
        var other=branch(company,"b2","two","Other",null,null);
        var existing=CompanyBranches.empty(company).appendCandidate(branch).appendCandidate(other);
        var version=BranchRecordVersion.initial(branch);
        var current=new BranchUpdatePolicy.Current(branch,BranchStatus.ACTIVE,version);
        var ctx=new BranchUpdatePolicy.Context(TENANT,UPDATE,BranchStatus.ACTIVE);
        var edited=branch(company,"b1","new","Changed","English","Description");
        var result=assess(current,version,edited,parent,existing,ctx);
        expect(result.outcome()==ELIGIBLE_CANDIDATE);
        expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow();
        expect(candidate.previous()==current);
        expect(candidate.record().branch()==edited);
        expect(candidate.record().version().value()==2);
        expect(candidate.record().status()==BranchStatus.ACTIVE);
        expect(candidate.fields().equals(Set.of(BranchUpdatePolicy.Field.NAME,BranchUpdatePolicy.Field.ENGLISH_NAME,BranchUpdatePolicy.Field.CODE,BranchUpdatePolicy.Field.DESCRIPTION)));
        expect(candidate.branches().branches().equals(java.util.List.of(edited,other)));
        expect(existing.branches().equals(java.util.List.of(branch,other)));
        expect(current.version().value()==1);
        expect(parent.version().value()==1);
        expect(parent.identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(!candidate.toString().contains("Changed"));
        var noop=assess(current,version,branch(company,"b1","one","Original",null,null),parent,existing,ctx);
        expect(noop.outcome()==UNCHANGED); expect(noop.proposed().isEmpty()); expect(!noop.auditRequired());
        blocked(assess(current,null,edited,parent,existing,ctx),"BRANCH_RECORD_VERSION_REQUIRED");
        blocked(assess(current,candidate.record().version(),edited,parent,existing,ctx),"BRANCH_RECORD_VERSION_STALE");
        blocked(assess(current,BranchRecordVersion.initial(other),edited,parent,existing,ctx),"BRANCH_RECORD_SCOPE_MISMATCH");
        blocked(assess(current,version,other,parent,existing,ctx),"BRANCH_RECORD_SCOPE_MISMATCH");
        blocked(assess(current,version,branch(company,"b1","two","Changed",null,null),parent,existing,ctx),"BRANCH_CODE_DUPLICATE");
        blocked(assess(current,version,edited,parent,CompanyBranches.empty(company),ctx),"BRANCH_COLLECTION_SNAPSHOT_MISMATCH");
        blocked(assess(current,version,edited,parent,null,ctx),"BRANCH_UPDATE_COVERAGE_UNRESOLVED");
        blocked(BranchUpdatePolicy.assess(current,version,edited,parent,existing,BranchRegistration.Coverage.UNKNOWN,ctx),"BRANCH_UPDATE_COVERAGE_UNRESOLVED");
        var denied=assess(current,version,edited,parent,existing,new BranchUpdatePolicy.Context(TENANT,NONE,BranchStatus.ACTIVE));
        blocked(denied,"BRANCH_UPDATE_AUTHORITY_REQUIRED"); expect(denied.auditRequired());
        blocked(assess(current,version,edited,parent,existing,new BranchUpdatePolicy.Context(TENANT,UPDATE,BranchStatus.INACTIVE)),"BRANCH_UPDATE_LIFECYCLE_CHANGE_FORBIDDEN");
        blocked(assess(current,version,edited,parent(company,CustomerStatus.ARCHIVED),existing,ctx),"BRANCH_PARENT_ARCHIVED");
        var inactive=new BranchUpdatePolicy.Current(branch,BranchStatus.INACTIVE,version);
        expect(assess(inactive,version,edited,parent(company,CustomerStatus.INACTIVE),existing,new BranchUpdatePolicy.Context(TENANT,UPDATE,BranchStatus.INACTIVE)).proposed().orElseThrow().record().status()==BranchStatus.INACTIVE);
        var foreign=Company.createCandidate(company.id(),new TenantId("foreign"),"Company",null);
        blocked(assess(current,version,branch(foreign,"b1","one","Changed",null,null),parent,existing,ctx),"BRANCH_RECORD_SCOPE_MISMATCH");
        var another=Company.createCandidate(new CustomerId("another"),TENANT,"Company",null);
        blocked(assess(current,version,branch(another,"b1","one","Changed",null,null),parent,existing,ctx),"BRANCH_RECORD_SCOPE_MISMATCH");
        blocked(assess(current,version,edited,parent(another,CustomerStatus.ACTIVE),existing,ctx),"BRANCH_RECORD_SCOPE_MISMATCH");
        var typed=Branch.createCandidate(branch.id(),TENANT,company,"one","Original",null,null,BranchTypeCatalog.initial(),Optional.of(new BranchTypeCatalog.Ref("BRANCH",Optional.empty())));
        blocked(assess(current,version,typed,parent,existing,ctx),"BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED");
        var phone=new ContactPoint("phone",branch.contactInfo().owner(),ContactTypeRef.system("MOBILE"),new ContactValue.Phone("123",Optional.empty()));
        blocked(assess(current,version,branch.withCandidateContactInfo(branch.contactInfo().withCandidatePoint(phone)),parent,existing,ctx),"BRANCH_UPDATE_TYPE_CONTACT_VALIDATION_REQUIRED");
        var max=new BranchRecordVersion(version.owner(),Long.MAX_VALUE);
        blocked(assess(new BranchUpdatePolicy.Current(branch,BranchStatus.ACTIVE,max),max,edited,parent,existing,ctx),"BRANCH_RECORD_VERSION_LIMIT");
        expect(assess(current,version,branch(company,"b1",null,"Original",null,null),parent,existing,ctx).outcome()==ELIGIBLE_CANDIDATE);
        System.out.println("PASS: "+checks+" isolated branch update policy assertions");
    }
    private static Branch branch(Company c,String id,String code,String name,String english,String description) { return Branch.createCandidate(new BranchId(id),c.tenantId(),c,code,name,english,description); }
    private static CustomerUpdatePolicy.Current parent(Company c,CustomerStatus s) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),s,CustomerRecordVersion.initial(c)); }
    private static BranchUpdatePolicy.Assessment assess(BranchUpdatePolicy.Current c,BranchRecordVersion v,Branch b,CustomerUpdatePolicy.Current p,CompanyBranches e,BranchUpdatePolicy.Context ctx) { return BranchUpdatePolicy.assess(c,v,b,p,e,BranchRegistration.Coverage.COMPLETE_CANDIDATE,ctx); }
    private static void blocked(BranchUpdatePolicy.Assessment a,String code) { expect(a.outcome()==BLOCKED); expect(a.code().equals(code)); expect(a.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch update policy assertion "+(checks+1)); checks++; }
}
