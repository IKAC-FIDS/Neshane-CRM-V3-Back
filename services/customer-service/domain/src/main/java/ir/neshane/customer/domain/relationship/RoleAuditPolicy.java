package ir.neshane.customer.domain.relationship;

/** Logical governance obligations only; no durable recording or actor validation. */
public final class RoleAuditPolicy {
    public enum Operation { SYSTEM_CREATE, TENANT_CREATE, METADATA_CHANGE, SCOPE_CHANGE,
        STATUS_CHANGE, HIDE, UNHIDE, SYSTEM_PUBLICATION, UNAUTHORIZED_MODIFICATION, READ }
    public enum Reason { OPTIONAL, REQUIRED, NOT_APPLICABLE }
    public record Requirement(boolean auditRequired, Reason reason, boolean releaseReferenceRequired) {}
    private RoleAuditPolicy() {}
    public static Requirement requirement(Operation operation, boolean incompatibleScope) {
        return switch (operation) {
            case READ -> new Requirement(false, Reason.NOT_APPLICABLE, false);
            case UNAUTHORIZED_MODIFICATION -> new Requirement(true, Reason.NOT_APPLICABLE, false);
            case SYSTEM_PUBLICATION -> new Requirement(true, Reason.REQUIRED, true);
            case SCOPE_CHANGE -> new Requirement(true, incompatibleScope ? Reason.REQUIRED : Reason.OPTIONAL, false);
            default -> new Requirement(true, Reason.OPTIONAL, false);
        };
    }
}
