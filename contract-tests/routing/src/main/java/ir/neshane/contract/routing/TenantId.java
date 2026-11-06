package ir.neshane.contract.routing;

record TenantId(String value) {
    TenantId {
        if (value == null || !value.matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            throw new RoutingContractException("Tenant ID must be a canonical lowercase UUIDv4");
        }
    }
}
