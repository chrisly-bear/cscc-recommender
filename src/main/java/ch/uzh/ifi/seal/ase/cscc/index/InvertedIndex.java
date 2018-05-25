package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Deprecated: Use InMemoryInvertedIndex or DiskBasedInvertedIndex which are based on Lucene.
 * This is the original implementation of our own Inverted Index design.
 */
@Deprecated
public class InvertedIndex implements IInvertedIndex {

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures";
    private final Logger LOGGER = Logger.getLogger(InvertedIndex.class.getName());

    // INDEX 1
    // Type - Inverted Index Structure
    private Map<String, InvertedIndexStructure> typeIndex = new HashMap<>();

    /**
     * Indexes doc in the two-level inverted index.
     * @param doc document which to put in index
     */
    public void indexDocument(IndexDocument doc) {
        String docType = doc.getType();
        InvertedIndexStructure invertedIndexStructure = typeIndex.get(docType);
        if (invertedIndexStructure == null) {
            invertedIndexStructure = new InvertedIndexStructure(docType);
            typeIndex.put(docType, invertedIndexStructure);
        }
        invertedIndexStructure.indexDoc(doc);
    }

    /**
     * Searches for all documents which map doc's type and contain similar terms in the overall context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in common with doc's overall context
     */
    public Set<IndexDocument> search(IndexDocument doc) {
        Set<IndexDocument> answers = new HashSet<>();
        String docType = doc.getType();
        InvertedIndexStructure invertedIndexStructure = typeIndex.get(docType);
        if (invertedIndexStructure != null) {
            answers = invertedIndexStructure.search(doc);
        }
        return answers;
    }

    /**
     * Warning: If {@code targetDir} already contains a persisted inverted index, that
     * inverted index will be overwritten.
     * @param targetDir directory in which to store the inverted index
     */
    public void persistToDisk(String targetDir) throws IOException {

        // create top level directory
        String invertedIndexDirPath = targetDir + "/" + INDEX_ROOT_DIR_NAME;
        File invertedIndexDir = new File(invertedIndexDirPath);
        deleteDirectoryIfExists(invertedIndexDir);
        invertedIndexDir.mkdir();

        // create directory for contexts
        String contextsDirPath = invertedIndexDirPath + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
        File contextsDir = new File(contextsDirPath);
        contextsDir.mkdir();

        // create directory for inverted index structures
        String invertedIndexStructuresDirPath = invertedIndexDirPath + "/" + INVERTED_INDEX_STRUCTURES_DIR_NAME;
        File invertedIndexStructuresDir = new File(invertedIndexStructuresDirPath);
        invertedIndexStructuresDir.mkdir();

        for (InvertedIndexStructure invertedIndexStructure : typeIndex.values()) {
            // serialize contexts (IndexDocument objects) stored in this inverted index structure to disk
            storeContexts(contextsDirPath, invertedIndexStructure.getIndexedDocs());
            // store inverted index structure (InvertedIndexStructure objects) to disk
            storeInvertedIndexStructure(invertedIndexStructuresDir, invertedIndexStructure);
        }
    }

    private void deleteDirectoryIfExists(File dir) throws IOException {
        if (dir.exists()) {
            System.out.println("'" + dir + "' directory already exists. Deleting it... ");
            FileUtils.deleteDirectory(dir);
        }
    }

    private void storeContexts(String contextsDirPath, Set<IndexDocument> contexts) throws IOException {
        for (IndexDocument doc : contexts) {
            serializeIndexDocument(contextsDirPath, doc);
        }
    }

    private void serializeIndexDocument(String contextsDirPath, IndexDocument doc) throws IOException {
        String contextPath = contextsDirPath + "/" + doc.getId() + ".ser";
        if (new File(contextPath).exists()) {
            LOGGER.info("Collision during serialization of IndexDocument: Doc with ID '" + doc.getId() + "' already exists!");
        }
        FileOutputStream fileOut = new FileOutputStream(contextPath);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(doc);
        out.close();
        fileOut.close();
//        System.out.println("Serialized data is saved in " + contextPath);
    }

