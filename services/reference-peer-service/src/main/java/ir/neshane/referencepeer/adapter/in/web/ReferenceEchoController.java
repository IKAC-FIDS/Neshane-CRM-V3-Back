package ir.neshane.referencepeer.adapter.in.web;

import ir.neshane.referencepeer.application.ReferenceEchoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/reference-echoes", produces = MediaType.APPLICATION_JSON_VALUE)
public final class ReferenceEchoController {
    private final ReferenceEchoUseCase useCase;

    public ReferenceEchoController(ReferenceEchoUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ReferenceEchoResponse echo(@Valid @RequestBody ReferenceEchoRequest request) {
        var result = useCase.echo(request.message());
        return new ReferenceEchoResponse(
                result.responder(), result.message(), result.correlationId(), result.traceId());
    }

    public record ReferenceEchoRequest(@NotBlank @Size(max = 128) String message) {}

    public record ReferenceEchoResponse(
            String responder, String message, String correlationId, String traceId) {}
}
