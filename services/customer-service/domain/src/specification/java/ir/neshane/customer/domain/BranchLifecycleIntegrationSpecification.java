package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import static ir.neshane.customer.domain.BranchLifecycle.Transition.*;
import static ir.neshane.customer.domain.BranchLifecycleIntegration.Authority.*;

final class BranchLifecycleIntegrationSpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final LifecycleReasonCatalog REASONS=LifecycleReasonCatalog.initial();
    static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(company,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(company));
        var branch=Branch.createCandidate(new BranchId("branch"),TENANT,company,"code","Branch",null,null);
        var sibling=Branch.createCandidate(new BranchId("sibling"),TENANT,company,"other","Other",null,null);
        var reg=BranchRegistration.assess(branch,parent,CompanyBranches.empty(company).appendCandidate(sibling),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(TENANT,BranchRegistration.Authority.REGISTER),BranchTypeCatalog.initial(),ContactTypeCatalog.empty(),Map.of()).proposed().orElseThrow();
        var facts=BranchChangeHistory.start(reg,meta(0)); var current=facts.current(); var history=BranchHistory.empty(branch); var collection=reg.branches();
        var close=context(TENANT,TRANSITION,DEACTIVATE,"BRANCH_CLOSED",1);
        var result=assess(current,current.version(),parent,collection,close,history);
        expect(result.outcome()==BranchLifecycleIntegration.Outcome.ELIGIBLE_CANDIDATE); expect(result.auditRequired());
        var candidate=result.proposed().orElseThrow();
        expect(candidate.previous()==current); expect(candidate.record().branch()==branch); expect(candidate.record().status()==BranchStatus.INACTIVE);
        expect(candidate.record().version().value()==2); expect(candidate.history().entries().size()==1); expect(history.entries().isEmpty());
        var entry=candidate.history().entries().getFirst(); var event=candidate.event();
        expect(entry.previousStatus()==BranchStatus.ACTIVE); expect(entry.newStatus()==BranchStatus.INACTIVE);
        expect(entry.reason().orElseThrow().reasonId().equals("BRANCH_CLOSED")); expect(event.eventType()==BranchEventDraft.Type.BranchDeactivated);
        expect(event.aggregateVersion()==2); expect(event.aggregateId().equals(branch.id())); expect(event.tenantId().equals(TENANT));
        expect(event.payload().companyId().equals(company.id())); expect(event.payload().newStatus()==BranchStatus.INACTIVE);
        expect(event.payload().previousStatus()==BranchStatus.ACTIVE); expect(event.occurredAt().equals(meta(1).occurredAt()));
        var advanced=facts.advanceLifecycle(candidate); expect(advanced.current()==candidate.record()); expect(advanced.entries().size()==1);
        expect(BranchChangeEventDraft.fromHistory(advanced).size()==1); expect(facts.current().version().value()==1);
        var reopen=context(TENANT,TRANSITION,REACTIVATE,"BRANCH_REOPENED",2);
        var inactiveParent=new CustomerUpdatePolicy.Current(parent.identity(),CustomerStatus.INACTIVE,parent.version());
        var archivedParent=new CustomerUpdatePolicy.Current(parent.identity(),CustomerStatus.ARCHIVED,parent.version());
        blocked(assess(candidate.record(),candidate.record().version(),inactiveParent,collection,reopen,candidate.history()),"BRANCH_PARENT_NOT_ACTIVE");
        blocked(assess(candidate.record(),candidate.record().version(),archivedParent,collection,reopen,candidate.history()),"BRANCH_PARENT_ARCHIVED");
        blocked(assess(candidate.record(),current.version(),parent,collection,reopen,candidate.history()),"BRANCH_RECORD_VERSION_STALE");
        blocked(assess(current,null,parent,collection,close,history),"BRANCH_RECORD_VERSION_REQUIRED");
        blocked(assess(current,current.version(),parent,collection,context(new TenantId("foreign"),TRANSITION,DEACTIVATE,"BRANCH_CLOSED",1),history),"BRANCH_RECORD_SCOPE_MISMATCH");
        var denied=assess(current,current.version(),parent,collection,context(TENANT,NONE,DEACTIVATE,"BRANCH_CLOSED",1),history);
        blocked(denied,"BRANCH_LIFECYCLE_AUTHORITY_REQUIRED"); expect(denied.auditRequired());
        blocked(assess(current,current.version(),parent,collection,context(TENANT,TRANSITION,DEACTIVATE,null,1),history),"BRANCH_REASON_REQUIRED");
        blocked(assess(current,current.version(),parent,CompanyBranches.empty(company),close,history),"BRANCH_COLLECTION_SNAPSHOT_MISMATCH");
        blocked(assess(candidate.record(),candidate.record().version(),parent,collection,close,candidate.history()),"BRANCH_TRANSITION_NOT_ALLOWED");
        var reopened=assess(candidate.record(),candidate.record().version(),parent,collection,reopen,candidate.history()).proposed().orElseThrow();
        expect(reopened.record().status()==BranchStatus.ACTIVE); expect(reopened.record().version().value()==3);
        expect(reopened.event().eventType()==BranchEventDraft.Type.BranchReactivated); expect(reopened.event().aggregateVersion()==3);
        expect(reopened.history().entries().size()==2); expect(reopened.history().entries().getLast().previousStatus()==BranchStatus.INACTIVE);
        var completed=advanced.advanceLifecycle(reopened); expect(completed.current().version().value()==3); expect(completed.entries().size()==1);
        var renamed=Branch.createCandidate(branch.id(),TENANT,company,"code","Renamed",null,null);
        var edit=BranchUpdatePolicy.assess(completed.current(),completed.current().version(),renamed,parent,collection,BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchUpdatePolicy.Context(TENANT,BranchUpdatePolicy.Authority.UPDATE,BranchStatus.ACTIVE)).proposed().orElseThrow();
        var afterEdit=completed.append(edit,meta(3)); expect(afterEdit.current().version().value()==4); expect(afterEdit.entries().size()==2);
        expect(BranchChangeEventDraft.fromHistory(afterEdit).getLast().recordVersion()==4);
        expect(parent.version().value()==1); expect(parent.status()==CustomerStatus.ACTIVE); expect(parent.identity().verificationStatus()==VerificationStatus.VERIFIED);
        expect(collection.branches().equals(List.of(sibling,branch))); expect(candidate.history().entries().size()==1);
        System.out.println("PASS: "+checks+" isolated branch lifecycle integration assertions");
    }
    private static LifecycleHistory.Context meta(int i) { return new LifecycleHistory.Context("h"+i,"actor",Instant.ofEpochSecond(i),"request"+i); }
    private static BranchLifecycleIntegration.Context context(TenantId t,BranchLifecycleIntegration.Authority a,BranchLifecycle.Transition op,String reason,int i) { return new BranchLifecycleIntegration.Context(t,a,op,true,reason,null,meta(i),"event"+i); }
    private static BranchLifecycleIntegration.Assessment assess(BranchUpdatePolicy.Current c,BranchRecordVersion v,CustomerUpdatePolicy.Current p,CompanyBranches branches,BranchLifecycleIntegration.Context ctx,BranchHistory history) {
        return BranchLifecycleIntegration.assess(c,v,p,branches,BranchRegistration.Coverage.COMPLETE_CANDIDATE,ctx,REASONS,history);
    }
    private static void blocked(BranchLifecycleIntegration.Assessment a,String code) { expect(a.outcome()==BranchLifecycleIntegration.Outcome.BLOCKED); expect(a.code().equals(code)); expect(a.proposed().isEmpty()); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch lifecycle integration assertion "+(checks+1)); checks++; }
}
