package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.HashSet;
import java.util.Set;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;

final class LogicalAccessPolicySpecification {
    private static int checks;
    static void run() {
        var tenant=new TenantId("tenant");
        var foreign=new TenantId("foreign");
        var empty=new LogicalAccessPolicy.ActorContext("actor",tenant,Set.of());
        for (var operation : LogicalAccessPolicy.Operation.values()) {
            var request=new LogicalAccessPolicy.Request(tenant,operation);
            expect(LogicalAccessPolicy.assess(null,request).code().equals("ACCESS_ACTOR_CONTEXT_REQUIRED"));
            expect(LogicalAccessPolicy.assess(empty,request).outcome()==DENIED);
            var actor=new LogicalAccessPolicy.ActorContext("actor",tenant,Set.of(new LogicalAccessPolicy.Grant(tenant,operation)));
            expect(LogicalAccessPolicy.assess(actor,request).outcome()==ELIGIBLE_CANDIDATE);
            expect(LogicalAccessPolicy.assess(actor,request).code().equals("ACCESS_OPERATION_ELIGIBLE_CANDIDATE"));
            expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(foreign,operation)).code().equals("ACCESS_TENANT_MISMATCH"));
            var foreignGrant=new LogicalAccessPolicy.ActorContext("actor",tenant,Set.of(new LogicalAccessPolicy.Grant(foreign,operation)));
            expect(LogicalAccessPolicy.assess(foreignGrant,request).code().equals("ACCESS_OPERATION_NOT_GRANTED"));
            expect(LogicalAccessPolicy.assess(foreignGrant,new LogicalAccessPolicy.Request(foreign,operation)).outcome()==DENIED);
            for (var other : LogicalAccessPolicy.Operation.values())
                if (other!=operation) expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(tenant,other)).outcome()==DENIED);
        }
        var grants=new HashSet<LogicalAccessPolicy.Grant>();
        grants.add(new LogicalAccessPolicy.Grant(tenant,CUSTOMER_READ));
        grants.add(new LogicalAccessPolicy.Grant(tenant,BRANCH_READ));
        var actor=new LogicalAccessPolicy.ActorContext("private-actor",tenant,grants);
        grants.clear();
        expect(actor.grants().size()==2);
        expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(tenant,CUSTOMER_READ)).outcome()==ELIGIBLE_CANDIDATE);
        expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(tenant,BRANCH_READ)).outcome()==ELIGIBLE_CANDIDATE);
        expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(tenant,IDENTITY_SENSITIVE_READ)).outcome()==DENIED);
        expect(LogicalAccessPolicy.assess(actor,new LogicalAccessPolicy.Request(tenant,CUSTOMER_UPDATE)).outcome()==DENIED);
        expect(!actor.toString().contains("private-actor"));
        try { actor.grants().clear(); throw new AssertionError("Mutable grants"); }
        catch (UnsupportedOperationException expected) { checks++; }
        reject("ACCESS_ACTOR_REQUIRED",()->new LogicalAccessPolicy.ActorContext(" ",tenant,Set.of()));
        reject("ACCESS_ACTOR_TENANT_REQUIRED",()->new LogicalAccessPolicy.ActorContext("actor",null,Set.of()));
        reject("ACCESS_GRANTS_REQUIRED",()->new LogicalAccessPolicy.ActorContext("actor",tenant,null));
        reject("ACCESS_GRANT_TENANT_REQUIRED",()->new LogicalAccessPolicy.Grant(null,CUSTOMER_READ));
        reject("ACCESS_TARGET_TENANT_REQUIRED",()->new LogicalAccessPolicy.Request(null,CUSTOMER_READ));
        reject("ACCESS_OPERATION_REQUIRED",()->new LogicalAccessPolicy.Grant(tenant,null));
        reject("ACCESS_OPERATION_REQUIRED",()->new LogicalAccessPolicy.Request(tenant,null));
        reject("ACCESS_REQUEST_REQUIRED",()->LogicalAccessPolicy.assess(actor,null));
        System.out.println("PASS: "+checks+" isolated logical access foundation assertions");
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | NullPointerException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected "+code);
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Logical access assertion "+(checks+1)); checks++; }
}
