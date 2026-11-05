package ir.neshane.contract.tenant;

import java.util.HashMap;
import java.util.Map;

final class TenantRegistryPolicy {

    enum RegistryStatus {
        ACTIVE,
        TOMBSTONED
    }

    record RegistryRecord(
            TenantId tenantId,
            DeploymentProfile profile,
            long registryVersion,
            RegistryStatus status,
            AuthorityProvenance authority) {}

    private final Map<TenantId, RegistryRecord> records = new HashMap<>();

    RegistryRecord register(
            TenantId tenantId,
            DeploymentProfile profile,
            AuthorityProvenance authority) {
        AuthorityPolicy.require(profile, AuthorityScope.REGISTRY, authority);
        if (records.containsKey(tenantId)) {
            throw new TenantContractException("Tenant ID collision or prohibited reuse");
        }
        RegistryRecord record = new RegistryRecord(
                tenantId, profile, 1, RegistryStatus.ACTIVE, authority);
        records.put(tenantId, record);
        return record;
    }

    RegistryRecord tombstone(TenantId tenantId, AuthorityProvenance authority) {
        RegistryRecord current = required(tenantId);
        AuthorityPolicy.require(current.profile(), AuthorityScope.REGISTRY, authority);
        if (current.status() == RegistryStatus.TOMBSTONED) {
            return current;
        }
        RegistryRecord tombstone = new RegistryRecord(
                tenantId,
                current.profile(),
                current.registryVersion() + 1,
                RegistryStatus.TOMBSTONED,
                authority);
        records.put(tenantId, tombstone);
        return tombstone;
    }

    RegistryRecord required(TenantId tenantId) {
        RegistryRecord record = records.get(tenantId);
        if (record == null) {
            throw new TenantContractException("Tenant ID is not registered");
        }
        return record;
    }
}
