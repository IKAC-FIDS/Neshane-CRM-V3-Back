package ir.neshane.contract.routing;

import java.util.Objects;

record SourceRecordReference(
        String recordType,
        String recordId,
        long recordVersion,
        Long placementGeneration,
        String semanticDigest,
        AuthorityProvenance authority) {

    SourceRecordReference {
        if (recordType == null || recordType.isBlank()
                || recordId == null || recordId.isBlank()
                || semanticDigest == null || semanticDigest.isBlank()
                || recordVersion < 1) {
            throw new RoutingContractException("Invalid snapshot source record reference");
        }
        Objects.requireNonNull(authority, "authority");
    }
}
