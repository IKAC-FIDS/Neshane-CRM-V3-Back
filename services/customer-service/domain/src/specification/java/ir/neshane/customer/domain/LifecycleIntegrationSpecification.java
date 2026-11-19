package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;
import ir.neshane.customer.domain.verification.VerificationStatus;

import ir.neshane.customer.domain.contact.ContactOwner;
import ir.neshane.customer.domain.contact.ContactInfo;
import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.identity.IdentityInfo;

import ir.neshane.customer.domain.contact.ContactValue;
import ir.neshane.customer.domain.contact.ContactTypeRef;
import ir.neshane.customer.domain.identity.IdentityOwner;
import ir.neshane.customer.domain.identity.IdentityIdentifier;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.customer.CustomerLifecycle.Outcome.*;

/** Cross-component synthetic candidate tests, not runtime integration. */
final class LifecycleIntegrationSpecification {
    private static int checks;

    static void run() {
        for (boolean company : List.of(false, true)) {
            var tenant = new TenantId("synthetic-tenant");
            var id = new CustomerId("synthetic-customer");
            var contactOwner = new ContactOwner(tenant, company ? ContactOwner.Kind.COMPANY : ContactOwner.Kind.PERSON, id.value());
            var identityOwner = new IdentityOwner(tenant, id, company ? IdentityOwner.Kind.COMPANY : IdentityOwner.Kind.PERSON);
            var contact = new ContactPoint("point", contactOwner, ContactTypeRef.system("email"),
                new ContactValue.Email("synthetic@example.invalid"));
            var identifier = new IdentityIdentifier("identifier", identityOwner, "type", "synthetic",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), IdentityIdentifier.Status.ACTIVE);
            var contacts = new ContactInfo(contactOwner, List.of(contact));
            var identities = new IdentityInfo(identityOwner, List.of(identifier));
            Customer customer = company ? Company.createCandidate(id, tenant, "Example", null, contacts, identities)
                : Person.createCandidate(id, tenant, "Example", "Person", null, contacts, identities);
            var reasons = LifecycleReasonCatalog.initial();
            var history = LifecycleHistory.empty(customer);
            var context = new LifecycleHistory.Context("history", "actor", Instant.EPOCH, "correlation");
            var deps = ArchiveDependencyCatalog.initial();
            var request = new ArchiveDependencyAssessment.Request(tenant, id, "request");
            var evidence = deps.definitions().values().stream()
                .filter(d -> company || !d.code().equals("ACTIVE_BRANCHES"))
                .map(d -> new ArchiveDependencyAssessment.Evidence(request, d.code(), d.ownerService(),
                    ArchiveDependencyAssessment.Status.CLEARED, "evidence-" + d.code(), Instant.EPOCH)).toList();
            expect(ArchiveDependencyAssessment.assess(customer, request, deps, evidence).outcome()
                == ArchiveDependencyAssessment.Outcome.ALL_CLEARED_CANDIDATE);
            expect(ArchiveDependencyAssessment.assess(customer, request, deps, List.of()).outcome()
                == ArchiveDependencyAssessment.Outcome.BLOCKED);
            expect(reasons.assess(tenant, customer.status(), ARCHIVE, true, "CUSTOMER_REQUEST", null)
                .transition().outcome() == BLOCKED);
            reject("CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED", () -> history.appendCandidate(customer, reasons,
                ARCHIVE, true, "CUSTOMER_REQUEST", null, context));
            expect(history.entries().isEmpty());
            expect(reasons.assess(tenant, customer.status(), DEACTIVATE, true, "CUSTOMER_REQUEST", null)
                .transition().outcome() == ELIGIBLE_CANDIDATE);
            var proposed = history.appendCandidate(customer, reasons, DEACTIVATE, true, "CUSTOMER_REQUEST", null, context);
            var entry = proposed.entries().getFirst();
            var event = CustomerEventDraft.fromCandidate(entry, "event", 2, Optional.empty());
            expect(event.tenantId().equals(tenant));
            expect(event.aggregateId().equals(id));
            expect(event.payload().previousStatus().orElseThrow() == CustomerStatus.ACTIVE);
            expect(event.payload().newStatus() == CustomerStatus.INACTIVE);
            expect(event.correlationId().equals(context.correlationId()));
            expect(customer.status() == CustomerStatus.ACTIVE);
            expect(customer.verificationStatus() == VerificationStatus.NOT_VERIFIED);
            expect(customer.contactInfo().equals(contacts));
            expect(customer.identityInfo().equals(identities));
            expect(history.entries().isEmpty());
            expect(entry.reason().title().exact("fa").isPresent());
            var hidden = reasons.withHidden(tenant, true, "CUSTOMER_REQUEST", true);
            reject("LIFECYCLE_REASON_NOT_SELECTABLE", () -> history.appendCandidate(customer, hidden,
                DEACTIVATE, true, "CUSTOMER_REQUEST", null, context));
            var other = new TenantId("other");
            Customer otherCustomer = company ? Company.createCandidate(id, other, "Example", null)
                : Person.createCandidate(id, other, "Example", "Person", null);
            expect(hidden.assess(other, CustomerStatus.ACTIVE, DEACTIVATE, true, "CUSTOMER_REQUEST", null)
                .transition().outcome() == ELIGIBLE_CANDIDATE);
            reject("HISTORY_CUSTOMER_SCOPE_MISMATCH", () -> history.appendCandidate(otherCustomer, hidden,
                DEACTIVATE, true, "CUSTOMER_REQUEST", null, context));
            reject("ARCHIVE_REQUEST_SCOPE_MISMATCH", () -> ArchiveDependencyAssessment.assess(otherCustomer,
                request, deps, evidence));
            expect(reasons.assess(tenant, CustomerStatus.INACTIVE, REACTIVATE, true, null, null).transition().outcome() == BLOCKED);
            expect(reasons.assess(tenant, CustomerStatus.ARCHIVED, RESTORE, true, "ARCHIVE_CORRECTION", null)
                .transition().outcome() == BLOCKED);
            expect(reasons.assess(tenant, CustomerStatus.ARCHIVED, REACTIVATE, true, null, null).transition().outcome() == REJECTED);
        }
        System.out.println("PASS: " + checks + " integrated isolated lifecycle assertions");
    }

    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Integrated lifecycle assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
