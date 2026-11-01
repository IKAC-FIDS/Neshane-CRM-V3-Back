package ir.neshane.platform.observability;

import java.util.Set;

/** Low-cardinality telemetry dimensions approved by this library's data-minimization boundary. */
public final class ApprovedTelemetryAttributes {
    public static final String HTTP_METHOD = "http.request.method";
    public static final String HTTP_ROUTE = "http.route";
    public static final String HTTP_STATUS_CODE = "http.response.status_code";
    public static final String ERROR_TYPE = "error.type";
    public static final String MESSAGING_SYSTEM = "messaging.system";
    public static final String MESSAGING_OPERATION = "messaging.operation.name";
    public static final String MESSAGING_DESTINATION = "messaging.destination.name";

    public static final Set<String> ALL = Set.of(
            HTTP_METHOD,
            HTTP_ROUTE,
            HTTP_STATUS_CODE,
            ERROR_TYPE,
            MESSAGING_SYSTEM,
            MESSAGING_OPERATION,
            MESSAGING_DESTINATION);

    private ApprovedTelemetryAttributes() {
    }
}
