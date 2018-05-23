package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.*;
import java.util.*;

public class InvertedIndex {

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures";
    private static final String DOC_ID_FIELD = "docID";
    private static final String OVERALL_CONTEXT_FIELD = "overallContext";

    // INDEX 1
    // Type - Inverted Index Structure
    private Map<String, InvertedIndexStructure> typeIndex = new HashMap<>();

    // <type, Lucene RAM directory>
    private Map<String, RAMDirectory> ramDirectories = null;
    // <docID, IndexDocument>
    private Map<String, IndexDocument> docsInRAMIndex = null;

    // directory where the Lucene index is persisted on disk
    private String indexFileSystemLocation = null;

    /**
     * In-memory index.
     * Initializing the inverted index without file system directory will make the inverted index work entirely in
     * memory. You can still persist this index after it has been created, but the index creation happens entirely
     * in memory (which can be a problem with large data sets). Initialize with a file system directory to store the
     * index on disk as it is being created.
     */
    public InvertedIndex() {
        ramDirectories = new HashMap<>();
        docsInRAMIndex = new HashMap<>();
    }

    /**
     * Disk index.
     * Initializing the inverted index with a file system directory will make the index be stored on disk immediately
     * after indexing each document. This is expected to be much slower than the in-memory index, but useful for large
     * data sets.
     * @param indexDir file system directory where the index will be stored
     */
    public InvertedIndex(String indexDir) {
        indexFileSystemLocation = indexDir + "/" + INDEX_ROOT_DIR_NAME;
    }

    private boolean isInMemoryIndex() {
        return indexFileSystemLocation == null;
    }

    /**
     * Deprecated: use the `indexDocumentLucene` method instead
     * Indexes doc in the two-level inverted index.
     * @param doc document which to put in index
     */
    @Deprecated
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
     * Uses Apache Lucene indexes instead of InvertedIndexStructure indexes.
     * @param doc document to store in index
     * @throws IOException if the index can't be written to disk
     */
    public void indexDocumentLucene(IndexDocument doc) throws IOException {
        if (isInMemoryIndex()) {
            // if in-memory index -> keep the IndexDocument object in a map
            docsInRAMIndex.put(doc.getId(), doc);
        } else {
            // if disk index -> serialize object and store it on disk
            String contextsDirPath = indexFileSystemLocation + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
            createDirectoryIfNotExists(new File(contextsDirPath));
            serializeIndexDocument(contextsDirPath, doc);
        }
        Directory indexDirectory = getIndexDirectory(doc);
        IndexWriterConfig config = new IndexWriterConfig();
        // CREATE_OR_APPEND creates a new index if one does not exist, otherwise it opens the index and documents will be appended
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        IndexWriter w = new IndexWriter(indexDirectory, config);
        addDocToLuceneIndex(w, doc);
        w.close();
    }

    /**
     * Returns either the RAM directory (if index is in-memory index) or the FSDirectory (if index is disk index).
     * @param doc
     * @return
     * @throws IOException
     */
    private Directory getIndexDirectory(IndexDocument doc) throws IOException {
        Directory result = null;
        String docType = doc.getType();
        if (indexFileSystemLocation != null) {
            String luceneIndexDirPath = indexFileSystemLocation + "/InvertedIndexStructures_Lucene/" + docType;
            FSDirectory fileDirectory = FSDirectory.open(new File(luceneIndexDirPath).toPath());
            result = fileDirectory;
        } else if (ramDirectories != null) {
            RAMDirectory ramDirForGivenType = ramDirectories.get(docType);
            if (ramDirForGivenType == null) {
                // RAMDirectory for this type does not exist yet
                ramDirForGivenType = new RAMDirectory();
                ramDirectories.put(docType, ramDirForGivenType);
            }
            result = ramDirForGivenType;
        }
        return result;
    }

    /**
     * Stores docID and the overall context in the Lucene index. The overall context will be what we search for at
     * retrieval time, the docID will be the result of the retrieval.
     * @param w
     * @param doc
     * @throws IOException
     */
    private void addDocToLuceneIndex(IndexWriter w, IndexDocument doc) throws IOException {
        Document luceneDoc = new Document();
        luceneDoc.add(new StringField(DOC_ID_FIELD, doc.getId(), Field.Store.YES));
        // store all terms in the overall context as tokens in the index
        // StringField: no tokenization
        // TextField: tokenization
        for (String term : doc.getOverallContext()) {
            luceneDoc.add(new StringField(OVERALL_CONTEXT_FIELD, term, Field.Store.YES));
        }
        w.addDocument(luceneDoc);
    }

