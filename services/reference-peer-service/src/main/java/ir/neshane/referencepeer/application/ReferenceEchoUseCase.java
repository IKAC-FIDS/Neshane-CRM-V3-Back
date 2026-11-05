package ir.neshane.referencepeer.application;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import ir.neshane.platform.observability.ObservabilityContext;
public final class ReferenceEchoUseCase {

    public EchoResult echo(String message) {
        return new EchoResult(
                "reference-peer-service",
                message,
                ObservabilityContext.correlationId(Context.current()),
                Span.current().getSpanContext().getTraceId());
    }

    public record EchoResult(String responder, String message, String correlationId, String traceId) {}
}
