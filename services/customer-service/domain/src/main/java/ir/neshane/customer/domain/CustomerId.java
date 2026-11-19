package ir.neshane.customer.domain;

/** Opaque, caller-supplied identity; no generation or topology policy. */
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CUSTOMER_ID_REQUIRED");
        }
    }
}
