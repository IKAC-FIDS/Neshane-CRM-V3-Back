package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Set;

/** Pure candidate authorization assessment; supplied grants are not trusted credentials. */
public final class LogicalAccessPolicy {
    public enum Operation {
        CUSTOMER_READ, CUSTOMER_CREATE, CUSTOMER_UPDATE, CUSTOMER_LIFECYCLE,
        BRANCH_READ, BRANCH_CREATE, BRANCH_UPDATE, BRANCH_LIFECYCLE,
        RELATIONSHIP_READ, RELATIONSHIP_ASSIGN, RELATIONSHIP_END,
        DOCUMENT_READ, DOCUMENT_UPLOAD, DOCUMENT_REPLACE, DOCUMENT_INVALIDATE,
        IDENTITY_SENSITIVE_READ, DOCUMENT_CONTENT_READ, VERIFICATION_EVIDENCE_READ,
        IDENTITY_CORRECT, DUPLICATE_EXCEPTION, VERIFY_MANUAL, VERIFICATION_REVOKE,
        CATALOG_MANAGE, HISTORY_READ
    }
    public enum Outcome { ELIGIBLE_CANDIDATE, DENIED }
    public record Grant(TenantId tenant,Operation operation) {
        public Grant {
            Objects.requireNonNull(tenant,"ACCESS_GRANT_TENANT_REQUIRED");
            Objects.requireNonNull(operation,"ACCESS_OPERATION_REQUIRED");
        }
    }
    public record ActorContext(String actorId,TenantId tenant,Set<Grant> grants) {
        public ActorContext {
            IdentityIdentifier.required(actorId,"ACCESS_ACTOR_REQUIRED");
            Objects.requireNonNull(tenant,"ACCESS_ACTOR_TENANT_REQUIRED");
            grants=Set.copyOf(Objects.requireNonNull(grants,"ACCESS_GRANTS_REQUIRED"));
        }
        @Override public String toString() { return "LogicalAccessPolicy.ActorContext[redacted]"; }
    }
    public record Request(TenantId tenant,Operation operation) {
        public Request {
            Objects.requireNonNull(tenant,"ACCESS_TARGET_TENANT_REQUIRED");
            Objects.requireNonNull(operation,"ACCESS_OPERATION_REQUIRED");
        }
    }
    public record Assessment(Outcome outcome,String code) {}
    private LogicalAccessPolicy() {}

    /** A grant must match the exact requested operation and active tenant. No implicit inheritance. */
    public static Assessment assess(ActorContext actor,Request request) {
        Objects.requireNonNull(request,"ACCESS_REQUEST_REQUIRED");
        if (actor==null) return denied("ACCESS_ACTOR_CONTEXT_REQUIRED");
        if (!actor.tenant().equals(request.tenant())) return denied("ACCESS_TENANT_MISMATCH");
        if (!actor.grants().contains(new Grant(request.tenant(),request.operation())))
            return denied("ACCESS_OPERATION_NOT_GRANTED");
        return new Assessment(Outcome.ELIGIBLE_CANDIDATE,"ACCESS_OPERATION_ELIGIBLE_CANDIDATE");
    }
    private static Assessment denied(String code) { return new Assessment(Outcome.DENIED,code); }
}
