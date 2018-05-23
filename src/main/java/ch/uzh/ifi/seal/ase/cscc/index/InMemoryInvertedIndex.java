package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory index.
 * This index will work entirely in memory. You can still persist this index after it has been created, but the
 * index creation happens entirely in memory (which can be a problem with large data sets). Use
 * {@code DiskBasedInvertedIndex} for data sets which cannot be indexed in-memory.
 */
public class InMemoryInvertedIndex extends AbstractInvertedIndex {

    // <type, Lucene RAM directory>
    private Map<String, RAMDirectory> ramDirectories = new HashMap<>();
    // <docID, IndexDocument>
    private Map<String, IndexDocument> docsInRAMIndex = new HashMap<>();

    @Override
    void serializeIndexDocument(IndexDocument doc) {
        // keep the IndexDocument object in a map
        docsInRAMIndex.put(doc.getId(), doc);
    }

    @Override
    Directory getIndexDirectory(IndexDocument doc) throws IOException {
        String docType = doc.getType();
        RAMDirectory ramDirForGivenType = ramDirectories.get(docType);
        if (ramDirForGivenType == null) {
            // RAMDirectory for this type does not exist yet
            ramDirForGivenType = new RAMDirectory();
            ramDirectories.put(docType, ramDirForGivenType);
        }
        return ramDirForGivenType;
    }

    @Override
    IndexDocument deserializeIndexDocument(String docID) {
        return docsInRAMIndex.get(docID);
    }

    public void persistToDiskLucene(String targetDir) {
        // TODO: persist Lucene in-memory index to disk
    }

    public void initializeFromDisk(String sourceDir) {
        // TODO: implement
    }

}
