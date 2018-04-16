package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.*;

public class InvertedIndex {

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
    public void persistToDisk(String targetDir) {

        // create top level directory
        String invertedIndexDirPath = targetDir + "/InvertedIndex";
        File invertedIndexDir = new File(invertedIndexDirPath);
        deleteDirectoryIfExists(invertedIndexDir);
        invertedIndexDir.mkdir();

        // create directory for contexts
        String contextsDirPath = invertedIndexDirPath + "/IndexDocuments";
        File contextDir = new File(contextsDirPath);
        contextDir.mkdir();

        // create directory for inverted index structures
        String invertedIndexStructuresDirPath = invertedIndexDirPath + "/InvertedIndexStructures";
        File invertedIndexStructuresDir = new File(invertedIndexStructuresDirPath);
        invertedIndexStructuresDir.mkdir();

        for (InvertedIndexStructure invertedIndexStructure : typeIndex.values()) {
            // serialize contexts (IndexDocument objects) stored in this inverted index structure to disk
            storeContexts(contextsDirPath, invertedIndexStructure.getIndexedDocs());
            // store inverted index structure (InvertedIndexStructure objects) to disk
            storeInvertedIndexStructure(invertedIndexStructuresDir, invertedIndexStructure);
        }
    }

    private void deleteDirectoryIfExists(File dir) {
        if (dir.exists()) {
            System.out.print("'InvertedIndex' directory already exists. Deleting it... ");
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeContexts(String contextsDirPath, Set<IndexDocument> contexts) {
        for (IndexDocument doc : contexts) {
            try {
                FileOutputStream fileOut = new FileOutputStream(contextsDirPath + "/" + doc.getMethodCall() + "_" + doc.getId() + ".ser");
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(doc);
                out.close();
                fileOut.close();
                System.out.println("Serialized data is saved in " + fileOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeInvertedIndexStructure(File invertedIndexStructuresDir, InvertedIndexStructure invertedIndexStructure) {
        String typeDirPath = invertedIndexStructuresDir + "/" + invertedIndexStructure.representedType;
        File typeDir = new File(typeDirPath);
        typeDir.mkdir();
        try {
            for (String methodName : invertedIndexStructure.index.keySet()) {
                String postingsListPath = typeDirPath + "/" + methodName;
                BufferedWriter invertedIndexStructureWriter = new BufferedWriter(new FileWriter(postingsListPath));

                for (IndexDocument posting : invertedIndexStructure.index.get(methodName)) {
                    invertedIndexStructureWriter.write(posting.getId());
                    invertedIndexStructureWriter.newLine();
                }
                invertedIndexStructureWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeFromDisk(String sourceDir) {
        // TODO 3.2.4: initialize index from disk
        // ...
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

    }

}
