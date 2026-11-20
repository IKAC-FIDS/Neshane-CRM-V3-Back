package ir.neshane.customer.application.duplicate.resolution.usecase;

import ir.neshane.customer.application.duplicate.resolution.command.DuplicateReuseCommand;
import ir.neshane.customer.application.duplicate.resolution.command.DuplicateResolutionResult;
import ir.neshane.customer.application.port.in.DuplicateReuseUseCase;

import ir.neshane.customer.domain.duplicate.DuplicateDetection;

import java.util.Objects;

public final class AssessDuplicateReuse
        implements DuplicateReuseUseCase {

    @Override
    public DuplicateResolutionResult execute(
            DuplicateReuseCommand command) {

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

            var history = command.history().appendReuse(
                    command.candidate(),
                    command.coverage(),
                    command.evidence(),
                    command.evidenceRevision(),
                    command.reviewed(),
                    command.selected(),
                    command.visibleSubjects(),
                    command.context());

            var entry = history.entries().getLast();

            if (!entry.request().equals(command.request())) {
                return DuplicateResolutionMapper.blocked(
                        "DUPLICATE_HISTORY_SCOPE_MISMATCH");
            }

            return DuplicateResolutionMapper.map(
                    entry, false);

        } catch (IllegalArgumentException failure) {

            return DuplicateResolutionMapper.blocked(
                    failure.getMessage());
        }
    }
}