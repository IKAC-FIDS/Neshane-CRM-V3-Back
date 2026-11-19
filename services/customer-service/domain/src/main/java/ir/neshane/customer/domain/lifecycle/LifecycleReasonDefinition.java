package ir.neshane.customer.domain.lifecycle;

import ir.neshane.customer.domain.customer.CustomerLifecycle;

import ir.neshane.customer.domain.shared.identifier.IdentifierText;

import ir.neshane.customer.domain.shared.localization.LocalizedText;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record LifecycleReasonDefinition(String reasonId, Optional<TenantId> tenantId,
        String code, LocalizedText title, LocalizedText description,
        Set<CustomerLifecycle.Transition> applicableTransitions,
        boolean requireComment, Status status, int displayOrder, Scope scope) {
    public enum Status { ACTIVE, INACTIVE }
    public enum Scope { CUSTOMER, BRANCH, BOTH }

    public LifecycleReasonDefinition(String reasonId, Optional<TenantId> tenantId, String code,
            LocalizedText title, LocalizedText description, Set<CustomerLifecycle.Transition> transitions,
            boolean requireComment, Status status, int displayOrder) {
        this(reasonId, tenantId, code, title, description, transitions, requireComment, status, displayOrder, Scope.CUSTOMER);
    }

    public LifecycleReasonDefinition {
        IdentifierText.requireNonBlank(reasonId, "LIFECYCLE_REASON_ID_REQUIRED");
        Objects.requireNonNull(tenantId, "LIFECYCLE_REASON_SCOPE_REQUIRED");
        IdentifierText.requireNonBlank(code, "LIFECYCLE_REASON_CODE_REQUIRED");
        code = code.strip().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_]{3,64}")) throw new IllegalArgumentException("LIFECYCLE_REASON_CODE_INVALID");
        Objects.requireNonNull(title, "LIFECYCLE_REASON_TITLE_REQUIRED");
        if (title.translations().isEmpty()) throw new IllegalArgumentException("LIFECYCLE_REASON_TITLE_REQUIRED");
        Objects.requireNonNull(description, "LIFECYCLE_REASON_DESCRIPTION_REQUIRED");
        applicableTransitions = Set.copyOf(applicableTransitions);
        if (applicableTransitions.isEmpty()) throw new IllegalArgumentException("LIFECYCLE_REASON_TRANSITIONS_REQUIRED");
        Objects.requireNonNull(status, "LIFECYCLE_REASON_STATUS_REQUIRED");
        Objects.requireNonNull(scope, "LIFECYCLE_REASON_APPLICABILITY_REQUIRED");
        if (scope == Scope.BRANCH && applicableTransitions.stream().anyMatch(t ->
                t == CustomerLifecycle.Transition.ARCHIVE || t == CustomerLifecycle.Transition.RESTORE))
            throw new IllegalArgumentException("LIFECYCLE_REASON_BRANCH_TRANSITION_INVALID");
        if (scope == Scope.BOTH && applicableTransitions.stream().noneMatch(t ->
                t == CustomerLifecycle.Transition.DEACTIVATE || t == CustomerLifecycle.Transition.REACTIVATE))
            throw new IllegalArgumentException("LIFECYCLE_REASON_BRANCH_TRANSITION_INVALID");
    }
}
