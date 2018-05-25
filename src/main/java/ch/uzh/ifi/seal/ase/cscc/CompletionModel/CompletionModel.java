package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import ch.uzh.ifi.seal.ase.cscc.index.*;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;

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

        ISSTNodeVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
        List<IndexDocument> indexDocuments = new LinkedList<>();

        sst.accept(indexDocumentExtractionVisitor, indexDocuments);

        for (IndexDocument document : indexDocuments) {
            index.indexDocument(document);
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
