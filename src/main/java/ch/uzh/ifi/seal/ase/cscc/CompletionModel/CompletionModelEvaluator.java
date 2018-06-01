package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.index.Recommender;

/**
 * Class holding a {@link CompletionModel} for evaluation of that model
 */
public class CompletionModelEvaluator {
    private int recommendationsRequested = 0;
    private int recommendationsMade = 0;
    private int recommendationsRelevant = 0;
    private CompletionModel model;

    /**
     * Creates a new {@link CompletionModelEvaluator} holding the a {@link CompletionModel}
     * @param model The {@link CompletionModel}
     */
    public CompletionModelEvaluator(CompletionModel model) {
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

    /**
     * Get the precision achieved by this model, i.e relevant recommendations / requested recommendations
     * @return
     */
    public float getPrecision() {
        return 100.f * recommendationsRelevant / recommendationsRequested;
    }

    /**
     * Get the recall achieved by this model, i.e. made recommendations / requested recommendations
     * @return
     */
    public float getRecall() {
        return 100.f * recommendationsMade / recommendationsRequested;
    }
}
