package ir.neshane.reference.adapter.in.web.error;

import ir.neshane.reference.adapter.in.web.ReferenceDiagnosticException;
import ir.neshane.reference.adapter.outbound.http.ReferencePeerUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ReferenceErrorHandler extends ResponseEntityExceptionHandler {

    private static final String DIAGNOSTIC_ERROR_CODE = "REFERENCE_DIAGNOSTIC_FAILURE";

    private final CorrelationIdResolver correlationIdResolver;

    public ReferenceErrorHandler(CorrelationIdResolver correlationIdResolver) {
        this.correlationIdResolver = correlationIdResolver;
    }

    @ExceptionHandler(ReferenceDiagnosticException.class)
    ProblemDetail handleReferenceDiagnosticFailure(
            ReferenceDiagnosticException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Reference diagnostic failure");
        problem.setDetail("The reference diagnostic request failed safely.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", DIAGNOSTIC_ERROR_CODE);
        problem.setProperty("correlationId", correlationIdResolver.resolve(request));
        return problem;
    }

    @ExceptionHandler(ReferencePeerUnavailableException.class)
    ProblemDetail handlePeerUnavailable(ReferencePeerUnavailableException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Reference peer unavailable");
        problem.setDetail("The reference interaction could not be completed.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "REFERENCE_PEER_UNAVAILABLE");
        problem.setProperty("correlationId", correlationIdResolver.resolve(request));
        return problem;
    }

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
        problem.setProperty("correlationId", ir.neshane.platform.observability.ObservabilityContext.correlationId(
                io.opentelemetry.context.Context.current()));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }
}
