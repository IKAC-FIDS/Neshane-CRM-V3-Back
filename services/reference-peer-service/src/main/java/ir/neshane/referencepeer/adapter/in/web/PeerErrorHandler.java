package ir.neshane.referencepeer.adapter.in.web;

import io.opentelemetry.context.Context;
import ir.neshane.platform.observability.ObservabilityContext;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public final class PeerErrorHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid reference request");
        problem.setDetail("The request did not satisfy the public contract.");
        problem.setInstance(URI.create(request.getDescription(false).replaceFirst("^uri=", "")));
        problem.setProperty("code", "REFERENCE_REQUEST_INVALID");
        problem.setProperty("correlationId", ObservabilityContext.correlationId(Context.current()));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }
}
