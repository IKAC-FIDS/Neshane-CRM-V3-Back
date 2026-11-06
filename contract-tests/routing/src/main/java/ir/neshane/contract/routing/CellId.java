package ir.neshane.contract.routing;

record CellId(String value) {
    CellId {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new RoutingContractException("Cell ID must be a non-empty opaque identifier");
        }
    }
}
