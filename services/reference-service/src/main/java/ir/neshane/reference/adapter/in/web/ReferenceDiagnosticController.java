package ir.neshane.reference.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/reference/diagnostics")
@Profile("!production")
@ConditionalOnProperty(
        prefix = "neshane.reference.diagnostics",
        name = "enabled",
        havingValue = "true")
public class ReferenceDiagnosticController {

    @GetMapping("/error")
    public void failForErrorContractVerification() {
        throw new ReferenceDiagnosticException();
    }
}
