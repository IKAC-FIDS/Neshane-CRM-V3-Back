package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.disclosure.identity.query.IdentityDisclosureQuery;
import ir.neshane.customer.application.access.disclosure.identity.query.IdentityDisclosureResult;

public interface IdentityDisclosureUseCase {

    IdentityDisclosureResult execute(
            IdentityDisclosureQuery query);
}
