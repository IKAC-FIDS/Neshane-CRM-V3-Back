package ir.neshane.customer.domain;

import ir.neshane.customer.domain.branch.model.Branch;

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
import java.util.Map;
import java.util.Set;

final class BranchChangeEventDraftSpecification {
    private static int checks;
    private static final Instant TIME=Instant.parse("2026-01-01T00:00:00Z");
    static void run() {
        var tenant=new TenantId("tenant");
        var company=Company.createCandidate(new CustomerId("company"),tenant,"Company",null);
        var parent=parent(company);
        var branch=branch(company,"branch","Original");
        var registered=register(company,branch);
        var history=BranchChangeHistory.start(registered,context("h1",0));
        var initial=BranchChangeEventDraft.fromHistory(history);
        expect(initial.size()==1);
        var created=initial.getFirst();
        expect(created.eventType()==BranchChangeEventDraft.Type.BranchRegistered);
        expect(created.eventVersion()==1); expect(created.recordVersion()==1); expect(created.previousVersion().isEmpty());
        expect(created.tenantId().equals(tenant)); expect(created.companyId().equals(company.id())); expect(created.branchId().equals(branch.id()));
        expect(created.fields().isEmpty()); expect(created.historyId().equals("h1")); expect(created.actorId().equals("actor"));
        expect(created.correlationId().equals("request-h1")); expect(created.occurredAt().equals(TIME));
        expect(created.eventId().matches("[0-9a-f]{64}"));
        expect(created.eventId().equals(BranchChangeEventDraft.fromCandidate(history.entries().getFirst()).eventId()));
        var edited=branch(company,"branch","Changed");
        var update=assess(history.current(),edited,parent,registered.branches()).proposed().orElseThrow();
        var next=history.append(update,context("h2",1));
        var all=BranchChangeEventDraft.fromHistory(next);
        expect(all.size()==2); expect(initial.size()==1); expect(history.entries().size()==1);
        expect(all.getFirst().eventId().equals(created.eventId()));
        var changed=all.getLast();
        expect(changed.eventType()==BranchChangeEventDraft.Type.BranchUpdated); expect(changed.eventVersion()==1);
        expect(changed.previousVersion().orElseThrow()==1); expect(changed.recordVersion()==2);
        expect(changed.tenantId().equals(created.tenantId())); expect(changed.companyId().equals(created.companyId()));
        expect(changed.branchId().equals(created.branchId())); expect(changed.fields().equals(Set.of(BranchUpdatePolicy.Field.NAME)));
        expect(changed.historyId().equals("h2")); expect(changed.actorId().equals("actor"));
        expect(changed.correlationId().equals("request-h2")); expect(changed.occurredAt().equals(TIME.plusSeconds(1)));
        expect(!changed.eventId().equals(created.eventId()));
        expect(changed.eventId().equals(BranchChangeEventDraft.fromCandidate(next.entries().getLast()).eventId()));
        expect(!changed.eventId().equals(BranchChangeEventDraft.fromHistory(history.append(update,context("alternate",1))).getLast().eventId()));
        expect(!changed.toString().contains("Changed")); expect(!changed.toString().contains("actor")); expect(!changed.toString().contains("request-h2"));
        immutable(()->all.clear()); immutable(()->changed.fields().clear());
        expect(!created.eventId().equals(eventFor(company,"other","h1")));
        var otherCompany=Company.createCandidate(new CustomerId("other"),tenant,"Other",null);
        expect(!created.eventId().equals(eventFor(otherCompany,"branch","h1")));
        var otherTenant=Company.createCandidate(company.id(),new TenantId("other"),"Company",null);
        expect(!created.eventId().equals(eventFor(otherTenant,"branch","h1")));
        var noop=assess(next.current(),edited,parent,update.branches());
        expect(noop.outcome()==BranchUpdatePolicy.Outcome.UNCHANGED); expect(noop.proposed().isEmpty());
        var blocked=assess(next.current(),branch(company,"other","Other"),parent,update.branches());
        expect(blocked.outcome()==BranchUpdatePolicy.Outcome.BLOCKED); expect(blocked.proposed().isEmpty());
        expect(BranchChangeEventDraft.fromHistory(next).size()==2); expect(parent.version().value()==1);
        reject("HISTORY_ENTRY_REQUIRED",()->BranchChangeEventDraft.fromCandidate(null));
        reject("BRANCH_HISTORY_REQUIRED",()->BranchChangeEventDraft.fromHistory(null));
        System.out.println("PASS: "+checks+" isolated branch change event assertions");
    }
    private static CustomerUpdatePolicy.Current parent(Company c) { return new CustomerUpdatePolicy.Current(IdentityRevision.snapshot(c,"r1",VerificationStatus.VERIFIED),CustomerStatus.ACTIVE,CustomerRecordVersion.initial(c)); }
    private static Branch branch(Company c,String id,String name) { return Branch.createCandidate(new BranchId(id),c.tenantId(),c,"code",name,null,null); }
    private static LifecycleHistory.Context context(String id,long seconds) { return new LifecycleHistory.Context(id,"actor",TIME.plusSeconds(seconds),"request-"+id); }
    private static BranchRegistration.Candidate register(Company c,Branch b) {
        return BranchRegistration.assess(b,parent(c),CompanyBranches.empty(c),BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchRegistration.Context(c.tenantId(),BranchRegistration.Authority.REGISTER),BranchTypeCatalog.initial(),ContactTypeCatalog.empty(),Map.of()).proposed().orElseThrow();
    }
    private static String eventFor(Company c,String id,String historyId) { return BranchChangeEventDraft.fromHistory(BranchChangeHistory.start(register(c,branch(c,id,"Original")),context(historyId,0))).getFirst().eventId(); }
    private static BranchUpdatePolicy.Assessment assess(BranchUpdatePolicy.Current current,Branch b,CustomerUpdatePolicy.Current parent,CompanyBranches collection) {
        return BranchUpdatePolicy.assess(current,current.version(),b,parent,collection,BranchRegistration.Coverage.COMPLETE_CANDIDATE,
            new BranchUpdatePolicy.Context(b.tenantId(),BranchUpdatePolicy.Authority.UPDATE,current.status()));
    }
    private static void reject(String code,Runnable action) { try { action.run(); } catch (NullPointerException ex) { expect(code.equals(ex.getMessage())); return; } throw new AssertionError("Expected rejection: "+code); }
    private static void immutable(Runnable action) { try { action.run(); } catch (UnsupportedOperationException ex) { expect(true); return; } throw new AssertionError("Expected immutable collection"); }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Branch change event assertion "+(checks+1)); checks++; }
}