    private static boolean luceneIndexExistsAndIsReadable(Directory indexDir) {
        boolean existsAndIsReadable = false;
        try {
            IndexReader reader = DirectoryReader.open(indexDir);
            existsAndIsReadable = true;
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return existsAndIsReadable;
    }

    /**
     * Deprecated: use the `searchLucene` method instead
     * Searches for all documents which map doc's type and contain similar terms in the overall context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in common with doc's overall context
     */
    @Deprecated
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
     * Searches the Lucene index for documents which match doc's type and which contain similar terms in the overall
     * context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in
     * common with doc's overall context
     * @throws IOException
     */
    public Set<IndexDocument> searchLucene(IndexDocument doc) throws IOException {
        Set<IndexDocument> answers = new HashSet<>();
        String docType = doc.getType();
        Directory indexDirectory = getIndexDirectory(doc);
        IndexReader reader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        for (String termStr : doc.getOverallContext()) {
            Term term = new Term(OVERALL_CONTEXT_FIELD, termStr);
            Query query = new TermQuery(term);
            boolQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
        }
        Query boolQuery = boolQueryBuilder.build();
        TopDocs docs = searcher.search(boolQuery, Integer.MAX_VALUE); // TODO: not sure if Integer.MAX_VALUE is such a good idea. There is probably a reason why Lucene does not offer to retrieve all matches at once.
        ScoreDoc[] hits = docs.scoreDocs;
        for (ScoreDoc hit : hits) {
            int luceneDocID = hit.doc;
            Document luceneDoc = searcher.doc(luceneDocID);
            String docID = luceneDoc.get(DOC_ID_FIELD);
            System.out.println(docID);
            // deserialize IndexDocument object with the given docID
            if (isInMemoryIndex()) {
                IndexDocument matchingDoc = docsInRAMIndex.get(docID);
                answers.add(matchingDoc);
            } else {
                IndexDocument matchingDoc = deserializeIndexDocument(this.indexFileSystemLocation + "/" + "IndexDocuments/" + docID + ".ser");
                answers.add(matchingDoc);
            }
        }
        return answers;
    }

    /**
     * Deprecated: use the `persistToDiskLucene` method instead
     * Warning: If {@code targetDir} already contains a persisted inverted index, that
     * inverted index will be overwritten.
     * @param targetDir directory in which to store the inverted index
     */
    @Deprecated
    public void persistToDisk(String targetDir) {

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

    public void persistToDiskLucene(String targetDir) {
        if (isInMemoryIndex()) {
            // TODO: persist Lucene in-memory index to disk
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

    private void deleteDirectoryIfExists(File dir) {
        if (dir.exists()) {
            System.out.println("'" + dir + "' directory already exists. Deleting it... ");
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeContexts(String contextsDirPath, Set<IndexDocument> contexts) {
        for (IndexDocument doc : contexts) {
            serializeIndexDocument(contextsDirPath, doc);
        }
    }

    private void serializeIndexDocument(String contextsDirPath, IndexDocument doc) {
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

    private void storeInvertedIndexStructure(File invertedIndexStructuresDir, InvertedIndexStructure invertedIndexStructure) {
        String typeDirPath = invertedIndexStructuresDir + "/" + invertedIndexStructure.representedType;
        File typeDir = new File(typeDirPath);
        typeDir.mkdir();
        for (String methodName : invertedIndexStructure.index.keySet()) {
            try {
                String postingsListPath = typeDirPath + "/" + methodName;
                BufferedWriter invertedIndexStructureWriter = new BufferedWriter(new FileWriter(postingsListPath));

                for (IndexDocument posting : invertedIndexStructure.index.get(methodName)) {
                    invertedIndexStructureWriter.write(posting.getId());
                    invertedIndexStructureWriter.newLine();
                }
                invertedIndexStructureWriter.close();
            } catch (IOException e) {
                // ignore for now
                // TODO investigate question marks as method names
            }
        }
    }

    public void initializeFromDisk(String sourceDir) {
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
    private Map<String, IndexDocument> initializeContexts(String pathToContexts) {
        Map<String, IndexDocument> docIdIndexDocumentMap = new HashMap<>();
        File contextsDir = new File(pathToContexts);
        FilenameFilter filenameFilter = (dir, name) -> name.endsWith(".ser");
        for (File file : contextsDir.listFiles(filenameFilter)) {
            IndexDocument doc = deserializeIndexDocument(file.getAbsolutePath());
            docIdIndexDocumentMap.put(doc.getId(), doc);
        }
        return docIdIndexDocumentMap;
    }

    private IndexDocument deserializeIndexDocument(String pathToFile) {
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

    /**
     * Creates InvertedIndexStructures from postings files stored in {@code pathToInvertedIndexStructures}
     * and puts them into the main index (INDEX 1).
     * @param docIdIndexDocumentMap         Maps document IDs with IndexDocument objects.
     * @param pathToInvertedIndexStructures Directory in which the InvertedIndexStructures are stored.
     */
    private void initializeInvertedIndexStructures(Map<String, IndexDocument> docIdIndexDocumentMap, String pathToInvertedIndexStructures) {
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
        private void initializeFromDisk(File dirContainingPostingsLists, Map<String, IndexDocument> idIndexDocumentMap) {
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

        private List<String> getDocIDsFromPostingsFile(File postingsFile) {
            List<String> docIDs = new LinkedList<>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(postingsFile));
                String id;
                while ((id = reader.readLine()) != null) {
                    docIDs.add(id);
//                    System.out.println("      " + id);
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
