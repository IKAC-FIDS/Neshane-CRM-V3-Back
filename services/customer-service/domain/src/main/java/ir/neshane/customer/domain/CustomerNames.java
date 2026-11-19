package ir.neshane.customer.domain;

import java.util.Optional;

final class CustomerNames {
    private CustomerNames() {}

    static String required(String value, String field, int maximum) {
        if (value == null) {
            throw new IllegalArgumentException(field + "_REQUIRED");
        }
        String trimmed = trim(value);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + "_REQUIRED");
        }
        int codePoints = trimmed.codePointCount(0, trimmed.length());
        if (codePoints > maximum) {
            throw new IllegalArgumentException(field + "_TOO_LONG");
        }
        if (trimmed.length() > maximum) {
            // Exact character-count semantics remain OPEN; do not guess.
            throw new IllegalArgumentException(field + "_LENGTH_POLICY_UNRESOLVED");
        }
        return trimmed;
    }

    static Optional<String> optionalEnglishName(String value) {
        return value == null ? Optional.empty()
            : Optional.of(required(value, "ENGLISH_NAME", 255));
    }

    private static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int cp = value.codePointAt(start);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) {
                break;
            }
            start += Character.charCount(cp);
        }
        while (end > start) {
            int cp = value.codePointBefore(end);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return value.substring(start, end);
    }
}
