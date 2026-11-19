package ir.neshane.customer.domain.access;

import ir.neshane.customer.domain.relationship.BusinessRoleCatalog;
import ir.neshane.customer.domain.relationship.RelationshipAssignment;
import ir.neshane.customer.domain.relationship.RelationshipLifecycleIntegration;
import ir.neshane.customer.domain.relationship.RelationshipTarget;

import ir.neshane.customer.domain.customer.model.Customer;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import static ir.neshane.customer.domain.access.LogicalAccessPolicy.*;

/** Logical disclosure disposition only. No projection, masking algorithm or trusted grants. */
public final class ReadDisclosurePolicy {
    public enum IdentityFields { DENIED, MASK_REQUIRED, ELIGIBLE_CANDIDATE }
    private ReadDisclosurePolicy() {}

    public static IdentityFields identityFields(ActorContext actor, Customer customer) {
        if (CustomerBranchAccessPolicy.assessCustomer(actor,customer,Operation.CUSTOMER_READ).outcome()!=Outcome.ELIGIBLE_CANDIDATE)
            return IdentityFields.DENIED;
        return SensitiveAccessPolicy.assessIdentity(actor,customer,Operation.IDENTITY_SENSITIVE_READ).outcome()==Outcome.ELIGIBLE_CANDIDATE
            ? IdentityFields.ELIGIBLE_CANDIDATE : IdentityFields.MASK_REQUIRED;
    }

    /** Audit review never inherits business history or catalog authority. */
    public static Assessment audit(ActorContext actor,TenantId targetTenant) {
        if (targetTenant==null) return denied();
        return assess(actor,new Request(targetTenant,Operation.AUDIT_READ));
    }

    /** Only a tenant-visible, selectable candidate; return no definition or private catalog details. */
    public static Assessment selectableRole(ActorContext actor,TenantId tenant,BusinessRoleCatalog catalog,
            BusinessRoleCatalog.Ref ref,RelationshipTarget.Kind kind) {
        if (tenant==null || catalog==null || ref==null || kind==null) return denied();
        var grant=assess(actor,new Request(tenant,Operation.CATALOG_SELECTABLE_READ));
        if (grant.outcome()!=Outcome.ELIGIBLE_CANDIDATE) return grant;
        try { catalog.requireSelectable(tenant,ref,kind); }
        catch (IllegalArgumentException ex) { return denied(); }
        return grant;
    }

    /** Bind the read candidate to supplied party snapshots, including the exact branch parent. */
    public static Assessment relationship(ActorContext actor,RelationshipAssignment.Fact fact,
            RelationshipLifecycleIntegration.Parties parties) {
        if (fact==null || parties==null) return denied();
        var grant=assess(actor,new Request(fact.tenant(),Operation.RELATIONSHIP_READ));
        if (grant.outcome()!=Outcome.ELIGIBLE_CANDIDATE) return grant;
        // Scope validation deliberately ignores lifecycle eligibility: historical facts remain readable candidates.
        if (!RelationshipLifecycleIntegration.matchesScope(fact,parties)) return denied();
        return grant;
    }

    private static Assessment denied() { return new Assessment(Outcome.DENIED,"ACCESS_RESOURCE_UNAVAILABLE"); }
}
