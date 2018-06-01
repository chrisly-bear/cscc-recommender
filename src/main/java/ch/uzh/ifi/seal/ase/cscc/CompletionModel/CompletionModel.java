package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import ch.uzh.ifi.seal.ase.cscc.index.IInvertedIndex;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.index.Recommender;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitorNoList;


/**
 * Class representing the model trained by the datasets
 */
public class CompletionModel {

    private IInvertedIndex index = null;

    /**
     * Creates a new completion model, representing the model trained by the datasets
     *
     * @param index An implementation of the IInvertedIndex interface
     */
    public CompletionModel(IInvertedIndex index) {
        this.index = index;
    }

    /**
     * Takes a Context object from the KaVe datasets, creates a new IndexDocument and adds it to the model
     *
     * @param ctx The context object to add to the model
     */
    public void train(Context ctx) {
        ISST sst = ctx.getSST();

        ISSTNodeVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitorNoList();

        sst.accept(indexDocumentExtractionVisitor, index);
    }

    public void startTraining() {
        index.startIndexing();
    }

    public void finishTraining() {
        index.finishIndexing();
    }

    /**
     * Creates a new Recommender for the given IndexDocument, using the underlying model to recommend
     *
     * @param document
     * @return A new recommender
     */
    public Recommender getRecommender(IndexDocument document) {
        return new Recommender(index, document);
    }

}
