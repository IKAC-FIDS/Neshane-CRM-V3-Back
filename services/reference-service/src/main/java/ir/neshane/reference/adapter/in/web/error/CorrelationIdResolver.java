package ir.neshane.reference.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class CorrelationIdResolver {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    String resolve(HttpServletRequest request) {
        String candidate = request.getHeader(CORRELATION_HEADER);
        if (candidate != null) {
            try {
                return UUID.fromString(candidate).toString();
            } catch (IllegalArgumentException ignored) {
                // An untrusted or malformed selector is replaced, never propagated as validated context.
            }
        }
        return UUID.randomUUID().toString();
    }
}
