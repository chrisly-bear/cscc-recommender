package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import ch.uzh.ifi.seal.ase.cscc.index.*;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitorNoList;

import java.io.IOException;
import java.nio.file.*;

public class CompletionModel {
    
    private IInvertedIndex index = CSCCConfiguration.getNewInvertedIndexInstance();

    public CompletionModel() {
    }

    public static CompletionModel fromDisk(String modelDir) {
        CompletionModel model = new CompletionModel();
        try {
            model.index.initializeFromDisk(modelDir);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // exit on IOException
        }
        return model;
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

    public void store(String modelOutputDir) {
        try {
            index.persistToDisk(modelOutputDir);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // exit on IOException
        }
    }
}
