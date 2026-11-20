package ir.neshane.customer.application.access.disclosure.audit.usecase;

import ir.neshane.customer.application.port.in.AuditReadAccessUseCase;
import ir.neshane.customer.application.access.disclosure.audit.query.AuditReadAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.ReadDisclosurePolicy;

import java.util.Objects;

/**
 * Evaluates audit read access independently of business access.
 * Does not retrieve or disclose audit records.
 */
public final class AssessAuditReadAccess
        implements AuditReadAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            AuditReadAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                ReadDisclosurePolicy.audit(
                        query.actor(),
                        query.targetTenant()));
    }
}