    private void storeInvertedIndexStructure(File invertedIndexStructuresDir, InvertedIndexStructure invertedIndexStructure) throws IOException {
        String typeDirPath = invertedIndexStructuresDir + "/" + invertedIndexStructure.representedType;
        File typeDir = new File(typeDirPath);
        typeDir.mkdir();
        for (String methodName : invertedIndexStructure.index.keySet()) {
            String postingsListPath = typeDirPath + "/" + methodName;
            BufferedWriter invertedIndexStructureWriter = new BufferedWriter(new FileWriter(postingsListPath));

            for (IndexDocument posting : invertedIndexStructure.index.get(methodName)) {
                invertedIndexStructureWriter.write(posting.getId());
                invertedIndexStructureWriter.newLine();
            }
            invertedIndexStructureWriter.close();
        }
    }

    public void initializeFromDisk(String sourceDir) throws IOException {
        // initialize contexts
        String contextsDirPath = sourceDir + "/" + INDEX_ROOT_DIR_NAME + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
        Map<String, IndexDocument> docIdIndexDocumentMap = initializeContexts(contextsDirPath);
        // initialize inverted index structures
        String invertedIndexStructuresDirPath = sourceDir + "/" + INDEX_ROOT_DIR_NAME + "/" + INVERTED_INDEX_STRUCTURES_DIR_NAME;
        initializeInvertedIndexStructures(docIdIndexDocumentMap, invertedIndexStructuresDirPath);
    }

    /**
     * @param pathToContexts Folder in which the contexts (serialized IndexDocument objects) are stored
     * @return Map where key is the document ID and value is the IndexDocument object
     */
    private Map<String, IndexDocument> initializeContexts(String pathToContexts) throws IOException {
        Map<String, IndexDocument> docIdIndexDocumentMap = new HashMap<>();
        File contextsDir = new File(pathToContexts);
        FilenameFilter filenameFilter = (dir, name) -> name.endsWith(".ser");
        for (File file : contextsDir.listFiles(filenameFilter)) {
            IndexDocument doc = deserializeIndexDocument(file.getAbsolutePath());
            docIdIndexDocumentMap.put(doc.getId(), doc);
        }
        return docIdIndexDocumentMap;
    }

