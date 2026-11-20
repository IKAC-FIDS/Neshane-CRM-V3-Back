package ir.neshane.customer.domain.branch.lifecycle;

import ir.neshane.customer.domain.branch.state.CompanyBranches;

import ir.neshane.customer.domain.branch.state.BranchUpdatePolicy;

import ir.neshane.customer.domain.branch.state.BranchRegistration;

import ir.neshane.customer.domain.branch.type.BranchTypeCatalog;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchRecordVersion;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.IdentityRevision;

import ir.neshane.customer.domain.customer.history.LifecycleHistory;

import ir.neshane.customer.domain.customer.model.Company;
import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.model.CustomerStatus;
import ir.neshane.customer.domain.verification.model.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactTypeCatalog;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.BranchId;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class BranchChangeHistorySpecification {
    private static int checks;
    private static final TenantId TENANT=new TenantId("tenant");
    private static final Instant TIME=Instant.parse("2026-01-01T00:00:00Z");
    public static void run() {
        var company=Company.createCandidate(new CustomerId("company"),TENANT,"Company",null);
        var parent=new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(company,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(company));
        var original=branch(company,"b","Original");
        var registration=register(company,parent,original);
        var start=BranchChangeHistory.start(registration,context("h1",0));
        var first=start.entries().getFirst();
        expect(start.entries().size()==1); expect(first.action()==BranchChangeHistory.Action.REGISTER);
        expect(first.previousVersion().isEmpty()); expect(first.nextVersion().value()==1);
        expect(first.owner().equals(BranchRecordVersion.Owner.of(original))); expect(first.fields().isEmpty());
        expect(first.context().actorId().equals("actor")); expect(first.context().correlationId().equals("request-h1"));
        expect(first.context().occurredAt().equals(TIME)); expect(start.current().branch()==original);
        expect(start.current().status()==BranchStatus.ACTIVE); expect(start.current().version().equals(registration.version()));
        var edited=branch(company,"b","Edited");
        var update=update(start.current(),edited,parent,registration.branches());
        var second=start.append(update,context("h2",1));
        var entry=second.entries().getLast();
        expect(second.entries().size()==2); expect(start.entries().size()==1); expect(second.entries().getFirst()==first);
        expect(entry.action()==BranchChangeHistory.Action.UPDATE); expect(entry.previousVersion().orElseThrow().value()==1);
        expect(entry.nextVersion().value()==2); expect(entry.owner().equals(first.owner()));
        expect(entry.fields().equals(Set.of(BranchUpdatePolicy.Field.NAME))); expect(second.current().branch()==edited);
        expect(second.current().version().equals(update.record().version())); expect(entry.context().historyId().equals("h2"));
        expect(!entry.toString().contains("Edited")); expect(!second.toString().contains("Original"));
        expect(!entry.toString().contains("actor")); expect(parent.version().value()==1);
        reject("HISTORY_ID_DUPLICATE",()->start.append(update,context("h1",1)));
        reject("HISTORY_TIME_DISCONTINUITY",()->start.append(update,context("h2",-1)));
        reject("BRANCH_HISTORY_VERSION_DISCONTINUITY",()->second.append(update,context("h3",2)));
        var fork=branch(company,"b","Forked");
        var forkCurrent=new BranchUpdatePolicy.Current(fork,BranchStatus.ACTIVE,registration.version());
        var forkUpdate=update(forkCurrent,edited,parent,CompanyBranches.empty(company).appendCandidate(fork));
        reject("BRANCH_HISTORY_FACT_DISCONTINUITY",()->start.append(forkUpdate,context("h3",2)));
        var other=branch(company,"other","Other");
        var otherRegistration=register(company,parent,other);
        var otherHistory=BranchChangeHistory.start(otherRegistration,context("o1",0));
        var otherUpdate=update(otherHistory.current(),branch(company,"other","Changed"),parent,otherRegistration.branches());
        reject("BRANCH_HISTORY_SCOPE_MISMATCH",()->start.append(otherUpdate,context("h3",2)));
        var inactive=new BranchUpdatePolicy.Current(edited,BranchStatus.INACTIVE,second.current().version());
        var thirdUpdate=update(inactive,branch(company,"b","Third"),parent,update.branches());
        var third=second.append(thirdUpdate,context("h3",1));
        expect(third.entries().size()==3); expect(third.current().status()==BranchStatus.INACTIVE);
        expect(third.current().version().value()==3); expect(second.current().status()==BranchStatus.ACTIVE);
        var noop=assess(start.current(),original,parent,registration.branches());
        expect(noop.outcome()==BranchUpdatePolicy.Outcome.UNCHANGED); expect(noop.proposed().isEmpty());
        var blocked=assess(start.current(),other,parent,registration.branches());
        expect(blocked.outcome()==BranchUpdatePolicy.Outcome.BLOCKED); expect(blocked.proposed().isEmpty());
        reject("BRANCH_UPDATE_CANDIDATE_REQUIRED",()->start.append(null,context("h4",3)));
        reject("HISTORY_CONTEXT_REQUIRED",()->start.append(update,null));
        reject("BRANCH_REGISTRATION_CANDIDATE_REQUIRED",()->BranchChangeHistory.start(null,context("h4",3)));
        reject("HISTORY_CONTEXT_REQUIRED",()->BranchChangeHistory.start(registration,null));
        immutable(()->second.entries().clear()); immutable(()->entry.fields().clear());
        expect(second.entries().size()==2); expect(entry.fields().size()==1);
        System.out.println("PASS: "+checks+" isolated branch change history assertions");
    }
    private static Branch branch(Company c,String id,String name) { return Branch.createCandidate(new BranchId(id),c.tenantId(),c,"code",name,null,null); }
    private static LifecycleHistory.Context context(String id,long seconds) { return new LifecycleHistory.Context(id,"actor",TIME.plusSeconds(seconds),"request-"+id); }
    private static BranchRegistration.Candidate register(Company c,CustomerUpdatePolicy.Current parent,Branch b) {
        return BranchRegistration.assess(b, ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy.lifecycleSnapshot(parent),CompanyBranches.empty(c),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(TENANT,BranchRegistration.Authority.REGISTER),BranchTypeCatalog.initial(),ContactTypeCatalog.empty(),Map.of()).proposed().orElseThrow();
    }
    private static BranchUpdatePolicy.Assessment assess(BranchUpdatePolicy.Current current,Branch b,CustomerUpdatePolicy.Current parent,CompanyBranches collection) {
        return BranchUpdatePolicy.assess(current,current.version(),b, ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy.lifecycleSnapshot(parent),collection,BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchUpdatePolicy.Context(TENANT,BranchUpdatePolicy.Authority.UPDATE,current.status()));
    }
    private static BranchUpdatePolicy.Candidate update(BranchUpdatePolicy.Current c,Branch b,CustomerUpdatePolicy.Current p,CompanyBranches collection) { return assess(c,b,p,collection).proposed().orElseThrow(); }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | NullPointerException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection: "+code);
    }
    private static void immutable(Runnable action) { try { action.run(); } catch (UnsupportedOperationException ex) { expect(true); return; } throw new AssertionError("Expected immutable collection"); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch change history assertion "+(checks+1)); checks++; }
}
