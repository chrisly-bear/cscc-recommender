package ch.uzh.ifi.seal.ase.cscc.index;

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

    // INDEX 2
    // Context Terms - Contexts
    private class InvertedIndexStructure {

        // we can define stopwords if we want,
        // terms contained in the stoplist will not be added to index
        private List<String> stopwords = Arrays.asList();

        private Map<String, List<IndexDocument>> index;
        private String representedType;

        private InvertedIndexStructure(String representedType) {
            this.index = new HashMap<>();
            this.representedType = representedType;
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
            System.out.println("indexed " + doc); // TODO: debug code, remove
        }

        private Set<IndexDocument> search(IndexDocument doc) {
            Set<IndexDocument> answers = new HashSet<>();
            if (!doc.getType().equals(this.representedType)) {
                // abort if we search in an inverted index structure which does not match doc's type
                return answers;
            }
            for (String term : doc.getOverallContext()) {
                Set<IndexDocument> answersForCurrentTerm = new HashSet<>(); // TODO: debug code, remove
                List<IndexDocument> postingsList = index.get(term);
                if (postingsList != null) {
                    // term exists in index
                    for (IndexDocument posting : postingsList) {
                        answersForCurrentTerm.add(posting); // TODO: debug code, remove
                        answers.add(posting);
                    }
                }
                // TODO: debug code, remove
                System.out.println("'" + term + "' found in these documents: ");
                for (IndexDocument answerDoc : answersForCurrentTerm) {
                    System.out.println("   " + answerDoc);
                }
            }
            return answers;
        }
    }

}
