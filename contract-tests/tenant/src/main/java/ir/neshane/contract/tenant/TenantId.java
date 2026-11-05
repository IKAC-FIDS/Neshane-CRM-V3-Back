package ir.neshane.contract.tenant;

import java.util.Objects;
import java.util.regex.Pattern;

record TenantId(String value) {

    private static final Pattern CANONICAL_UUID_V4 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    TenantId {
        Objects.requireNonNull(value, "value");
        if (!CANONICAL_UUID_V4.matcher(value).matches()) {
            throw new TenantContractException("Tenant ID must be a canonical lowercase UUIDv4");
        }
    }
}
