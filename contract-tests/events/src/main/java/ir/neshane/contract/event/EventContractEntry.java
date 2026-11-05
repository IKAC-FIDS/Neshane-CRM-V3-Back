package ir.neshane.contract.event;

import java.nio.file.Path;

record EventContractEntry(
        String type,
        String payloadVersion,
        Path schemaPath,
        String schemaId,
        String owner,
        String tenantMode,
        String status) {

    String key() {
        return type + "@" + payloadVersion;
    }
}
