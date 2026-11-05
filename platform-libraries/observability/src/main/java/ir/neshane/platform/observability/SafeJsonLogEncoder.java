package ir.neshane.platform.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** JSON encoder that deliberately ignores arbitrary MDC entries. */
public final class SafeJsonLogEncoder extends EncoderBase<ILoggingEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String serviceName;
    private int maximumMessageLength = 4096;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setMaximumMessageLength(int maximumMessageLength) {
        this.maximumMessageLength = maximumMessageLength;
    }

    @Override
    public void start() {
        if (serviceName == null || serviceName.isBlank()) {
            addError("serviceName is required");
            return;
        }
        if (maximumMessageLength < 1) {
            addError("maximumMessageLength must be positive");
            return;
        }
        super.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        json.put("level", event.getLevel().toString());
        json.put("service.name", serviceName);
        json.put("logger", event.getLoggerName());
        json.put("thread", event.getThreadName());
        json.put("message", TelemetrySanitizer.sanitizeMessage(event.getFormattedMessage(), maximumMessageLength));
        if (event.getThrowableProxy() != null) {
            json.put("exception.type", event.getThrowableProxy().getClassName());
        }
        copyTraceField(event, json, "trace_id");
        copyTraceField(event, json, "span_id");
        CorrelationMetadata.validate(event.getMDCPropertyMap().get(CorrelationMetadata.MDC_NAME))
                .ifPresent(value -> json.put(CorrelationMetadata.MDC_NAME, value));
        try {
            return (objectMapper.writeValueAsString(json) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            addError("Unable to encode safe JSON log", exception);
            return ("{\"level\":\"ERROR\",\"message\":\"log-encoding-failed\"}" + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private static void copyTraceField(ILoggingEvent event, Map<String, Object> target, String field) {
        String value = event.getMDCPropertyMap().get(field);
        int expectedLength = "trace_id".equals(field) ? 32 : 16;
        if (value != null && value.matches("[0-9a-f]{" + expectedLength + "}")) {
            target.put(field, value);
        }
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}
