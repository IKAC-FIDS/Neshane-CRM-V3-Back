package ir.neshane.customer.application.port.in;

import ir.neshane.customer.application.duplicate.similarity.query.BasicSimilarityQuery;
import ir.neshane.customer.application.duplicate.similarity.query.BasicSimilarityResult;

public interface BasicSimilarityUseCase {

    BasicSimilarityResult execute(
            BasicSimilarityQuery query);
}