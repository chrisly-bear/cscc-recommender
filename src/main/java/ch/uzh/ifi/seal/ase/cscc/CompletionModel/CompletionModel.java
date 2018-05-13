package ch.uzh.ifi.seal.ase.cscc.CompletionModel;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.index.InvertedIndex;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;

public class CompletionModel {
    private InvertedIndex index = new InvertedIndex();

    public CompletionModel(String modelOutputDir) throws IOException {
        Path path = Paths.get(modelOutputDir);

        try {
            if (!isDirectoryEmpty(path)) throw new IOException("provided non-empty directory");
        } catch (NoSuchFileException e) {
            Files.createDirectory(path);
        }
    }

    public void train(Context ctx) {
        ISST sst = ctx.getSST();

        IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
        List<IndexDocument> indexDocuments = new LinkedList<>();

        sst.accept(indexDocumentExtractionVisitor, indexDocuments);

        for (IndexDocument document : indexDocuments) {
            index.indexDocument(document);
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }
}
