package ir.neshane.customer.domain;

/** Opaque identity, independent of the participating entities. */
public record RelationshipId(String value) {
    public RelationshipId {
        IdentityIdentifier.required(value, "RELATIONSHIP_ID_REQUIRED");
    }
}
