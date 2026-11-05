package ir.neshane.reference.adapter.in.web;

import ir.neshane.reference.application.RelayReferenceUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/reference-relays", produces = MediaType.APPLICATION_JSON_VALUE)
public final class ReferenceRelayController {
    private final RelayReferenceUseCase useCase;

    public ReferenceRelayController(RelayReferenceUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ReferenceRelayResponse relay(@Valid @RequestBody ReferenceRelayRequest request) {
        var result = useCase.relay(request.message());
        return new ReferenceRelayResponse(
                result.responder(), result.message(), result.correlationId(), result.traceId());
    }

    public record ReferenceRelayRequest(@NotBlank @Size(max = 128) String message) {}

    public record ReferenceRelayResponse(
            String responder, String message, String correlationId, String traceId) {}
}
