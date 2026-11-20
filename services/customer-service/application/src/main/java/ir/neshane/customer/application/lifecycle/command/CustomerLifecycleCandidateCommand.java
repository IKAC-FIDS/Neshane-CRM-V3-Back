package ir.neshane.customer.application.lifecycle.command;

import ir.neshane.customer.domain.customer.model.CustomerRecordVersion;
import ir.neshane.customer.domain.customer.state.CustomerUpdatePolicy;
import ir.neshane.customer.domain.customer.state.CustomerLifecycleIntegration;
import ir.neshane.customer.domain.customer.state.CrossAggregateLifecycle;
import ir.neshane.customer.domain.customer.history.LifecycleHistory;
import ir.neshane.customer.domain.lifecycle.LifecycleReasonCatalog;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyCatalog;
import ir.neshane.customer.domain.lifecycle.ArchiveDependencyAssessment;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Internal synthetic lifecycle assessment command.
 * Does not authorize or persist lifecycle transitions.
 */
public record CustomerLifecycleCandidateCommand(
        TransitionFacts transition,
        HistoryFacts history,
        DependencyFacts dependencies) {

    public CustomerLifecycleCandidateCommand {
        Objects.requireNonNull(transition);
        Objects.requireNonNull(history);
        Objects.requireNonNull(dependencies);
    }

    public record TransitionFacts(
            CustomerUpdatePolicy.Current current,
            CustomerRecordVersion expectedVersion,
            CustomerLifecycleIntegration.Context context) {

        public TransitionFacts {
            Objects.requireNonNull(current);
            Objects.requireNonNull(context);
        }

        @Override
        public String toString() {
            return "TransitionFacts[redacted]";
        }
    }

    public record HistoryFacts(
            LifecycleReasonCatalog reasons,
            LifecycleHistory history) {

        @Override
        public String toString() {
            return "HistoryFacts[redacted]";
        }
    }

    public record DependencyFacts(
            ArchiveDependencyCatalog catalog,
            List<ArchiveDependencyAssessment.Evidence> externalEvidence,
            CrossAggregateLifecycle.Snapshot snapshot,
            LocalDate assessmentDay) {

        public DependencyFacts {
            if (externalEvidence != null) {
                externalEvidence = List.copyOf(externalEvidence);
            }
        }

        @Override
        public String toString() {
            return "DependencyFacts[redacted]";
        }
    }

    @Override
    public String toString() {
        return "CustomerLifecycleCandidateCommand[redacted]";
    }
}
