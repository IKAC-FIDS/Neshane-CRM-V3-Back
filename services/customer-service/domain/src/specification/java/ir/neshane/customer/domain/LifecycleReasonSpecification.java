package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static ir.neshane.customer.domain.CustomerStatus.*;
import static ir.neshane.customer.domain.CustomerLifecycle.Transition.*;
import static ir.neshane.customer.domain.CustomerLifecycle.Outcome.*;

final class LifecycleReasonSpecification {
    private static final TenantId A = new TenantId("a");
    private static final TenantId B = new TenantId("b");
    private static int checks;

    static void run() {
        var catalog = LifecycleReasonCatalog.initial();
        expect(SystemLifecycleReasonVocabulary.values().length == 11);
        for (var reason : SystemLifecycleReasonVocabulary.values()) {
            expect(reason.definition().title().exact("fa").isPresent());
            expect(reason.definition().title().exact("en").isPresent());
        }
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, "CUSTOMER_REQUEST", null).transition().outcome() == ELIGIBLE_CANDIDATE);
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, null, null).transition().outcome() == REJECTED);
        expect(catalog.assess(A, INACTIVE, REACTIVATE, true, null, null).transition().outcome() == BLOCKED);
        expect(catalog.assess(A, ACTIVE, ARCHIVE, true, "REGISTRATION_ERROR", null).transition().outcome() == BLOCKED);
        expect(catalog.assess(A, ARCHIVED, RESTORE, true, "ARCHIVE_CORRECTION", null).transition().outcome() == BLOCKED);
        expect(catalog.assess(A, ARCHIVED, REACTIVATE, true, "OTHER", "comment").transition().outcome() == REJECTED);
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, "OTHER", " ").transition().code().equals("LIFECYCLE_REASON_COMMENT_REQUIRED"));
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, "OTHER", "synthetic").transition().outcome() == ELIGIBLE_CANDIDATE);
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, "REGISTRATION_ERROR", null).transition().outcome() == REJECTED);
        expect(catalog.assess(A, ACTIVE, DEACTIVATE, true, "UNKNOWN", null).transition().outcome() == REJECTED);
        var hidden = catalog.withHidden(A, true, "CUSTOMER_REQUEST", true);
        expect(hidden.assess(A, ACTIVE, DEACTIVATE, true, "CUSTOMER_REQUEST", null).transition().outcome() == REJECTED);
        expect(hidden.assess(B, ACTIVE, DEACTIVATE, true, "CUSTOMER_REQUEST", null).transition().outcome() == ELIGIBLE_CANDIDATE);
        expect(hidden.withHidden(A, true, "CUSTOMER_REQUEST", false).assess(A, ACTIVE, DEACTIVATE, true, "CUSTOMER_REQUEST", null).transition().outcome() == ELIGIBLE_CANDIDATE);
        var custom = def(A, "CUSTOM", "Old", LifecycleReasonDefinition.Status.ACTIVE);
        var added = catalog.addCandidate(A, custom);
        var selection = added.assess(A, ACTIVE, DEACTIVATE, false, "id", null);
        expect(selection.reason().orElseThrow().title().exact("en").orElseThrow().equals("Old"));
        expect(added.assess(B, ACTIVE, DEACTIVATE, false, "id", null).transition().outcome() == REJECTED);
        expect(added.assess(A, ACTIVE, DEACTIVATE, true, "id", null).transition().outcome() == REJECTED);
        var revised = added.reviseCandidate(A, def(A, "CUSTOM", "New", LifecycleReasonDefinition.Status.ACTIVE));
        expect(revised.assess(A, ACTIVE, DEACTIVATE, false, "id", null).reason().orElseThrow().title().exact("en").orElseThrow().equals("New"));
        expect(selection.reason().orElseThrow().title().exact("en").orElseThrow().equals("Old"));
        expect(revised.withHidden(A, false, "id", true).assess(A, ACTIVE, DEACTIVATE, false, "id", null).transition().outcome() == REJECTED);
        expect(revised.reviseCandidate(A, def(A, "CUSTOM", "New", LifecycleReasonDefinition.Status.INACTIVE)).assess(A, ACTIVE, DEACTIVATE, false, "id", null).transition().outcome() == REJECTED);
        reject(() -> added.addCandidate(A, custom));
        reject(() -> added.addCandidate(B, custom));
        reject(() -> catalog.addCandidate(A, def(A, "OTHER", "Reserved", LifecycleReasonDefinition.Status.ACTIVE)));
        reject(() -> added.reviseCandidate(A, def(A, "CHANGED", "New", LifecycleReasonDefinition.Status.ACTIVE)));
        reject(() -> catalog.reviseCandidate(A, SystemLifecycleReasonVocabulary.OTHER.definition()));
        expect(added.addCandidate(B, def(B, "CUSTOM", "Independent", LifecycleReasonDefinition.Status.ACTIVE))
            .assess(B, ACTIVE, DEACTIVATE, false, "id", null).transition().outcome() == ELIGIBLE_CANDIDATE);
        System.out.println("PASS: " + checks + " isolated lifecycle reason assertions");
    }

    private static LifecycleReasonDefinition def(TenantId tenant, String code, String title, LifecycleReasonDefinition.Status status) {
        return new LifecycleReasonDefinition("id", Optional.of(tenant), code, new LocalizedText(Map.of("en", title)),
            new LocalizedText(Map.of()), Set.of(DEACTIVATE), false, status, 0);
    }
    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Reason assertion " + (checks + 1));
        checks++;
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException e) { checks++; return; }
        throw new AssertionError("Expected rejection");
    }
}
