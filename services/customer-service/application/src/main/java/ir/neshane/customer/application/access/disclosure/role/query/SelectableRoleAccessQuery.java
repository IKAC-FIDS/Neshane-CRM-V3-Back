package ir.neshane.customer.application.access.disclosure.role.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.relationship.BusinessRoleCatalog;
import ir.neshane.customer.domain.relationship.RelationshipTarget;
import ir.neshane.customer.domain.shared.identifier.TenantId;

/**
 * Internal assessment of access to a selectable business role.
 */
public record SelectableRoleAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        TenantId targetTenant,
        BusinessRoleCatalog catalog,
        BusinessRoleCatalog.Ref reference,
        RelationshipTarget.Kind targetKind) {

    @Override
    public String toString() {
        return "SelectableRoleAccessQuery[redacted]";
    }
}
