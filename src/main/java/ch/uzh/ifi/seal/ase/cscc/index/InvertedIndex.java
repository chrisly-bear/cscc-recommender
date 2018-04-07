package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InvertedIndex {

    // INDEX 1: Type Name - Method Names
    private HashMap<String, List<String>> index_TypeMethod;

    // INDEX 2: Method Name - Index Documents
    private HashMap<String, List<IndexDocument>> index_MethodDocument;

    public InvertedIndex() {
        this.index_TypeMethod = new HashMap<>();
        this.index_MethodDocument = new HashMap<>();
    }

    public void indexDocument(IndexDocument doc) {
        // TODO 3.2:
        // ...
    }

    /**
     * Get 3 code completion suggestions for the receiver object.
     * @param receiverObj object, on which the code completion is called
     * @return names of the methods that are suggested for code completion
     */
    public List<String> getRecommendation(IndexDocument receiverObj) {
        List<IndexDocument> baseCandidates = getBaseCandidates(receiverObj);
        List<IndexDocument> refinedCandidates = getRefindedCandidates(baseCandidates, receiverObj);
        List<IndexDocument> sortedRefinedCandidates = sortRefindedCandidates(refinedCandidates, receiverObj);
        List<IndexDocument> recommendations = getTopThreeCandidates(sortedRefinedCandidates);
        List<String> recommendationsMethodNames = getMethodNames(recommendations);
        return recommendationsMethodNames;
    }

    private List<IndexDocument> getBaseCandidates(IndexDocument receiverObj) {
        // TODO 3.3.1:
        // ...
        return null;
    }

    private List<IndexDocument> getRefindedCandidates(List<IndexDocument> baseCandidates, IndexDocument receiverObj) {
        // TODO 3.3.2:
        // ...
        // use IndexDocument.lineContextHammingDistanceToOther(IndexDocument)
        // and IndexDocument.overallContextHammingDistanceToOther(IndexDocument)
        // to get hamming distance between baseCandidate and receiverObj
        return null;
    }

    private List<IndexDocument> sortRefindedCandidates(List<IndexDocument> refinedCandidates, IndexDocument receiverObj) {
        // TODO 3.3.3:
        // ...
        // use IndexDocument.longestCommonSubsequenceOverallContextToOther(IndexDocument)
        // and IndexDocument.levenshteinDistanceLineContextToOther(IndexDocument)
        return null;
    }

    private List<IndexDocument> getTopThreeCandidates(List<IndexDocument> sortedRefinedCandidates) {
        // TODO 3.3.4:
        // ...
        return null;
    }

    private List<String> getMethodNames(List<IndexDocument> recommendations) {
        List<String> methodNames = new LinkedList<>();
        for (IndexDocument doc : recommendations) {
            methodNames.add(doc.getMethodCall());
        }
        return methodNames;
    }

}
