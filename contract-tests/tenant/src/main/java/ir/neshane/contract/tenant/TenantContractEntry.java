package ir.neshane.contract.tenant;

import java.nio.file.Path;

record TenantContractEntry(
        String name,
        String version,
        Path schemaPath,
        String schemaId,
        String owner,
        String status) {

    String key() {
        return name + "@" + version;
    }
}
