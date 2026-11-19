package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Operation.*;
import static ir.neshane.customer.domain.LogicalAccessPolicy.Outcome.*;
import static ir.neshane.customer.domain.AuditRequirementsPolicy.Reason.*;

final class AuditRequirementsPolicySpecification {
    private static int checks;
    static void run() {
        var ordinaryReads=Set.of(CUSTOMER_READ,BRANCH_READ,RELATIONSHIP_READ,DOCUMENT_READ,CATALOG_SELECTABLE_READ);
        var protectedReads=Set.of(IDENTITY_SENSITIVE_READ,DOCUMENT_CONTENT_READ,VERIFICATION_EVIDENCE_READ,HISTORY_READ,AUDIT_READ);
        var reasonOps=Set.of(RELATIONSHIP_END,DOCUMENT_INVALIDATE,IDENTITY_CORRECT,DUPLICATE_EXCEPTION,VERIFY_MANUAL,VERIFICATION_REVOKE);
        for (var op : Operation.values()) {
            var base=AuditRequirementsPolicy.requirement(op,ELIGIBLE_CANDIDATE,false,false);
            expect(base.auditRequired()!=ordinaryReads.contains(op));
            expect(base.reason()==(reasonOps.contains(op)?REQUIRED:ordinaryReads.contains(op)||protectedReads.contains(op)?NOT_APPLICABLE:OPTIONAL));
            for (var outcome : Outcome.values()) {
                for (boolean audit : new boolean[]{false,true}) {
                    for (boolean reason : new boolean[]{false,true}) {
                        var result=AuditRequirementsPolicy.requirement(op,outcome,audit,reason);
                        expect(result.auditRequired()==(outcome==DENIED||base.auditRequired()||audit||reason));
                        expect(result.reason()==(reason?REQUIRED:outcome==DENIED?NOT_APPLICABLE:base.reason()));
                    }
                }
            }
        }
        var tenant=new TenantId("private-tenant");
        var foreign=new TenantId("foreign");
        var actor=new ActorContext("private-actor",tenant,Set.of(new Grant(tenant,IDENTITY_SENSITIVE_READ)));
        var request=new Request(tenant,IDENTITY_SENSITIVE_READ);
        var access=LogicalAccessPolicy.assess(actor,request);
        var at=Instant.parse("2030-01-01T00:00:00Z");
        var metadata=AuditRequirementsPolicy.metadata(actor,request,access,at,"private-correlation");
        expect(metadata.actorId().orElseThrow().equals(actor.actorId()));
        expect(metadata.actorTenant().orElseThrow().equals(tenant));
        expect(metadata.targetTenant().equals(tenant));
        expect(metadata.operation()==IDENTITY_SENSITIVE_READ);
        expect(metadata.outcome()==ELIGIBLE_CANDIDATE);
        expect(metadata.occurredAt().equals(at));
        expect(metadata.correlationId().equals("private-correlation"));
        expect(!metadata.toString().contains("private"));
        var missing=AuditRequirementsPolicy.metadata(null,request,LogicalAccessPolicy.assess(null,request),at,"corr");
        expect(missing.actorId().isEmpty()); expect(missing.actorTenant().isEmpty()); expect(missing.outcome()==DENIED);
        var cross=new Request(foreign,IDENTITY_SENSITIVE_READ);
        var denied=AuditRequirementsPolicy.metadata(actor,cross,LogicalAccessPolicy.assess(actor,cross),at,"corr");
        expect(denied.actorTenant().orElseThrow().equals(tenant)); expect(denied.targetTenant().equals(foreign)); expect(denied.outcome()==DENIED);
        reject("AUDIT_ELIGIBILITY_SCOPE_INVALID",()->AuditRequirementsPolicy.metadata(actor,cross,access,at,"corr"));
        reject("AUDIT_ELIGIBILITY_SCOPE_INVALID",()->AuditRequirementsPolicy.metadata(null,request,access,at,"corr"));
        reject("AUDIT_ELIGIBILITY_GRANT_MISMATCH",()->AuditRequirementsPolicy.metadata(actor,new Request(tenant,CUSTOMER_READ),access,at,"corr"));
        reject("AUDIT_ACTOR_SCOPE_INCOMPLETE",()->new AuditRequirementsPolicy.Metadata(Optional.of("actor"),Optional.empty(),tenant,CUSTOMER_READ,DENIED,at,"corr"));
        reject("AUDIT_TIMESTAMP_REQUIRED",()->AuditRequirementsPolicy.metadata(actor,request,access,null,"corr"));
        reject("AUDIT_CORRELATION_REQUIRED",()->AuditRequirementsPolicy.metadata(actor,request,access,at," "));
        reject("AUDIT_ACCESS_ASSESSMENT_REQUIRED",()->AuditRequirementsPolicy.metadata(actor,request,null,at,"corr"));
        reject("AUDIT_OPERATION_REQUIRED",()->AuditRequirementsPolicy.requirement(null,DENIED,false,false));
        reject("AUDIT_OUTCOME_REQUIRED",()->AuditRequirementsPolicy.requirement(CUSTOMER_READ,null,false,false));
        var prior=RoleAuditPolicy.requirement(RoleAuditPolicy.Operation.SYSTEM_PUBLICATION,false);
        var combined=AuditRequirementsPolicy.requirement(CATALOG_MANAGE,ELIGIBLE_CANDIDATE,prior.auditRequired(),prior.reason()==RoleAuditPolicy.Reason.REQUIRED);
        expect(combined.auditRequired()); expect(combined.reason()==REQUIRED); expect(prior.releaseReferenceRequired());
        System.out.println("PASS: "+checks+" isolated audit requirement assertions");
    }
    private static void reject(String code,Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | NullPointerException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected "+code);
    }
    private static void expect(boolean value) { if (!value) throw new AssertionError("Audit requirement assertion "+(checks+1)); checks++; }
}
