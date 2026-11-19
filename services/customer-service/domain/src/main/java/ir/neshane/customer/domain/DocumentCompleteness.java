package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.Customer;

import ir.neshane.customer.domain.shared.identifier.CustomerDocumentId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Synthetic requirement coverage, never identity proof or permission to register/verify. */
public final class DocumentCompleteness {
    public enum CountingConvention { UNRESOLVED, ANY_CURRENT_PROVIDED }
    public enum Outcome { ASSESSED, UNRESOLVED, BLOCKED }
    public enum Coverage { COMPLETE, INCOMPLETE, UNRESOLVED }
    public record Item(String typeId,Set<CustomerDocument.State> observedStates,Coverage coverage,boolean unresolvedValidity) {
        public Item { observedStates = Set.copyOf(observedStates); }
        @Override public String toString() { return "DocumentCompleteness.Item[protected, redacted]"; }
    }
    public record Summary(int totalRequired,OptionalInt completedRequired,List<Item> items) {
        public Summary { items = List.copyOf(items); }
        @Override public String toString() { return "DocumentCompleteness.Summary[protected, redacted]"; }
    }
    public record Assessment(Outcome outcome,String code,Optional<Summary> summary) {}
    private DocumentCompleteness() {}

    public static Assessment assess(Customer customer,DocumentRequirementPolicy policy,
            DocumentRequirementPolicy.Operation operation,String requestId,DocumentRequirementPolicy.Snapshot snapshot,
            List<DocumentVersionHistory> histories,LocalDate asOf,DocumentValidity.ExpiryBoundary boundary,
            CountingConvention convention) {
        Objects.requireNonNull(policy);
        var scope = policy.assessSnapshot(customer,operation,requestId,snapshot);
        if (scope.outcome() != DocumentRequirementPolicy.Outcome.ELIGIBLE_CANDIDATE)
            return blocked(scope.code());
        Objects.requireNonNull(asOf);
        Objects.requireNonNull(boundary);
        Objects.requireNonNull(convention);
        var supplied = List.copyOf(histories);
        var ids = new HashSet<CustomerDocumentId>();
        for (var history : supplied) {
            var doc = history.current();
            if (!snapshot.owner().equals(doc.owner())) return blocked("DOCUMENT_COMPLETENESS_SCOPE_MISMATCH");
            if (!ids.add(doc.id())) return blocked("DOCUMENT_COMPLETENESS_DUPLICATE_HISTORY");
        }
        var items = new ArrayList<Item>();
        int completed = 0;
        boolean unresolved = false;
        for (var required : snapshot.requiredTypes()) {
            var observed = EnumSet.noneOf(CustomerDocument.State.class);
            boolean uncertain = false;
            boolean found = false;
            for (var history : supplied) {
                var doc = history.current();
                if (!required.id().equals(doc.typeId())) continue;
                found = true;
                var validity = DocumentValidity.assess(doc,asOf,boundary);
                if (validity.state().isPresent()) observed.add(validity.state().orElseThrow());
                else uncertain = true;
            }
            if (!found) observed.add(CustomerDocument.State.NOT_PROVIDED);
            Coverage coverage;
            if (!found) coverage = Coverage.INCOMPLETE;
            else if (convention == CountingConvention.UNRESOLVED) coverage = Coverage.UNRESOLVED;
            else if (observed.contains(CustomerDocument.State.PROVIDED)) coverage = Coverage.COMPLETE;
            else coverage = uncertain ? Coverage.UNRESOLVED : Coverage.INCOMPLETE;
            if (coverage == Coverage.COMPLETE) completed++;
            if (coverage == Coverage.UNRESOLVED) unresolved = true;
            items.add(new Item(required.id(),observed,coverage,uncertain));
        }
        var summary = new Summary(items.size(),unresolved ? OptionalInt.empty() : OptionalInt.of(completed),items);
        return new Assessment(unresolved ? Outcome.UNRESOLVED : Outcome.ASSESSED,
            unresolved ? "DOCUMENT_COMPLETENESS_UNRESOLVED" : "DOCUMENT_COMPLETENESS_CANDIDATE",Optional.of(summary));
    }
    private static Assessment blocked(String code) {
        return new Assessment(Outcome.BLOCKED,code,Optional.empty());
    }
}
