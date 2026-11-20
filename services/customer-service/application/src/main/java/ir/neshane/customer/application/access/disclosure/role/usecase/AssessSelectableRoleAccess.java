package ir.neshane.customer.application.access.disclosure.role.usecase;

import ir.neshane.customer.application.port.in.SelectableRoleAccessUseCase;
import ir.neshane.customer.application.access.disclosure.role.query.SelectableRoleAccessQuery;
import ir.neshane.customer.application.access.shared.AccessAssessmentResult;
import ir.neshane.customer.application.access.shared.AccessAssessmentMapper;
import ir.neshane.customer.domain.access.ReadDisclosurePolicy;

import java.util.Objects;

/**
 * Evaluates tenant-visible, selectable role access.
 * Does not disclose role definitions or private catalog details.
 */
public final class AssessSelectableRoleAccess
        implements SelectableRoleAccessUseCase {

    @Override
    public AccessAssessmentResult execute(
            SelectableRoleAccessQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        return AccessAssessmentMapper.map(
                ReadDisclosurePolicy.selectableRole(
                        query.actor(),
                        query.targetTenant(),
                        query.catalog(),
                        query.reference(),
                        query.targetKind()));
    }
}
