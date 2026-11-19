package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import static ir.neshane.customer.domain.LogicalAccessPolicy.*;

/** Additive logical audit obligations; not a durable audit record or proof of action execution. */
public final class AuditRequirementsPolicy {
    /** Required logical information vocabulary, not a storage schema or payload. */
    public enum Information {
        EVENT_ID, APPLICABLE_TENANT, ACTOR_ID_AND_TYPE, RESOURCE_TYPE_AND_AVAILABLE_ID,
        OPERATION, APPLICABLE_OLD_NEW_VALUES, REQUIRED_OR_PROVIDED_REASON,
        TIMESTAMP, OUTCOME, CORRELATION_ID, APPLICABLE_EVIDENCE_REFERENCE
    }
    public static java.util.Set<Information> logicalInformation() {
        return java.util.Set.copyOf(java.util.EnumSet.allOf(Information.class));
    }
    public enum Reason { NOT_APPLICABLE, OPTIONAL, REQUIRED }
    public record Requirement(boolean auditRequired,Reason reason) {}
    public record Metadata(Optional<String> actorId,Optional<TenantId> actorTenant,TenantId targetTenant,
            Operation operation,Outcome outcome,Instant occurredAt,String correlationId) {
        public Metadata {
            Objects.requireNonNull(actorId,"AUDIT_ACTOR_OPTIONAL_REQUIRED");
            Objects.requireNonNull(actorTenant,"AUDIT_ACTOR_TENANT_OPTIONAL_REQUIRED");
            if (actorId.isPresent()!=actorTenant.isPresent()) throw new IllegalArgumentException("AUDIT_ACTOR_SCOPE_INCOMPLETE");
            actorId.ifPresent(id->IdentifierText.requireNonBlank(id,"AUDIT_ACTOR_REQUIRED"));
            Objects.requireNonNull(targetTenant,"AUDIT_TARGET_TENANT_REQUIRED");
            Objects.requireNonNull(operation,"AUDIT_OPERATION_REQUIRED");
            Objects.requireNonNull(outcome,"AUDIT_OUTCOME_REQUIRED");
            if (outcome==Outcome.ELIGIBLE_CANDIDATE && (actorTenant.isEmpty() || !actorTenant.orElseThrow().equals(targetTenant)))
                throw new IllegalArgumentException("AUDIT_ELIGIBILITY_SCOPE_INVALID");
            Objects.requireNonNull(occurredAt,"AUDIT_TIMESTAMP_REQUIRED");
            IdentifierText.requireNonBlank(correlationId,"AUDIT_CORRELATION_REQUIRED");
        }
        @Override public String toString() { return "AuditRequirementsPolicy.Metadata[redacted]"; }
    }
    private AuditRequirementsPolicy() {}

    /** Prior domain obligations can strengthen, but never relax, this policy. */
    public static Requirement requirement(Operation operation,Outcome outcome,boolean domainAuditRequired,boolean domainReasonRequired) {
        Objects.requireNonNull(operation,"AUDIT_OPERATION_REQUIRED");
        Objects.requireNonNull(outcome,"AUDIT_OUTCOME_REQUIRED");
        if (outcome==Outcome.DENIED)
            return new Requirement(true,domainReasonRequired?Reason.REQUIRED:Reason.NOT_APPLICABLE);
        var base=switch (operation) {
            case CUSTOMER_READ, BRANCH_READ, RELATIONSHIP_READ, DOCUMENT_READ, CATALOG_SELECTABLE_READ ->
                new Requirement(false,Reason.NOT_APPLICABLE);
            case IDENTITY_SENSITIVE_READ, DOCUMENT_CONTENT_READ, VERIFICATION_EVIDENCE_READ, HISTORY_READ, AUDIT_READ ->
                new Requirement(true,Reason.NOT_APPLICABLE);
            case RELATIONSHIP_END, DOCUMENT_INVALIDATE, IDENTITY_CORRECT, DUPLICATE_EXCEPTION, VERIFY_MANUAL, VERIFICATION_REVOKE ->
                new Requirement(true,Reason.REQUIRED);
            default -> new Requirement(true,Reason.OPTIONAL);
        };
        return new Requirement(base.auditRequired() || domainAuditRequired || domainReasonRequired,
            domainReasonRequired?Reason.REQUIRED:base.reason());
    }

    /** Access outcome only; no evidence, document contents, free-text reason or grant payload. */
    public static Metadata metadata(ActorContext actor,Request request,Assessment access,Instant occurredAt,String correlationId) {
        Objects.requireNonNull(request,"ACCESS_REQUEST_REQUIRED");
        Objects.requireNonNull(access,"AUDIT_ACCESS_ASSESSMENT_REQUIRED");
        if (access.outcome()==Outcome.ELIGIBLE_CANDIDATE && actor!=null && actor.tenant().equals(request.tenant()) &&
            assess(actor,request).outcome()!=Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException("AUDIT_ELIGIBILITY_GRANT_MISMATCH");
        return new Metadata(Optional.ofNullable(actor).map(ActorContext::actorId),Optional.ofNullable(actor).map(ActorContext::tenant),
            request.tenant(),request.operation(),access.outcome(),occurredAt,correlationId);
    }
}
