package ir.neshane.customer.application.access.disclosure.identity.usecase;

import ir.neshane.customer.application.port.in.IdentityDisclosureUseCase;
import ir.neshane.customer.application.access.disclosure.identity.query.IdentityDisclosureQuery;
import ir.neshane.customer.application.access.disclosure.identity.query.IdentityDisclosureResult;
import ir.neshane.customer.domain.access.ReadDisclosurePolicy;

import java.util.Objects;

/**
 * Evaluates identity disclosure disposition.
 * No actual identity disclosure, projection or masking.
 */
public final class AssessIdentityDisclosure
        implements IdentityDisclosureUseCase {

    @Override
    public IdentityDisclosureResult execute(
            IdentityDisclosureQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        var assessment = ReadDisclosurePolicy.identityFields(
                query.actor(),
                query.customer());

        return switch (assessment) {

            case DENIED ->
                new IdentityDisclosureResult(
                        IdentityDisclosureResult.Status.DENIED,
                        "IDENTITY_DISCLOSURE_DENIED");

            case MASK_REQUIRED ->
                new IdentityDisclosureResult(
                        IdentityDisclosureResult.Status.MASK_REQUIRED,
                        "IDENTITY_DISCLOSURE_MASK_REQUIRED");

            case ELIGIBLE_CANDIDATE ->
                new IdentityDisclosureResult(
                        IdentityDisclosureResult.Status.ELIGIBLE_CANDIDATE,
                        "IDENTITY_DISCLOSURE_ELIGIBLE_CANDIDATE");
        };
    }
}
