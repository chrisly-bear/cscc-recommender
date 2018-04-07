package ch.uzh.ifi.seal.ase.cscc.index;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import ch.uzh.ifi.seal.ase.cscc.utils.IoHelper;

import java.io.File;
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
                    IndexDocument doc = createIndexDocumentFromKaVEContext(ctx);
                    index.indexDocument(doc);
                }
            }
        }
    }

    private static IndexDocument createIndexDocumentFromKaVEContext(Context ctx) {
        // TODO 3.1:
        // build a IndexDocument object from a KaVE context
        return null;
    }

}