    private IndexDocument deserializeIndexDocument(String pathToFile) throws IOException {
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

    /**
     * Creates InvertedIndexStructures from postings files stored in {@code pathToInvertedIndexStructures}
     * and puts them into the main index (INDEX 1).
     * @param docIdIndexDocumentMap         Maps document IDs with IndexDocument objects.
     * @param pathToInvertedIndexStructures Directory in which the InvertedIndexStructures are stored.
     */
    private void initializeInvertedIndexStructures(Map<String, IndexDocument> docIdIndexDocumentMap, String pathToInvertedIndexStructures) throws IOException {
        File invertedIndexStructuresDir = new File(pathToInvertedIndexStructures);
        FileFilter directoryFilter = file -> file.isDirectory();
        for (File typeDir : invertedIndexStructuresDir.listFiles(directoryFilter)) {
            // each type will get its own inverted index structure
            String type = typeDir.getName();
            InvertedIndexStructure iis = new InvertedIndexStructure(type);
            iis.initializeFromDisk(typeDir, docIdIndexDocumentMap);
            this.typeIndex.put(type, iis);
        }
    }

    // INDEX 2
    // Context Terms - Contexts
    private class InvertedIndexStructure {

        // we can define stopwords if we want,
        // terms contained in the stoplist will not be added to index
        private List<String> stopwords = Arrays.asList();

        private Map<String, List<IndexDocument>> index;
        private String representedType;
        private Set<IndexDocument> indexedDocs;

        private InvertedIndexStructure(String representedType) {
            this.index = new HashMap<>();
            this.representedType = representedType;
            this.indexedDocs = new HashSet<>();
        }

        private void indexDoc(IndexDocument doc) {
            if (!doc.getType().equals(this.representedType)) {
                // don't index documents which do not match the inverted index structure's type
                return;
            }
            for (String term : doc.getOverallContext()) {
                if (stopwords.contains(term))
                    continue;
                List<IndexDocument> postingsList = index.get(term);
                if (postingsList == null) {
                    postingsList = new LinkedList<>();
                    index.put(term, postingsList);
                }
                postingsList.add(doc);
            }
            indexedDocs.add(doc);
//            System.out.println("indexed " + doc);
        }

        private Set<IndexDocument> search(IndexDocument doc) {
            Set<IndexDocument> answers = new HashSet<>();
            if (!doc.getType().equals(this.representedType)) {
                // abort if we search in an inverted index structure which does not match doc's type
                return answers;
            }
            for (String term : doc.getOverallContext()) {
//                Set<IndexDocument> answersForCurrentTerm = new HashSet<>();
                List<IndexDocument> postingsList = index.get(term);
                if (postingsList != null) {
                    // term exists in index
                    for (IndexDocument posting : postingsList) {
//                        answersForCurrentTerm.add(posting);
                        answers.add(posting);
                    }
                }
//                System.out.println("'" + term + "' found in these documents: ");
//                for (IndexDocument answerDoc : answersForCurrentTerm) {
//                    System.out.println("   " + answerDoc);
//                }
            }
            return answers;
        }

        private Set<IndexDocument> getIndexedDocs() {
            return this.indexedDocs;
        }

        /**
         * Instantiates this InvertedIndexStructure instance from postings lists stored on disk.
         * @param dirContainingPostingsLists Directory where postings files are stored. The directory must have
         *                                   the name of the type this instance represents, e.g.
         *                                   {@code javax.swing.JInternalFrame}. Within this directory, each file should
         *                                   have the name of the index term (i.e. the method name) and contain a set
         *                                   of newline separated document IDs of documents (contexts) which contain the
         *                                   term.
         * @param idIndexDocumentMap         Maps document IDs to IndexDocument objects.
         */
        private void initializeFromDisk(File dirContainingPostingsLists, Map<String, IndexDocument> idIndexDocumentMap) throws IOException {
            if (!dirContainingPostingsLists.isDirectory()) {
                return;
            }
            String type = dirContainingPostingsLists.getName();
            this.representedType = type;
//            System.out.println(this.representedType);
            FileFilter fileFilter = (file) -> file.isFile();
            for (File postingsFile : dirContainingPostingsLists.listFiles(fileFilter)) {
                String methodName = postingsFile.getName();
//                System.out.println("   " + methodName);
                List<String> docIDs = getDocIDsFromPostingsFile(postingsFile);
                List<IndexDocument> indexDocuments = convertDocIDsToIndexDocuments(idIndexDocumentMap, docIDs);
                indexedDocs.addAll(indexDocuments);
                index.put(methodName, indexDocuments);
            }
        }

        private List<String> getDocIDsFromPostingsFile(File postingsFile) throws IOException {
            List<String> docIDs = new LinkedList<>();
            BufferedReader reader = new BufferedReader(new FileReader(postingsFile));
            String id;
            while ((id = reader.readLine()) != null) {
                docIDs.add(id);
//                    System.out.println("      " + id);
            }
            reader.close();
            return docIDs;
        }

        private List<IndexDocument> convertDocIDsToIndexDocuments(Map<String, IndexDocument> idIndexDocumentMap, List<String> docIDs) {
            List<IndexDocument> postings_objects = new LinkedList<>();
            for (String docID : docIDs) {
                IndexDocument docToIndex = idIndexDocumentMap.get(docID);
                postings_objects.add(docToIndex);
            }
            return postings_objects;
        }

    }


}
