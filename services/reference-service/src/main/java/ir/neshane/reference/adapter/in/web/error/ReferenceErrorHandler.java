package ir.neshane.reference.adapter.in.web.error;

import ir.neshane.reference.adapter.in.web.ReferenceDiagnosticException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReferenceErrorHandler {

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
}
