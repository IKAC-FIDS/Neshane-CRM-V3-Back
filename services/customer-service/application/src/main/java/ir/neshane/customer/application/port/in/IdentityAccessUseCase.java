package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.access.sensitive.identity.query.IdentityAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;

public interface IdentityAccessUseCase {

    AccessAssessmentResult execute(IdentityAccessQuery query);
}
