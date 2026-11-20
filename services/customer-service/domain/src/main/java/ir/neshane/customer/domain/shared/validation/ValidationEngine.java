package ir.neshane.customer.domain.shared.validation;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Evaluates only the supplied declarative rules. PASS is not identity verification. */
public final class ValidationEngine {
    public enum Status { PASS, FAIL, BLOCKED }
    public record Issue(String code, Optional<ValidationProfile.Field> field) {}
    public record Result(Status status, List<Issue> issues) {
        public Result { issues = List.copyOf(issues); }
    }

    private ValidationEngine() {}

    public static Result evaluate(ValidationProfile profile, Map<ValidationProfile.Field, String> fields) {
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

    public static Result blocked(String code) {
        return new Result(Status.BLOCKED, List.of(new Issue(code, Optional.empty())));
    }
}
