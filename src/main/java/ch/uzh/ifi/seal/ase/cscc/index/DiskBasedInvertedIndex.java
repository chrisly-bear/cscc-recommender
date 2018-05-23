package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;

/**
 * Disk index.
 * This index will be stored on disk immediately after indexing each document. This is expected to be much
 * slower than the {@code InMemoryInvertedIndex}, but useful for large data sets.
 */
public class DiskBasedInvertedIndex extends AbstractInvertedIndex {

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures_Lucene";

    // directory where the Lucene index is persisted on disk
    private String indexFileSystemLocation;

    /**
     * Constructor
     * @param indexDir directory in which the inverted index will be stored.
     */
    public DiskBasedInvertedIndex(String indexDir) {
        indexFileSystemLocation = indexDir + "/" + INDEX_ROOT_DIR_NAME;
    }

    @Override
    void serializeIndexDocument(IndexDocument doc) {
        // serialize IndexDocument object and store it on disk
        String contextsDirPath = indexFileSystemLocation + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
        createDirectoryIfNotExists(new File(contextsDirPath));
        try {
            String contextPath = contextsDirPath + "/" + doc.getId() + ".ser";
            FileOutputStream fileOut = new FileOutputStream(contextPath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(doc);
            out.close();
            fileOut.close();
//                System.out.println("Serialized data is saved in " + contextPath);
        } catch (IOException e) {
            // ignore for now
            // TODO investigate question marks as method names
        }
    }

    private void createDirectoryIfNotExists(File dir) {
        if (!dir.exists()) {
            System.out.println("'" + dir + "' does not exist yet. Creating it... ");
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                e.printStackTrace();
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
    IndexDocument deserializeIndexDocument(String docID) {
        String pathToFile = this.indexFileSystemLocation + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME + "/" + docID + ".ser";
        IndexDocument doc = null;
        try {
            FileInputStream fileIn = new FileInputStream(pathToFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            doc = (IndexDocument) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return doc;
    }

}
