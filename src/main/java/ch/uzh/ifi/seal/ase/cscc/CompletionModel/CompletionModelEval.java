package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.index.Recommender;

public class CompletionModelEval {
    private int recommendationsRequested = 0;
    private int recommendationsMade = 0;
    private int recommendationsRelevant = 0;
    private CompletionModel model;

    public CompletionModelEval(CompletionModel model) {
        this.model = model;
    }

    /**
     * Test if our model recommends the given method.
     * @param document The ground truth method to test against. If this method is in our recommendation then we
     *                 consider this recommendation as correct.
     */
    public void evaluate(IndexDocument document) {
        Recommender recommender = model.getRecommender(document);

        recommendationsRequested++;

        if (recommender.containsTopThree(document)) {
            recommendationsMade++;
            recommendationsRelevant++;
        } else if (recommender.contains(document)) {
            recommendationsMade++;
        }
    }

    public float getPrecision() {
        return 100.f * recommendationsRelevant / recommendationsRequested;
    }

    public float getRecall() {
        return 100.f * recommendationsMade / recommendationsRequested;
    }
}
