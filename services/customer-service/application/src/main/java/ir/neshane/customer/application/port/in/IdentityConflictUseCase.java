package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.identity.query.IdentityConflictQuery;
import ir.neshane.customer.application.duplicate.identity.query.IdentityConflictResult;

public interface IdentityConflictUseCase {

    IdentityConflictResult execute(
            IdentityConflictQuery query);
}