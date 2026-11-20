package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.contact.query.AssessContactCandidateQuery;
import ir.neshane.customer.application.contact.query.AssessContactCandidateResult;

/** Internal input port for isolated contact candidate assessment. */
public interface AssessContactCandidateUseCase {

    AssessContactCandidateResult execute(
            AssessContactCandidateQuery query);
}
