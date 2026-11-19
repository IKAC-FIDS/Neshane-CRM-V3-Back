package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.Objects;
import java.util.Optional;

/** Typed target reference, not an authorization or existence assertion. */
public record RelationshipTarget(TenantId tenantId, Kind kind, String id,
                                 Optional<CustomerId> parentCompanyId) {
    public enum Kind { COMPANY, BRANCH }

    public RelationshipTarget {
        Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(kind, "RELATIONSHIP_TARGET_KIND_REQUIRED");
        IdentifierText.requireNonBlank(id, "RELATIONSHIP_TARGET_ID_REQUIRED");
        Objects.requireNonNull(parentCompanyId, "RELATIONSHIP_PARENT_OPTIONAL_REQUIRED");
        if ((kind == Kind.BRANCH) != parentCompanyId.isPresent())
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_PARENT_MISMATCH");
    }

    static RelationshipTarget of(Company company) {
        Objects.requireNonNull(company, "RELATIONSHIP_TARGET_REQUIRED");
        return new RelationshipTarget(company.tenantId(), Kind.COMPANY, company.id().value(), Optional.empty());
    }

    static RelationshipTarget of(Branch branch) {
        Objects.requireNonNull(branch, "RELATIONSHIP_TARGET_REQUIRED");
        return new RelationshipTarget(branch.tenantId(), Kind.BRANCH, branch.id().value(), Optional.of(branch.companyId()));
    }
}
