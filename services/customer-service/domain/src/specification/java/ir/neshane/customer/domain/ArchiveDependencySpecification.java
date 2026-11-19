package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Company;
import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Person;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ir.neshane.customer.domain.ArchiveDependencyAssessment.Status.*;
import static ir.neshane.customer.domain.ArchiveDependencyAssessment.Outcome.ALL_CLEARED_CANDIDATE;

final class ArchiveDependencySpecification {
    private static int checks;
    static void run() {
        var tenant = new TenantId("synthetic");
        var id = new CustomerId("customer");
        var company = Company.createCandidate(id, tenant, "Example", null);
        var person = Person.createCandidate(id, tenant, "Example", "Person", null);
        var catalog = ArchiveDependencyCatalog.initial();
        var request = new ArchiveDependencyAssessment.Request(tenant, id, "request-1");
        expect(catalog.definitions().size() == 10);
        expect(catalog.definitions().get("ACTIVE_BRANCHES").applicableCustomerTypes()
            .equals(Set.of(ArchiveDependencyCatalog.CustomerKind.COMPANY)));
        for (var def : catalog.definitions().values()) {
            expect(def.title().exact("fa").isPresent());
            expect(def.title().exact("en").isPresent());
        }
        var complete = catalog.definitions().values().stream()
            .map(d -> evidence(request, d, CLEARED)).toList();
        expect(ArchiveDependencyAssessment.assess(company, request, catalog, complete).outcome() == ALL_CLEARED_CANDIDATE);
        var empty = ArchiveDependencyAssessment.assess(company, request, catalog, List.of());
        expect(empty.outcome() == ArchiveDependencyAssessment.Outcome.BLOCKED);
        expect(empty.statuses().size() == 10);
        expect(empty.statuses().values().stream().allMatch(s -> s == UNKNOWN));
        var branchless = complete.stream().filter(e -> !e.dependencyCode().equals("ACTIVE_BRANCHES")).toList();
        expect(ArchiveDependencyAssessment.assess(company, request, catalog, branchless).outcome() != ALL_CLEARED_CANDIDATE);
        var personResult = ArchiveDependencyAssessment.assess(person, request, catalog, branchless);
        expect(personResult.outcome() == ALL_CLEARED_CANDIDATE);
        expect(personResult.statuses().get("ACTIVE_BRANCHES") == NOT_APPLICABLE);
        expect(ArchiveDependencyAssessment.assess(person, request, catalog, complete).outcome() != ALL_CLEARED_CANDIDATE);
        var def = catalog.definitions().get("ACTIVE_CONTRACTS");
        for (var status : List.of(BLOCKED, UNKNOWN, UNAVAILABLE, NOT_APPLICABLE)) {
            var altered = new ArrayList<>(complete.stream().filter(e -> !e.dependencyCode().equals(def.code())).toList());
            altered.add(evidence(request, def, status));
            var result = ArchiveDependencyAssessment.assess(company, request, catalog, altered);
            expect(result.outcome() == ArchiveDependencyAssessment.Outcome.BLOCKED);
            expect(result.statuses().get(def.code()) == (status == NOT_APPLICABLE ? UNKNOWN : status));
        }
        var duplicate = new ArrayList<>(complete);
        duplicate.add(complete.getFirst());
        reject("DEPENDENCY_DUPLICATE_EVIDENCE", () -> ArchiveDependencyAssessment.assess(company, request, catalog, duplicate));
        var other = new ArchiveDependencyAssessment.Request(new TenantId("other"), id, "request-1");
        reject("ARCHIVE_REQUEST_SCOPE_MISMATCH", () -> ArchiveDependencyAssessment.assess(company, other, catalog, complete));
        reject("DEPENDENCY_REQUEST_MISMATCH", () -> ArchiveDependencyAssessment.assess(company, request, catalog, List.of(evidence(other, def, CLEARED))));
        var wrongRequest = new ArchiveDependencyAssessment.Request(tenant, id, "request-2");
        reject("DEPENDENCY_REQUEST_MISMATCH", () -> ArchiveDependencyAssessment.assess(company, request, catalog, List.of(evidence(wrongRequest, def, CLEARED))));
        var wrongOwner = new ArchiveDependencyAssessment.Evidence(request, def.code(), "Other", CLEARED, "evidence", Instant.EPOCH);
        reject("DEPENDENCY_OWNER_MISMATCH", () -> ArchiveDependencyAssessment.assess(company, request, catalog, List.of(wrongOwner)));
        var unknown = new ArchiveDependencyAssessment.Evidence(request, "UNKNOWN_CODE", "Other", CLEARED, "evidence", Instant.EPOCH);
        reject("DEPENDENCY_UNKNOWN_CODE", () -> ArchiveDependencyAssessment.assess(company, request, catalog, List.of(unknown)));
        var extra = new ArchiveDependencyCatalog.Definition("extra-id", "EXTRA", new LocalizedText(Map.of("en", "Extra")),
            new LocalizedText(Map.of()), "NewService", Set.of(ArchiveDependencyCatalog.CustomerKind.COMPANY), ArchiveDependencyCatalog.Status.ACTIVE, 10);
        var extended = catalog.addSystemCandidate(extra);
        expect(extended.definitions().size() == 11);
        expect(catalog.definitions().size() == 10);
        expect(ArchiveDependencyAssessment.assess(company, request, extended, complete).outcome() != ALL_CLEARED_CANDIDATE);
        var extendedEvidence = new ArrayList<>(complete);
        extendedEvidence.add(evidence(request, extra, CLEARED));
        expect(ArchiveDependencyAssessment.assess(company, request, extended, extendedEvidence).outcome() == ALL_CLEARED_CANDIDATE);
        reject("DEPENDENCY_ID_OR_CODE_DUPLICATE", () -> extended.addSystemCandidate(extra));
        var inactive = new ArchiveDependencyCatalog.Definition(extra.dependencyId(), extra.code(), extra.title(), extra.description(),
            extra.ownerService(), extra.applicableCustomerTypes(), ArchiveDependencyCatalog.Status.INACTIVE, 10);
        expect(ArchiveDependencyAssessment.assess(company, request, catalog.addSystemCandidate(inactive), extendedEvidence)
            .outcome() == ArchiveDependencyAssessment.Outcome.BLOCKED);
        expect(CustomerLifecycle.assess(company.status(), CustomerLifecycle.Transition.ARCHIVE, "reason")
            .outcome() == CustomerLifecycle.Outcome.BLOCKED);
        expect(company.status() == CustomerStatus.ACTIVE);
        System.out.println("PASS: " + checks + " isolated archive dependency assertions");
    }
    private static ArchiveDependencyAssessment.Evidence evidence(ArchiveDependencyAssessment.Request request,
            ArchiveDependencyCatalog.Definition definition, ArchiveDependencyAssessment.Status status) {
        return new ArchiveDependencyAssessment.Evidence(request, definition.code(), definition.ownerService(), status,
            "evidence-" + definition.code(), Instant.EPOCH);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Archive assertion " + (checks + 1));
        checks++;
    }
    private static void reject(String code, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { expect(code.equals(ex.getMessage())); return; }
        throw new AssertionError("Expected rejection " + code);
    }
}
