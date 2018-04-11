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
        // TODO: test if this method does everything correctly
        int threshold = 30; // TODO: this threshold was picked at random and was never tested (maybe a good value is mentioned in the paper?)
        int k = 200;

        List<ScoredIndexDocument> scoredBaseCandidates = new LinkedList<>();
        for (IndexDocument baseCandidate : baseCandidates) {
            int lineContextDistance = baseCandidate.lineContextHammingDistanceToOther(receiverObj);
            int overallContextDistance = baseCandidate.overallContextHammingDistanceToOther(receiverObj);
            int viableDistance = overallContextDistance > threshold ? lineContextDistance : overallContextDistance;
            // we take the negative distance so that after sorting, candidates with lower distance (i.e. higher score) will come first
            ScoredIndexDocument scoredDoc = new ScoredIndexDocument(baseCandidate, -viableDistance, 0);
            scoredBaseCandidates.add(scoredDoc);
        }
        scoredBaseCandidates.sort(null); // compare using the Comparable interface implemented in ScoredIndexDocument
        // get the k top candidates
        List<ScoredIndexDocument> refinedScoredCandidates = scoredBaseCandidates.subList(0, Math.min(k,scoredBaseCandidates.size()));
        List<IndexDocument> refinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : refinedScoredCandidates) {
            refinedCandidates.add(sid.getIndexDocumentWithoutScore());
        }
        return refinedCandidates;
    }

    private List<IndexDocument> sortRefindedCandidates(List<IndexDocument> refinedCandidates, IndexDocument receiverObj) {
        // TODO 3.3.3:
        // ...
        // use IndexDocument.normalizedLongestCommonSubsequenceLengthOverallContextToOther(IndexDocument)
        // and IndexDocument.normalizedLevenshteinDistanceLineContextToOther(IndexDocument)
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
