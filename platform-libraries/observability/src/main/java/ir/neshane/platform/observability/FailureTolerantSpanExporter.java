package ir.neshane.platform.observability;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;

/** Prevents exporter implementation failures from escaping into request processing paths. */
final class FailureTolerantSpanExporter implements SpanExporter {
    private final SpanExporter delegate;

    FailureTolerantSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        try {
            return delegate.export(spans);
        } catch (RuntimeException ignored) {
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode flush() {
        try {
            return delegate.flush();
        } catch (RuntimeException ignored) {
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        try {
            return delegate.shutdown();
        } catch (RuntimeException ignored) {
            return CompletableResultCode.ofFailure();
        }
    }
}
