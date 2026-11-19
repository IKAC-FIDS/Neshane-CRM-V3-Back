package ir.neshane.customer.domain;

import java.util.Objects;
import java.util.Optional;

/** Independent subordinate candidate; not a Customer or authorized registration. */
public final class Branch {
    private final BranchId id;
    private final TenantId tenantId;
    private final CustomerId companyId;
    private final Optional<String> code;
    private final String name;
    private final Optional<String> englishName;
    private final Optional<String> description;

    private Branch(BranchId id, TenantId tenantId, Company parent, String code,
                   String name, String englishName, String description) {
        this.id = Objects.requireNonNull(id, "BRANCH_ID_REQUIRED");
        this.tenantId = Objects.requireNonNull(tenantId, "TENANT_ID_REQUIRED");
        Objects.requireNonNull(parent, "BRANCH_PARENT_REQUIRED");
        if (!tenantId.equals(parent.tenantId())) throw new IllegalArgumentException("BRANCH_PARENT_TENANT_MISMATCH");
        if (parent.status() != CustomerStatus.ACTIVE) throw new IllegalArgumentException("BRANCH_PARENT_NOT_ACTIVE");
        this.companyId = parent.id();
        this.code = optionalRaw(code, "BRANCH_CODE_REQUIRED");
        this.name = CustomerNames.required(name, "BRANCH_NAME", 255);
        this.englishName = CustomerNames.optionalEnglishName(englishName);
        this.description = optionalRaw(description, "BRANCH_DESCRIPTION_REQUIRED");
    }

    public static Branch createCandidate(BranchId id, TenantId tenant, Company parent,
            String code, String name, String englishName, String description) {
        return new Branch(id, tenant, parent, code, name, englishName, description);
    }

    private static Optional<String> optionalRaw(String value, String error) {
        if (value == null) return Optional.empty();
        IdentityIdentifier.required(value, error);
        return Optional.of(value);
    }

    public BranchId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CustomerId companyId() { return companyId; }
    public Optional<String> code() { return code; }
    public String name() { return name; }
    public Optional<String> englishName() { return englishName; }
    public Optional<String> description() { return description; }
    public BranchStatus status() { return BranchStatus.ACTIVE; }
    @Override public String toString() { return "Branch[candidate, redacted]"; }
}
