package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateCommand;
import ir.neshane.customer.application.audit.metadata.command.AuditMetadataCandidateResult;

public interface AuditMetadataCandidateUseCase {

    AuditMetadataCandidateResult execute(
            AuditMetadataCandidateCommand command);
}
