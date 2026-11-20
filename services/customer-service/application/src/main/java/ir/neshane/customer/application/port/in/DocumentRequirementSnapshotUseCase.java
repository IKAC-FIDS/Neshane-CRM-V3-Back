package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.document.requirement.query.DocumentRequirementSnapshotQuery;
import ir.neshane.customer.application.document.requirement.query.DocumentRequirementSnapshotResult;

public interface DocumentRequirementSnapshotUseCase {

    DocumentRequirementSnapshotResult execute(
            DocumentRequirementSnapshotQuery query);
}