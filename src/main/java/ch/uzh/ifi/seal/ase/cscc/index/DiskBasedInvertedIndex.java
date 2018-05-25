package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Disk index.
 * This index will be stored on disk immediately after indexing each document. This is expected to be much
 * slower than the {@code InMemoryInvertedIndex}, but useful for large data sets.
 */
public class DiskBasedInvertedIndex extends AbstractInvertedIndex {

    private final Logger LOGGER = Logger.getLogger(DiskBasedInvertedIndex.class.getName());

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures_Lucene";

    // directory where the Lucene index is persisted on disk
    private String indexFileSystemLocation;
    // set of docIDs to keep track of already indexed documents
    private Set<String> indexedDocs = new HashSet<>();

    /**
     * Constructor
     * @param indexDir directory in which the inverted index will be stored.
     */
    public DiskBasedInvertedIndex(String indexDir) {
        indexFileSystemLocation = indexDir + "/" + INDEX_ROOT_DIR_NAME;
    }

    @Override
    boolean isIndexed(IndexDocument doc) {
        return this.indexedDocs.contains(doc.getId());
    }

    @Override
    void serializeIndexDocument(IndexDocument doc) throws IOException {
        // serialize IndexDocument object and store it on disk
        String contextsDirPath = indexFileSystemLocation + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
        createDirectoryIfNotExists(new File(contextsDirPath));
        String contextPath = contextsDirPath + "/" + doc.getId() + ".ser";
        FileOutputStream fileOut = new FileOutputStream(contextPath);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(doc);
        out.close();
        fileOut.close();
//        System.out.println("Serialized data is saved in " + contextPath);
        /*
         * Careful: If we enable the following line we keep track of the indexed documents
         * in memory. This reduces I/O because we don't have to read an index from disk to
         * see if a document is already in the index. However, it lets our memory explode!
         */
//        this.indexedDocs.add(doc.getId());
    }

    private void createDirectoryIfNotExists(File dir) {
        if (!dir.exists()) {
            System.out.println("'" + dir + "' does not exist yet. Creating it... ");
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1); // exit on IOException
            }
        }
    }

    @Override
    Directory getIndexDirectory(IndexDocument doc) throws IOException {
        String docType = doc.getType();
        String luceneIndexDirPath = indexFileSystemLocation + "/" + INVERTED_INDEX_STRUCTURES_DIR_NAME + "/" + docType;
        FSDirectory fileDirectory = FSDirectory.open(new File(luceneIndexDirPath).toPath());
        return fileDirectory;
    }

    @Override
    IndexDocument deserializeIndexDocument(String docID) throws IOException {
        String pathToFile = this.indexFileSystemLocation + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME + "/" + docID + ".ser";
        IndexDocument doc = null;
        FileInputStream fileIn = new FileInputStream(pathToFile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        try {
            doc = (IndexDocument) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1); // exit on exception
        }
        in.close();
        fileIn.close();
        return doc;
    }

    public void persistToDisk(String targetDir) {
        // empty implementation to conform to interface, index is persisted immediately
        // when a new document is added to the index
    }

    public void initializeFromDisk(String sourceDir) {
        // empty implementation to conform to interface
        LOGGER.warning("Called initializeFromDisk method which does not do anything in " +
                DiskBasedInvertedIndex.class.getSimpleName() + ". Consider removing the call to initializeFromDisk.");
    }
}
