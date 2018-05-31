package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import ch.uzh.ifi.seal.ase.cscc.index.*;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitorNoList;

import java.io.IOException;
import java.nio.file.*;

public class CompletionModel {
    
    private IInvertedIndex index = null;

    public CompletionModel(IInvertedIndex index) {
        this.index = index;
    }

    public void train(Context ctx) {
        ISST sst = ctx.getSST();

        ISSTNodeVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitorNoList();

        sst.accept(indexDocumentExtractionVisitor, index);
    }

    /**
     * Clean things up in the IInvertedIndex when we don't need the instance anymore.
     */
    public void cleanUp() {
        if (index instanceof DiskBasedInvertedIndex) {
            System.out.println("Closing DiskBasedInvertedIndex cleanly...");
            DiskBasedInvertedIndex dbindex = (DiskBasedInvertedIndex) index;
            dbindex.cleanUp();
        } else if (index instanceof ParallelizedInvertedIndex) {
            System.out.println("Closing ParallelizedInvertedIndex cleanly...");
            ParallelizedInvertedIndex pindex = (ParallelizedInvertedIndex) index;
            pindex.cleanUp();
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public Recommender getRecommender(IndexDocument document) {
        return new Recommender(index, document);
    }

}
