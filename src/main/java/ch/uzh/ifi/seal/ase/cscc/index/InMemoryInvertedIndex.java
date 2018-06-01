package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory index.
 * This index will work entirely in memory. You can still persist this index after it has been created, but the
 * index creation happens entirely in memory (which can be a problem with large data sets). Use
 * {@code DiskBasedInvertedIndex} for data sets which cannot be indexed in-memory.
 */
public class InMemoryInvertedIndex extends AbstractInvertedIndex {

    // <docID, IndexDocument>
    private Map<String, IndexDocument> docsInRAMIndex = new HashMap<>();
    private RAMDirectory directory = new RAMDirectory();

    public InMemoryInvertedIndex() {
        super.initializeIndexDirectory();
    }

    @Override
    boolean isIndexed(IndexDocument doc) {
        return docsInRAMIndex.containsKey(doc.getId());
    }

    @Override
    void serializeIndexDocument(IndexDocument doc) {
        // keep the IndexDocument object in a map
        docsInRAMIndex.put(doc.getId(), doc);
    }

    @Override
    Directory getIndexDirectory() {
        return directory;
    }

    @Override
    IndexDocument deserializeIndexDocument(String docID) {
        return docsInRAMIndex.get(docID);
    }

}
