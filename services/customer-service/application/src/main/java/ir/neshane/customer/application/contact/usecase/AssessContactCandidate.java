package ir.neshane.customer.application.contact.usecase;

import ir.neshane.customer.domain.contact.ContactPoint;
import ir.neshane.customer.domain.contact.ContactValidation;
import ir.neshane.customer.domain.shared.validation.ValidationEngine;
import ir.neshane.customer.domain.shared.validation.ValidationProfile;

/**
 * Isolated assessment of supplied synthetic contact facts.
 * Not registration, verification or runtime authorization.
 */
public final class AssessContactCandidate {

    public ValidationEngine.Result execute(
            ValidationProfile profile,
            ContactPoint point) {

        return ContactValidation.evaluate(profile, point);
    }
}
