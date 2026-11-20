package ir.neshane.customer.domain.shared.identifier;

/** Shared identifier text guard; preserves whitespace and error semantics. */
public final class IdentifierText {
    private IdentifierText() {}

    public static void requireNonBlank(String value, String code) {
        if (value == null || value.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp))) {
            throw new IllegalArgumentException(code);
        }
    }
}
