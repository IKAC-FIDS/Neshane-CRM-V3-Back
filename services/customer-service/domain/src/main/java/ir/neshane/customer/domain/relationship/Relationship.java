package ir.neshane.customer.domain.relationship;

import ir.neshane.customer.domain.branch.lifecycle.BranchLifecycle;

import ir.neshane.customer.domain.branch.model.Branch;
import ir.neshane.customer.domain.branch.model.BranchStatus;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;
import ir.neshane.customer.domain.shared.identifier.RelationshipId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** One business role per independent candidate. No catalog, overlap or authorization clearance implied. */
public final class Relationship {
    public enum Status { ACTIVE, ENDED }
    private final RelationshipId id;
    private final TenantId tenantId;
    private final CustomerId personId;
    private final RelationshipTarget target;
    private final String roleCode;
    private final LocalDate startDate;
    private final Optional<LocalDate> endDate;

    private Relationship(RelationshipId id, Person person, RelationshipTarget target,
                         String roleCode, LocalDate startDate, LocalDate endDate) {
        this.id = Objects.requireNonNull(id, "RELATIONSHIP_ID_REQUIRED");
        Objects.requireNonNull(person, "RELATIONSHIP_PERSON_REQUIRED");
        this.tenantId = person.tenantId();
        this.personId = person.id();
        this.target = target;
        if (!tenantId.equals(target.tenantId()))
            throw new IllegalArgumentException("RELATIONSHIP_TENANT_MISMATCH");
        if (person.status() != CustomerStatus.ACTIVE)
            throw new IllegalArgumentException("RELATIONSHIP_PERSON_NOT_ACTIVE");
        this.roleCode = BusinessRoleDefinition.canonicalCode(roleCode);
        this.startDate = Objects.requireNonNull(startDate, "RELATIONSHIP_START_REQUIRED");
        RelationshipTemporalPolicy.requireInterval(startDate,Optional.ofNullable(endDate));
        this.endDate = Optional.ofNullable(endDate);
    }

    public static Relationship createCandidate(RelationshipId id, Person person, Company company,
                                               String roleCode, LocalDate start, LocalDate end) {
        var target = RelationshipTarget.of(company);
        if (company.status() != CustomerStatus.ACTIVE)
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_NOT_ACTIVE");
        return new Relationship(id, person, target, roleCode, start, end);
    }

    public static Relationship createCandidate(RelationshipId id, Person person, Branch branch, Company parent,
                                               String roleCode, LocalDate start, LocalDate end) {
        var target = RelationshipTarget.of(branch);
        if (BranchLifecycle.effectiveStatus(branch, parent) != BranchStatus.ACTIVE)
            throw new IllegalArgumentException("RELATIONSHIP_TARGET_NOT_ACTIVE");
        return new Relationship(id, person, target, roleCode, start, end);
    }

    public RelationshipId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CustomerId personId() { return personId; }
    public RelationshipTarget target() { return target; }
    public String roleCode() { return roleCode; }
    public LocalDate startDate() { return startDate; }
    public Optional<LocalDate> endDate() { return endDate; }
    public Status status() { return Status.ACTIVE; }
    @Override public String toString() { return "Relationship[candidate, redacted]"; }
}
