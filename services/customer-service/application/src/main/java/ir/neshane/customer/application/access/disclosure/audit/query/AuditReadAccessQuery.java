package ir.neshane.customer.application.access.disclosure.audit.query;

import ir.neshane.customer.domain.access.LogicalAccessPolicy;
import ir.neshane.customer.domain.shared.identifier.TenantId;

/**
 * Internal synthetic audit read assessment.
 */
public record AuditReadAccessQuery(
        LogicalAccessPolicy.ActorContext actor,
        TenantId targetTenant) {

    @Override
    public String toString() {
        return "AuditReadAccessQuery[redacted]";
    }
}
