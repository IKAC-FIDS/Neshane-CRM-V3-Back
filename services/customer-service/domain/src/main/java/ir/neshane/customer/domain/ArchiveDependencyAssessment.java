package ir.neshane.customer.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure fail-closed synthetic assessment. No result authorizes an archive. */
public final class ArchiveDependencyAssessment {
    public enum Status { CLEARED, BLOCKED, UNKNOWN, UNAVAILABLE, NOT_APPLICABLE }
    public enum Outcome { ALL_CLEARED_CANDIDATE, BLOCKED }
    public record Request(TenantId tenantId, CustomerId customerId, String requestId) {
        public Request {
            Objects.requireNonNull(tenantId, "TENANT_REQUIRED");
            Objects.requireNonNull(customerId, "CUSTOMER_REQUIRED");
            IdentityIdentifier.required(requestId, "ARCHIVE_REQUEST_REQUIRED");
        }
    }
    public record Evidence(Request request, String dependencyCode, String ownerService,
            Status status, String evidenceId, Instant observedAt) {
        public Evidence {
            Objects.requireNonNull(request, "ARCHIVE_REQUEST_REQUIRED");
            IdentityIdentifier.required(dependencyCode, "DEPENDENCY_CODE_REQUIRED");
            IdentityIdentifier.required(ownerService, "DEPENDENCY_OWNER_REQUIRED");
            Objects.requireNonNull(status, "DEPENDENCY_STATUS_REQUIRED");
            IdentityIdentifier.required(evidenceId, "DEPENDENCY_EVIDENCE_REQUIRED");
            Objects.requireNonNull(observedAt, "DEPENDENCY_TIME_REQUIRED");
        }
    }
    public record Result(Request request, Outcome outcome, Map<String, Status> statuses) {
        public Result { statuses = Map.copyOf(statuses); }
    }
    private ArchiveDependencyAssessment() {}

    public static Result assess(Customer customer, Request request, ArchiveDependencyCatalog catalog,
            List<Evidence> evidence) {
        Objects.requireNonNull(customer, "CUSTOMER_REQUIRED");
        Objects.requireNonNull(request, "ARCHIVE_REQUEST_REQUIRED");
        if (!customer.id().equals(request.customerId()) || !customer.tenantId().equals(request.tenantId()))
            throw new IllegalArgumentException("ARCHIVE_REQUEST_SCOPE_MISMATCH");
        var kind = customer instanceof Company ? ArchiveDependencyCatalog.CustomerKind.COMPANY
            : ArchiveDependencyCatalog.CustomerKind.PERSON;
        Map<String, Evidence> supplied = new HashMap<>();
        for (Evidence item : List.copyOf(evidence)) {
            if (!request.equals(item.request())) throw new IllegalArgumentException("DEPENDENCY_REQUEST_MISMATCH");
            var definition = catalog.definitions().get(item.dependencyCode());
            if (definition == null) throw new IllegalArgumentException("DEPENDENCY_UNKNOWN_CODE");
            if (!definition.ownerService().equals(item.ownerService()))
                throw new IllegalArgumentException("DEPENDENCY_OWNER_MISMATCH");
            if (supplied.putIfAbsent(item.dependencyCode(), item) != null)
                throw new IllegalArgumentException("DEPENDENCY_DUPLICATE_EVIDENCE");
        }
        Map<String, Status> statuses = new HashMap<>();
        for (var definition : catalog.definitions().values()) {
            var item = supplied.get(definition.code());
            Status status;
            if (!definition.applicableCustomerTypes().contains(kind)) {
                // Contradictory reported dependency is not silently discarded.
                status = item == null || item.status() == Status.NOT_APPLICABLE
                    ? Status.NOT_APPLICABLE : Status.UNKNOWN;
            } else if (definition.status() != ArchiveDependencyCatalog.Status.ACTIVE || item == null) {
                status = Status.UNKNOWN;
            } else {
                status = item.status() == Status.NOT_APPLICABLE ? Status.UNKNOWN : item.status();
            }
            statuses.put(definition.code(), status);
        }
        boolean clear = statuses.values().stream().allMatch(s -> s == Status.CLEARED || s == Status.NOT_APPLICABLE);
        return new Result(request, clear ? Outcome.ALL_CLEARED_CANDIDATE : Outcome.BLOCKED, statuses);
    }
}
