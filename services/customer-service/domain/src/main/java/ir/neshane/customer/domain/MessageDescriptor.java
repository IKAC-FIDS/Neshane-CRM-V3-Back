package ir.neshane.customer.domain;

/** Stable internal message metadata, not an approved external error contract. */
public record MessageDescriptor(String code, String messageKey) {
    public MessageDescriptor {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("MESSAGE_CODE_REQUIRED");
        }
        if (messageKey == null || messageKey.isBlank()) {
            throw new IllegalArgumentException("MESSAGE_KEY_REQUIRED");
        }
    }
}
