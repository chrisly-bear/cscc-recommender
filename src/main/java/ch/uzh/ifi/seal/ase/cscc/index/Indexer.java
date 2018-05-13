package ch.uzh.ifi.seal.ase.cscc.index;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import ch.uzh.ifi.seal.ase.cscc.utils.IoHelper;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;

import java.io.File;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Indexer {

    public static void indexAllContexts(String contextsDirectory) {
        InvertedIndex index = new InvertedIndex();
        List<String> zips = IoHelper.findAllZips(contextsDirectory);
        int zipTotal = zips.size();
        int zipCount = 0;
        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;
            zipCount++;

            System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                    perc);

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {
                while (ra.hasNext()) {
                    Context ctx = ra.getNext(Context.class);
                    List<IndexDocument> indexDocuments = createIndexDocumentsFromKaVEContext(ctx);
                    indexDocuments.forEach(index::indexDocument);
                }
            }
        }
        index.persistToDisk(Paths.get("").toAbsolutePath().toString()+"/serializedIndex");
    }

    private static List<IndexDocument> createIndexDocumentsFromKaVEContext(Context ctx) {
        ISST sst = ctx.getSST();
        IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
        List<IndexDocument> indexDocuments = new ArrayList<>();
        sst.accept(indexDocumentExtractionVisitor, indexDocuments);
        return indexDocuments;
    }

}
