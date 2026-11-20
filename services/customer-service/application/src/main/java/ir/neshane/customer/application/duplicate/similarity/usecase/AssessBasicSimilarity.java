package ir.neshane.customer.application.duplicate.similarity.usecase;

import ir.neshane.customer.application.duplicate.similarity.query.BasicSimilarityQuery;
import ir.neshane.customer.application.duplicate.similarity.query.BasicSimilarityResult;
import ir.neshane.customer.application.port.in.BasicSimilarityUseCase;
import ir.neshane.customer.domain.duplicate.BasicSimilarityDetection;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AssessBasicSimilarity
        implements BasicSimilarityUseCase {

    @Override
    public BasicSimilarityResult execute(
            BasicSimilarityQuery query) {

        Objects.requireNonNull(query, "QUERY_REQUIRED");

        try {
            var comparison = BasicSimilarityDetection.compare(
                    query.candidate(),
                    query.request(),
                    query.existing());

            var signals = comparison.signals().stream()
                    .map(signal -> switch (signal) {
                        case NAME ->
                            BasicSimilarityResult.Signal.NAME;
                        case PHONE ->
                            BasicSimilarityResult.Signal.PHONE;
                        case EMAIL ->
                            BasicSimilarityResult.Signal.EMAIL;
                        default ->
                            throw new IllegalArgumentException(
                                "SIMILARITY_SIGNAL_UNSUPPORTED");
                    })
                    .collect(Collectors.toUnmodifiableSet());

            return new BasicSimilarityResult(
                    signals.isEmpty()
                        ? BasicSimilarityResult.Status.NO_MATCH
                        : BasicSimilarityResult.Status.POTENTIAL_DUPLICATE,
                    signals.isEmpty()
                        ? "SIMILARITY_NO_MATCH_CANDIDATE"
                        : "SIMILARITY_WARNING",
                    signals);

        } catch (IllegalArgumentException exception) {

            var message = exception.getMessage();

            var code =
                    "DUPLICATE_REQUEST_SCOPE_MISMATCH".equals(message)
                    || "DUPLICATE_EVIDENCE_SCOPE_MISMATCH".equals(message)
                    ? message
                    : "SIMILARITY_ASSESSMENT_BLOCKED";

            return new BasicSimilarityResult(
                    BasicSimilarityResult.Status.BLOCKED,
                    code, Set.of());
        }
    }
}