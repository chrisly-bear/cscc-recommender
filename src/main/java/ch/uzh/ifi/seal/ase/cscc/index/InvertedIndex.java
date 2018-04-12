package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.*;

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
        List<IndexDocument> topThreeCandidates = sortRefindedCandidatesAndGetTopThree(refinedCandidates, receiverObj);
        List<String> recommendationsMethodNames = getMethodNames(topThreeCandidates);
        return recommendationsMethodNames;
    }

    private List<IndexDocument> getBaseCandidates(IndexDocument receiverObj) {
        // TODO 3.3.1:
        // ...
        return null;
    }

    private List<IndexDocument> getRefindedCandidates(List<IndexDocument> baseCandidates, IndexDocument receiverObj) {
        // TODO: test if this method does everything correctly
        int switchToLineContextThreshold = 30; // TODO: this threshold was picked at random and was never tested (maybe a good value is mentioned in the paper?)
        int k = 200;

        List<ScoredIndexDocument> scoredBaseCandidates = new LinkedList<>();
        for (IndexDocument baseCandidate : baseCandidates) {
            int lineContextDistance = baseCandidate.lineContextHammingDistanceToOther(receiverObj);
            int overallContextDistance = baseCandidate.overallContextHammingDistanceToOther(receiverObj);
            int viableDistance = overallContextDistance > switchToLineContextThreshold ? lineContextDistance : overallContextDistance;
            // we take the negative distance so that after sorting, candidates with lower distance (i.e. higher score) will come first
            ScoredIndexDocument scoredDoc = new ScoredIndexDocument(baseCandidate, -viableDistance, 0);
            scoredBaseCandidates.add(scoredDoc);
        }
        scoredBaseCandidates.sort(null); // compare using the Comparable interface implemented in ScoredIndexDocument
        // get the k top candidates
        List<ScoredIndexDocument> refinedScoredCandidates = scoredBaseCandidates.subList(0, Math.min(k,scoredBaseCandidates.size()));
        List<IndexDocument> refinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : refinedScoredCandidates) {
            refinedCandidates.add(sid.getIndexDocumentWithoutScores());
        }
        return refinedCandidates;
    }

    private List<IndexDocument> sortRefindedCandidatesAndGetTopThree(List<IndexDocument> refinedCandidates, IndexDocument receiverObj) {
        // TODO: test if this method does everything correctly
        double filteringThreshold = 0.30;
        int candidatesToSuggest = 3;
        List<ScoredIndexDocument> sortedRefinedScoredCandidates = new LinkedList<>();
        for (IndexDocument refinedCandidate : refinedCandidates) {
            double normLCS = refinedCandidate.normalizedLongestCommonSubsequenceLengthOverallContextToOther(receiverObj);
            if (normLCS > filteringThreshold) {
                double normLev = refinedCandidate.normalizedLevenshteinDistanceLineContextToOther(receiverObj);
                ScoredIndexDocument scoredDoc = new ScoredIndexDocument(refinedCandidate, normLCS, normLev);
                sortedRefinedScoredCandidates.add(scoredDoc);
            }
        }
        sortedRefinedScoredCandidates.sort(null); // compare using the Comparable interface implemented in ScoredIndexDocument
        // remove duplicates
        removeDuplicates(sortedRefinedScoredCandidates);
        // get the top three
        sortedRefinedScoredCandidates = sortedRefinedScoredCandidates.subList(0, Math.min(candidatesToSuggest-1, sortedRefinedScoredCandidates.size()));
        List<IndexDocument> sortedRefinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : sortedRefinedScoredCandidates) {
            sortedRefinedCandidates.add(sid.getIndexDocumentWithoutScores());
        }
        return sortedRefinedCandidates;
    }

    /**
     * Removes documents with same method call name while leaving the order of the list unchanged.
     */
    private void removeDuplicates(List<ScoredIndexDocument> sortedRefinedCandidates) {
        Set<String> uniqueMethodNames = new HashSet<>();
        for (int i = 0; i < sortedRefinedCandidates.size(); i++) {
            String methodName = sortedRefinedCandidates.get(i).getMethodCall();
            if (uniqueMethodNames.contains(methodName)) {
                sortedRefinedCandidates.remove(i);
                i--;
            } else {
                uniqueMethodNames.add(methodName);
            }
        }
    }

    private List<String> getMethodNames(List<IndexDocument> recommendations) {
        List<String> methodNames = new LinkedList<>();
        for (IndexDocument doc : recommendations) {
            methodNames.add(doc.getMethodCall());
        }
        return methodNames;
    }

}
