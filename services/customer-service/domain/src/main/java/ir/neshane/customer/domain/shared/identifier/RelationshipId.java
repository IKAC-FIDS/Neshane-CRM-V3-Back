package ir.neshane.customer.domain.shared.identifier;

/** Opaque identity, independent of the participating entities. */
public record RelationshipId(String value) {
    public RelationshipId {
        IdentifierText.requireNonBlank(value, "RELATIONSHIP_ID_REQUIRED");
    }
}
