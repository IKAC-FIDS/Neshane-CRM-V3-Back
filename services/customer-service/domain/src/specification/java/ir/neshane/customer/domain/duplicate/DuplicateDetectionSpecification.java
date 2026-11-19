package ir.neshane.customer.domain.duplicate;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.Customer;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.identity.IdentityOwner;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.util.List;
import java.util.Set;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Operation.*;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Coverage.*;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Outcome.*;
import static ir.neshane.customer.domain.duplicate.DuplicateDetection.Signal.*;

public final class DuplicateDetectionSpecification {
    private static int checks;
    public static void run() {
        var tenant = new TenantId("a");
        var foreign = new TenantId("b");
        var person = Person.createCandidate(new CustomerId("person"), tenant, "A", "B", null);
        var company = Company.createCandidate(new CustomerId("company"), tenant, "Company", null);
        var request = DuplicateDetection.Request.of(person, CREATE, "request");
        expect(request.kind() == IdentityOwner.Kind.PERSON);
        expect(DuplicateDetection.Request.of(company, CREATE, "company-request").kind() == IdentityOwner.Kind.COMPANY);
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of()).outcome() == NO_MATCH);
        expect(assess(person, request, UNKNOWN, List.of()).outcome() == BLOCKED);
        var noMatch = comparison(request, tenant, "other", Set.of());
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(noMatch)).outcome() == NO_MATCH);
        for (var signal : List.of(UNVERIFIED_IDENTIFIER, NAME, PHONE, EMAIL)) {
            expect(assess(person, request, COMPLETE_CANDIDATE, List.of(comparison(request, tenant, "other", Set.of(signal)))).outcome() == POTENTIAL_DUPLICATE);
        }
        var definite = comparison(request, tenant, "protected-id", Set.of(VERIFIED_VALID_IDENTIFIER));
        var potential = comparison(request, tenant, "another", Set.of(NAME, PHONE));
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(definite, potential)).outcome() == DEFINITE_CONFLICT);
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(potential, definite)).outcome() == DEFINITE_CONFLICT);
        expect(!assess(person, request, COMPLETE_CANDIDATE, List.of(definite)).toString().contains("protected-id"));
        expect(!definite.toString().contains("protected-id"));
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(definite, definite)).code().equals("DUPLICATE_EVIDENCE_DUPLICATE"));
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(comparison(request, foreign, "foreign", Set.of(NAME)))).code().equals("DUPLICATE_EVIDENCE_SCOPE_MISMATCH"));
        var otherRequest = DuplicateDetection.Request.of(person, CREATE, "other-request");
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(comparison(otherRequest, tenant, "other", Set.of(NAME)))).outcome() == BLOCKED);
        expect(assess(company, request, COMPLETE_CANDIDATE, List.of()).code().equals("DUPLICATE_REQUEST_SCOPE_MISMATCH"));
        expect(assess(person, request, COMPLETE_CANDIDATE, List.of(comparison(request, tenant, "person", Set.of()))).code().equals("DUPLICATE_CUSTOMER_ID_ALREADY_EXISTS"));
        var update = DuplicateDetection.Request.of(person, UPDATE, "update");
        var self = comparison(update, tenant, "person", Set.of(VERIFIED_VALID_IDENTIFIER));
        expect(assess(person, update, COMPLETE_CANDIDATE, List.of(self)).outcome() == NO_MATCH);
        expect(assess(person, update, COMPLETE_CANDIDATE, List.of(self, comparison(update, tenant, "other", Set.of(VERIFIED_VALID_IDENTIFIER)))).outcome() == DEFINITE_CONFLICT);
        var wrongKind = new DuplicateDetection.Comparison(update, tenant, person.id(), IdentityOwner.Kind.COMPANY, Set.of());
        expect(assess(person, update, COMPLETE_CANDIDATE, List.of(wrongKind)).code().equals("DUPLICATE_EVIDENCE_KIND_MISMATCH"));
        var companyRequest = DuplicateDetection.Request.of(company, CREATE, "company-request");
        var sharedPhone = new DuplicateDetection.Comparison(companyRequest, tenant, person.id(), IdentityOwner.Kind.PERSON, Set.of(PHONE));
        expect(assess(company, companyRequest, COMPLETE_CANDIDATE, List.of(sharedPhone)).outcome() == POTENTIAL_DUPLICATE);
        expect(person.identityInfo().identifiers().isEmpty());
        expect(company.contactInfo().points().isEmpty());
        System.out.println("PASS: " + checks + " isolated duplicate foundation assertions");
    }
    private static DuplicateDetection.Comparison comparison(DuplicateDetection.Request request, TenantId tenant, String id, Set<DuplicateDetection.Signal> signals) {
        return new DuplicateDetection.Comparison(request, tenant, new CustomerId(id), IdentityOwner.Kind.PERSON, signals);
    }
    private static DuplicateDetection.Assessment assess(Customer customer, DuplicateDetection.Request request, DuplicateDetection.Coverage coverage,
            List<DuplicateDetection.Comparison> comparisons) {
        return DuplicateDetection.assess(customer, request, coverage, comparisons);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Duplicate foundation assertion " + (checks + 1));
        checks++;
    }
}
