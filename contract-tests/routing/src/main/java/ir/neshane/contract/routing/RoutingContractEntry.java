package ir.neshane.contract.routing;

import java.nio.file.Path;

record RoutingContractEntry(
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
