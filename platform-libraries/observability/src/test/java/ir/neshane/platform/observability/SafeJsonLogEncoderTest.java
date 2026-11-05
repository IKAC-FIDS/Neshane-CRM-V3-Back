package ir.neshane.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SafeJsonLogEncoderTest {
    @Test
    void emitsStructuredAllowlistedFieldsAndDropsSensitiveMdc() throws Exception {
        LoggerContext loggerContext = new LoggerContext();
        SafeJsonLogEncoder encoder = new SafeJsonLogEncoder();
        encoder.setContext(loggerContext);
        encoder.setServiceName("reference-service");
        encoder.start();
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName("test.logger");
        event.setMessage("failed with Bearer this-is-a-sensitive-token");
        event.setThrowableProxy(new ThrowableProxy(new IllegalStateException("password=must-not-escape")));
        event.setMDCPropertyMap(Map.of(
                "trace_id", "0af7651916cd43dd8448eb211c80319c",
                "span_id", "b7ad6b7169203331",
                "correlation_id", "ec3f035a-17cc-4bb2-aee3-5e9b493c5140",
                "authorization", "Bearer must-not-escape",
                "customer.name", "must-not-escape"));

        String encoded = new String(encoder.encode(event), StandardCharsets.UTF_8);
        JsonNode json = new ObjectMapper().readTree(encoded);

        assertThat(json.get("service.name").asText()).isEqualTo("reference-service");
        assertThat(json.get("trace_id").asText()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(json.get("exception.type").asText()).isEqualTo(IllegalStateException.class.getName());
        assertThat(encoded).doesNotContain("this-is-a-sensitive-token", "must-not-escape", "customer.name");
    }
}
