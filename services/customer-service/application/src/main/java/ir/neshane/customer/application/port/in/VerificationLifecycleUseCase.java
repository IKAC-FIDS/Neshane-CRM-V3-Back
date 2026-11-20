package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.verification.lifecycle.query.VerificationLifecycleQuery;
import ir.neshane.customer.application.verification.lifecycle.query.VerificationLifecycleResult;

public interface VerificationLifecycleUseCase {

    VerificationLifecycleResult execute(
            VerificationLifecycleQuery query);
}