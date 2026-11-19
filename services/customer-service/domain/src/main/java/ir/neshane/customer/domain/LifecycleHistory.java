package ir.neshane.customer.domain;

import ir.neshane.customer.domain.customer.CustomerLifecycle;
import ir.neshane.customer.domain.customer.CustomerStatus;
import ir.neshane.customer.domain.customer.Customer;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.identifier.TenantId;
import ir.neshane.customer.domain.shared.identifier.CustomerId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable synthetic history draft. Not a durable history or proof of execution. */
public final class LifecycleHistory {
    public record Context(String historyId, String actorId, Instant occurredAt, String correlationId) {
        public Context {
            IdentifierText.requireNonBlank(historyId, "HISTORY_ID_REQUIRED");
            IdentifierText.requireNonBlank(actorId, "ACTOR_ID_REQUIRED");
            Objects.requireNonNull(occurredAt, "OCCURRED_AT_REQUIRED");
            IdentifierText.requireNonBlank(correlationId, "CORRELATION_ID_REQUIRED");
        }
        @Override public String toString() { return "LifecycleHistory.Context[redacted]"; }
    }

    public static final class Entry {
        private final CustomerId customerId;
        private final TenantId tenantId;
        private final CustomerStatus previousStatus;
        private final CustomerStatus newStatus;
        private final LifecycleReasonCatalog.Selection reason;
        private final Optional<String> comment;
        private final Context context;

        private Entry(Customer customer, CustomerStatus target, LifecycleReasonCatalog.Selection reason,
                      String comment, Context context) {
            this.customerId = customer.id();
            this.tenantId = customer.tenantId();
            this.previousStatus = customer.status();
            this.newStatus = target;
            this.reason = reason;
            this.comment = Optional.ofNullable(comment);
            this.context = context;
        }
        public CustomerId customerId() { return customerId; }
        public TenantId tenantId() { return tenantId; }
        public CustomerStatus previousStatus() { return previousStatus; }
        public CustomerStatus newStatus() { return newStatus; }
        public LifecycleReasonCatalog.Selection reason() { return reason; }
        public Optional<String> comment() { return comment; }
        public Context context() { return context; }
        @Override public String toString() { return "LifecycleHistory.Entry[draft, redacted]"; }
    }

    private final CustomerId customerId;
    private final TenantId tenantId;
    private final List<Entry> entries;

    private LifecycleHistory(CustomerId customerId, TenantId tenantId, List<Entry> entries) {
        this.customerId = customerId;
        this.tenantId = tenantId;
        this.entries = List.copyOf(entries);
    }

    public static LifecycleHistory empty(Customer customer) {
        Objects.requireNonNull(customer, "CUSTOMER_REQUIRED");
        return new LifecycleHistory(customer.id(), customer.tenantId(), List.of());
    }

    public List<Entry> entries() { return entries; }

    /** Re-evaluates the catalog instead of accepting a caller-constructed assessment. */
    public LifecycleHistory appendCandidate(Customer customer, LifecycleReasonCatalog catalog,
            CustomerLifecycle.Transition transition, boolean system, String reasonId, String comment, Context context) {
        Objects.requireNonNull(context, "HISTORY_CONTEXT_REQUIRED");
        if (!customerId.equals(customer.id()) || !tenantId.equals(customer.tenantId()))
            throw new IllegalArgumentException("HISTORY_CUSTOMER_SCOPE_MISMATCH");
        var result = catalog.assess(tenantId, customer.status(), transition, system, reasonId, comment);
        if (result.transition().outcome() != CustomerLifecycle.Outcome.ELIGIBLE_CANDIDATE)
            throw new IllegalArgumentException(result.transition().code());
        if (entries.stream().anyMatch(e -> e.context().historyId().equals(context.historyId())))
            throw new IllegalArgumentException("HISTORY_ID_DUPLICATE");
        if (!entries.isEmpty()) {
            Entry last = entries.getLast();
            if (last.newStatus() != customer.status())
                throw new IllegalArgumentException("HISTORY_STATUS_DISCONTINUITY");
            if (context.occurredAt().isBefore(last.context().occurredAt()))
                throw new IllegalArgumentException("HISTORY_TIME_DISCONTINUITY");
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(customer, result.transition().proposed().orElseThrow(),
            result.reason().orElseThrow(), comment, context));
        return new LifecycleHistory(customerId, tenantId, next);
    }
}
