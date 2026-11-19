package ir.neshane.customer.domain.customer;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure state transition assessment. ELIGIBLE_CANDIDATE is never authorization
 * or an applied change. No caller-supplied clearance flag can unlock a blocked path.
 */
public final class CustomerLifecycle {
    public enum Transition { DEACTIVATE, REACTIVATE, ARCHIVE, RESTORE }
    public enum Outcome { ELIGIBLE_CANDIDATE, REJECTED, BLOCKED }

    public record Assessment(CustomerStatus current, Transition transition,
                             Optional<CustomerStatus> proposed, Outcome outcome, String code) {}

    private CustomerLifecycle() {}

    public static Assessment assess(CustomerStatus current, Transition transition, String reason) {
        Objects.requireNonNull(current, "CUSTOMER_STATUS_REQUIRED");
        Objects.requireNonNull(transition, "CUSTOMER_TRANSITION_REQUIRED");
        CustomerStatus target = switch (transition) {
            case DEACTIVATE -> current == CustomerStatus.ACTIVE ? CustomerStatus.INACTIVE : null;
            case REACTIVATE -> current == CustomerStatus.INACTIVE ? CustomerStatus.ACTIVE : null;
            case ARCHIVE -> current == CustomerStatus.ARCHIVED ? null : CustomerStatus.ARCHIVED;
            case RESTORE -> current == CustomerStatus.ARCHIVED ? CustomerStatus.INACTIVE : null;
        };
        if (target == null) {
            return new Assessment(current, transition, Optional.empty(), Outcome.REJECTED,
                "CUSTOMER_TRANSITION_NOT_ALLOWED");
        }
        if (transition != Transition.REACTIVATE && (reason == null || reason.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp)))) {
            return new Assessment(current, transition, Optional.of(target), Outcome.REJECTED,
                "CUSTOMER_TRANSITION_REASON_REQUIRED");
        }
        return switch (transition) {
            case DEACTIVATE -> new Assessment(current, transition, Optional.of(target),
                Outcome.ELIGIBLE_CANDIDATE, "CUSTOMER_DEACTIVATION_CANDIDATE");
            case REACTIVATE -> blocked(current, transition, target, "CUSTOMER_REVALIDATION_UNRESOLVED");
            case ARCHIVE -> blocked(current, transition, target, "CUSTOMER_ARCHIVE_CLEARANCE_UNRESOLVED");
            case RESTORE -> blocked(current, transition, target, "CUSTOMER_RESTORATION_CONTRACT_UNRESOLVED");
        };
    }

    private static Assessment blocked(CustomerStatus current, Transition transition,
                                       CustomerStatus target, String code) {
        return new Assessment(current, transition, Optional.of(target), Outcome.BLOCKED, code);
    }
}
