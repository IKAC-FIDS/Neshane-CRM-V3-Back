package ir.neshane.customer.application.duplicate.resolution.usecase;

import ir.neshane.customer.application.duplicate.resolution.command.DuplicateRegistrationCommand;
import ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult;
import ir.neshane.customer.application.port.in.DuplicateRegistrationUseCase;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;
import ir.neshane.customer.domain.duplicate.DuplicateEventDraft;
import ir.neshane.customer.domain.duplicate.DuplicateExceptionPolicy;

import java.util.Objects;
import java.util.Optional;

public final class AssessDuplicateRegistration
        implements DuplicateRegistrationUseCase {

    @Override
    public DuplicateResolutionResult execute(
            DuplicateRegistrationCommand command) {

        Objects.requireNonNull(command, "COMMAND_REQUIRED");

        try {
            if (!DuplicateDetection.Request.of(
                    command.candidate(),
                    command.request().operation(),
                    command.request().requestId())
                    .equals(command.request())) {

                return DuplicateResolutionMapper.blocked(
                        "DUPLICATE_REQUEST_SCOPE_MISMATCH");
            }

            var history = command.history().appendRegistration(
                    command.candidate(),
                    command.coverage(),
                    command.evidence(),
                    command.evidenceRevision(),
                    command.authority(),
                    command.reviewed(),
                    command.reason(),
                    command.context());

            var entry = history.entries().getLast();

            if (!entry.request().equals(command.request())) {
                return DuplicateResolutionMapper.blocked(
                        "DUPLICATE_HISTORY_SCOPE_MISMATCH");
            }

            boolean exception =
                    entry.decision().outcome()
                    == DuplicateExceptionPolicy.Outcome.EXCEPTION_CANDIDATE;

            if (exception) {
                if (command.eventId() == null
                        || command.eventId().isBlank()) {
                    return new DuplicateResolutionResult(
                            DuplicateResolutionResult.Status.BLOCKED,
                            "DUPLICATE_EVENT_CONTEXT_REQUIRED",
                            entry.auditRequired(),
                            Optional.empty(),
                            false);
                }

                DuplicateEventDraft.fromCandidate(
                        entry,
                        command.eventId(),
                        Optional.empty());
            }

            return DuplicateResolutionMapper.map(
                    entry, exception);

        } catch (IllegalArgumentException failure) {

            return DuplicateResolutionMapper.blocked(
                    failure.getMessage());
        }
    }
}