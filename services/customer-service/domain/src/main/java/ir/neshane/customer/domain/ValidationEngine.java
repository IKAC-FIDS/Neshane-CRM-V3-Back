package ir.neshane.customer.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Evaluates only the supplied declarative rules. PASS is not identity verification. */
public final class ValidationEngine {
    public enum Status { PASS, FAIL, BLOCKED }
    public record Issue(String code, Optional<ValidationProfile.Field> field) {}
    public record Result(Status status, List<Issue> issues) {
        public Result { issues = List.copyOf(issues); }
    }

    private ValidationEngine() {}

    public static Result contact(ValidationProfile profile, ContactPoint point) {
        Objects.requireNonNull(profile, "VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(point, "CONTACT_POINT_REQUIRED");
        if (profile.tenant().isPresent() && !profile.tenant().orElseThrow().equals(point.owner().tenantId())) {
            return blocked("VALIDATION_PROFILE_TENANT_MISMATCH");
        }
        Map<ValidationProfile.Field, String> fields = new EnumMap<>(ValidationProfile.Field.class);
        switch (point.value()) {
            case ContactValue.Phone phone -> {
                fields.put(ValidationProfile.Field.VALUE, phone.number());
                phone.extension().ifPresent(value -> fields.put(ValidationProfile.Field.EXTENSION, value));
            }
            case ContactValue.Email email -> fields.put(ValidationProfile.Field.VALUE, email.address());
            case ContactValue.Website website -> fields.put(ValidationProfile.Field.VALUE, website.url());
            case ContactValue.Text text -> fields.put(ValidationProfile.Field.VALUE, text.value());
            case ContactValue.Address address -> {
                fields.put(ValidationProfile.Field.VALUE, address.addressLine());
                address.country().ifPresent(value -> fields.put(ValidationProfile.Field.COUNTRY, value));
                address.province().ifPresent(value -> fields.put(ValidationProfile.Field.PROVINCE, value));
                address.city().ifPresent(value -> fields.put(ValidationProfile.Field.CITY, value));
                address.postalCode().ifPresent(value -> fields.put(ValidationProfile.Field.POSTAL_CODE, value));
            }
        }
        return evaluate(profile, fields);
    }

    public static Result identity(ValidationProfile profile, IdentityIdentifier identifier) {
        Objects.requireNonNull(profile, "VALIDATION_PROFILE_REQUIRED");
        Objects.requireNonNull(identifier, "IDENTIFIER_REQUIRED");
        if (profile.tenant().isPresent()) {
            return blocked("IDENTIFIER_SYSTEM_PROFILE_REQUIRED");
        }
        Map<ValidationProfile.Field, String> fields = new EnumMap<>(ValidationProfile.Field.class);
        fields.put(ValidationProfile.Field.VALUE, identifier.value());
        identifier.issuingCountry().ifPresent(value -> fields.put(ValidationProfile.Field.COUNTRY, value));
        identifier.issuingAuthority().ifPresent(value -> fields.put(ValidationProfile.Field.ISSUER, value));
        identifier.issueDate().ifPresent(value -> fields.put(ValidationProfile.Field.ISSUE_DATE, value.toString()));
        identifier.expiryDate().ifPresent(value -> fields.put(ValidationProfile.Field.EXPIRY_DATE, value.toString()));
        return evaluate(profile, fields);
    }

    private static Result evaluate(ValidationProfile profile, Map<ValidationProfile.Field, String> fields) {
        List<Issue> issues = new ArrayList<>();
        boolean blocked = false;
        boolean failed = false;
        for (ValidationProfile.Rule rule : profile.rules()) {
            if (rule instanceof ValidationProfile.Unsupported) {
                blocked = true;
                issues.add(new Issue("VALIDATION_RULE_UNSUPPORTED", Optional.empty()));
            } else if (rule instanceof ValidationProfile.Required required) {
                String value = fields.get(required.field());
                if (value == null || value.codePoints().allMatch(cp ->
                        Character.isWhitespace(cp) || Character.isSpaceChar(cp))) {
                    failed = true;
                    issues.add(issue("VALIDATION_REQUIRED", required.field()));
                }
            } else if (rule instanceof ValidationProfile.Length length) {
                String value = fields.get(length.field());
                if (value != null) {
                    boolean units = within(value.length(), length);
                    boolean points = within(value.codePointCount(0, value.length()), length);
                    if (units != points) {
                        blocked = true;
                        issues.add(issue("VALIDATION_LENGTH_POLICY_UNRESOLVED", length.field()));
                    } else if (!units) {
                        failed = true;
                        issues.add(issue("VALIDATION_LENGTH", length.field()));
                    }
                }
            } else if (rule instanceof ValidationProfile.AllowedValues allowed) {
                String value = fields.get(allowed.field());
                if (value != null && !allowed.values().contains(value)) {
                    failed = true;
                    issues.add(issue("VALIDATION_VALUE_NOT_ALLOWED", allowed.field()));
                }
            }
        }
        return new Result(failed ? Status.FAIL : blocked ? Status.BLOCKED : Status.PASS, issues);
    }

    private static boolean within(int size, ValidationProfile.Length length) {
        return size >= length.minimum() && size <= length.maximum();
    }

    private static Issue issue(String code, ValidationProfile.Field field) {
        return new Issue(code, Optional.of(field));
    }

    static Result blocked(String code) {
        return new Result(Status.BLOCKED, List.of(new Issue(code, Optional.empty())));
    }
}
