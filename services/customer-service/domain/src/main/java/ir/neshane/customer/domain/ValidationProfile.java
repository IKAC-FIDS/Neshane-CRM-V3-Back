package ir.neshane.customer.domain;

import ir.neshane.customer.domain.shared.identifier.TenantId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Declarative bounded rule descriptors; no arbitrary scripts or regex execution. */
public record ValidationProfile(String id, Optional<TenantId> tenant, List<Rule> rules) {
    public enum Field {
        VALUE, EXTENSION, COUNTRY, PROVINCE, CITY, POSTAL_CODE, ISSUER, ISSUE_DATE, EXPIRY_DATE
    }

    public sealed interface Rule permits Required, Length, AllowedValues, Unsupported {}

    public record Required(Field field) implements Rule {
        public Required { Objects.requireNonNull(field, "VALIDATION_FIELD_REQUIRED"); }
    }

    public record Length(Field field, int minimum, int maximum) implements Rule {
        public Length {
            Objects.requireNonNull(field, "VALIDATION_FIELD_REQUIRED");
            if (minimum < 0 || maximum < minimum) {
                throw new IllegalArgumentException("VALIDATION_LENGTH_RANGE_INVALID");
            }
        }
    }

    public record AllowedValues(Field field, Set<String> values) implements Rule {
        public AllowedValues {
            Objects.requireNonNull(field, "VALIDATION_FIELD_REQUIRED");
            values = Set.copyOf(Objects.requireNonNull(values, "VALIDATION_VALUES_REQUIRED"));
            if (values.isEmpty()) {
                throw new IllegalArgumentException("VALIDATION_VALUES_REQUIRED");
            }
        }
    }

    public enum UnsupportedKind { FORMAT, PATTERN, RANGE, ALGORITHM, COUNTRY_SPECIFIC, CROSS_FIELD }

    public record Unsupported(UnsupportedKind kind) implements Rule {
        public Unsupported { Objects.requireNonNull(kind, "VALIDATION_RULE_KIND_REQUIRED"); }
    }

    public ValidationProfile {
        IdentityIdentifier.required(id, "VALIDATION_PROFILE_ID_REQUIRED");
        Objects.requireNonNull(tenant, "VALIDATION_PROFILE_SCOPE_REQUIRED");
        rules = List.copyOf(Objects.requireNonNull(rules, "VALIDATION_RULES_REQUIRED"));
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("VALIDATION_RULES_REQUIRED");
        }
    }
}
