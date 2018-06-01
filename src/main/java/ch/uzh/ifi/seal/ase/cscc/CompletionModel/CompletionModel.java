package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import ch.uzh.ifi.seal.ase.cscc.index.IInvertedIndex;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.index.Recommender;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitorNoList;

public class CompletionModel {
    
    private IInvertedIndex index = null;

    public CompletionModel(IInvertedIndex index) {
        this.index = index;
    }

    public void startTraining() {
        index.startIndexing();
    }

    public void finishTraining() {
        index.finishIndexing();
    }

    public void train(Context ctx) {
        ISST sst = ctx.getSST();
        ISSTNodeVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitorNoList();
        sst.accept(indexDocumentExtractionVisitor, index);
    }

    public Recommender getRecommender(IndexDocument document) {
        return new Recommender(index, document);
    }

}
