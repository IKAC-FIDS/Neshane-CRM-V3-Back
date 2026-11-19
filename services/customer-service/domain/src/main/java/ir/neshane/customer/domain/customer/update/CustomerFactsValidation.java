package ir.neshane.customer.domain.customer.update;

import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.contact.ContactTypeCatalog;
import ir.neshane.customer.domain.identity.IdentifierTypeCatalog;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactGroup;
import ir.neshane.customer.domain.identity.IdentityInfo;
import ir.neshane.customer.domain.identity.IdentityIdentifier;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pure factual-change validation for explicitly supplied customer snapshots. */
public final class CustomerFactsValidation {
    private CustomerFactsValidation() {}

    public static Optional<String> validateChanges(Customer original,Customer proposed,ContactTypeCatalog contacts,
            IdentifierTypeCatalog identifiers,Map<String,ValidationProfile> contactProfiles,Map<String,ValidationProfile> identifierProfiles) {
        Objects.requireNonNull(contacts);
        Objects.requireNonNull(identifiers);
        try {
            var oldContacts = original.contactInfo();
            var newContacts = proposed.contactInfo();
            for (var old : oldContacts.points())
                if (newContacts.points().stream().noneMatch(p -> p.id().equals(old.id()))) return Optional.of("CUSTOMER_CONTACT_HISTORY_REQUIRED");
            for (var point : newContacts.points()) {
                var old = oldContacts.points().stream().filter(p -> p.id().equals(point.id())).findFirst();
                if (old.filter(point::equals).isPresent()) continue;
                if (point.status() == ContactPoint.Status.INACTIVE) {
                    if (old.filter(point::isInactiveVersionOf).isEmpty()) return Optional.of("CUSTOMER_CONTACT_INACTIVE_CHANGE_INVALID");
                    continue;
                }
                var others = new ContactInfo(newContacts.owner(),newContacts.points().stream()
                    .filter(p -> !p.id().equals(point.id()) && p.status() == ContactPoint.Status.ACTIVE).toList());
                var result = contacts.validateCandidate(others,point,contactProfiles.get(point.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_CONTACT_VALIDATION_"+result.status().name());
            }
            for (var group : ContactGroup.values()) {
                var primary = newContacts.primary(group);
                if (primary.isEmpty()) continue;
                var point = newContacts.requirePoint(primary.orElseThrow());
                if (point.status() != ContactPoint.Status.ACTIVE) return Optional.of("CUSTOMER_CONTACT_PRIMARY_MISMATCH");
                if (!primary.equals(oldContacts.primary(group)) || !oldContacts.points().contains(point)) {
                    var checked = contacts.selectPrimaryCandidate(new ContactInfo(newContacts.owner(),newContacts.points()),point.id());
                    if (!checked.primary(group).equals(primary)) return Optional.of("CUSTOMER_CONTACT_PRIMARY_MISMATCH");
                }
            }
            var oldIds = original.identityInfo();
            var newIds = proposed.identityInfo();
            for (var old : oldIds.identifiers())
                if (newIds.identifiers().stream().noneMatch(i -> i.id().equals(old.id()))) return Optional.of("CUSTOMER_IDENTIFIER_HISTORY_REQUIRED");
            for (var id : newIds.identifiers()) {
                var old = oldIds.identifiers().stream().filter(i -> i.id().equals(id.id())).findFirst();
                if (old.filter(id::equals).isPresent()) continue;
                if (id.status() == IdentityIdentifier.Status.INACTIVE) {
                    if (old.filter(i -> new IdentityIdentifier(i.id(),i.owner(),i.typeId(),i.value(),i.issuingCountry(),i.issuingAuthority(),
                            i.issueDate(),i.expiryDate(),IdentityIdentifier.Status.INACTIVE).equals(id)).isEmpty())
                        return Optional.of("CUSTOMER_IDENTIFIER_INACTIVE_CHANGE_INVALID");
                    continue;
                }
                var others = new IdentityInfo(newIds.owner(),newIds.identifiers().stream().filter(i -> !i.id().equals(id.id())).toList());
                var result = identifiers.validateCandidate(others,id,identifierProfiles.get(id.id()));
                if (result.status() != ValidationEngine.Status.PASS) return Optional.of("CUSTOMER_IDENTIFIER_VALIDATION_"+result.status().name());
            }
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.of(ex.getMessage());
        }
    }
}
