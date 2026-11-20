package ir.neshane.customer.application.access.sensitive.document.usecase;

import ir.neshane.customer.application.port.in.DocumentAccessUseCase;
import ir.neshane.customer.application.access.sensitive.document.query.DocumentAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.SensitiveAccessPolicy;

import java.util.Objects;

/**
 * Evaluates owner-bound document access.
 * No file access, document disclosure or execution.
 */
public final class AssessDocumentAccess
        implements DocumentAccessUseCase {

    @Override
    public AccessAssessmentResult execute(DocumentAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                SensitiveAccessPolicy.assessDocument(
                        query.actor(),
                        query.owner(),
                        query.document(),
                        query.operation()));
    }
}
