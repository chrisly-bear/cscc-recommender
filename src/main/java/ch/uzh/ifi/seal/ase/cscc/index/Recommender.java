package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Recommender {

    private List<IndexDocument> baseCandidates;
    private List<IndexDocument> refinedCandidates;
    private List<ScoredIndexDocument> scoredCandidates;
    private List<IndexDocument> topThreeCandidates;
    private IndexDocument receiverObj;

    /**
     * @param index       inverted index structure (model) with which to suggest code completions
     * @param receiverObj object, on which the code completion is called
     */
    public Recommender(InvertedIndex index, IndexDocument receiverObj) {
        this.receiverObj = receiverObj;
        baseCandidates = getBaseCandidates(index, receiverObj);
        refinedCandidates = getRefindedCandidates(baseCandidates, receiverObj);
        scoredCandidates = sortRefindedCandidates(refinedCandidates, receiverObj);
        topThreeCandidates = getTopThreeCandidates(scoredCandidates);
    }

    private static List<IndexDocument> getBaseCandidates(InvertedIndex index, IndexDocument receiverObj) {
        List<IndexDocument> baseCandidates = new LinkedList<>();
        baseCandidates.addAll(index.search(receiverObj));
        return baseCandidates;
    }

    private static List<IndexDocument> getRefindedCandidates(List<IndexDocument> baseCandidates, IndexDocument receiverObj) {
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
        List<ScoredIndexDocument> refinedScoredCandidates = scoredBaseCandidates.subList(0, Math.min(k, scoredBaseCandidates.size()));
        List<IndexDocument> refinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : refinedScoredCandidates) {
            refinedCandidates.add(sid.getIndexDocumentWithoutScores());
        }
        return refinedCandidates;
    }

    private static List<ScoredIndexDocument> sortRefindedCandidates(List<IndexDocument> refinedCandidates, IndexDocument receiverObj) {
        // TODO: test if this method does everything correctly
        double filteringThreshold = 0.30;

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

        return sortedRefinedScoredCandidates;
    }

    private static List<IndexDocument> getTopThreeCandidates(List<ScoredIndexDocument> sortedRefinedScoredCandidates) {
        final int CANDIDATES_TO_SUGGEST = 3;
        // get the top three
        sortedRefinedScoredCandidates = sortedRefinedScoredCandidates.subList(0, Math.min(CANDIDATES_TO_SUGGEST - 1,
                sortedRefinedScoredCandidates.size()));
        List<IndexDocument> sortedRefinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : sortedRefinedScoredCandidates) {
            sortedRefinedCandidates.add(sid.getIndexDocumentWithoutScores());
        }
        return sortedRefinedCandidates;
    }

    /**
     * Removes documents with same method call name while leaving the order of the list unchanged.
     */
    private static void removeDuplicates(List<ScoredIndexDocument> sortedRefinedCandidates) {
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

    private static List<String> getMethodNames(List<IndexDocument> recommendations) {
        List<String> methodNames = new LinkedList<>();
        for (IndexDocument doc : recommendations) {
            methodNames.add(doc.getMethodCall());
        }
        return methodNames;
    }

    /**
     * Get 3 code completion suggestions for the receiver object.
     *
     * @return names of the methods that are suggested for code completion
     */
    public List<String> getTopThreeRecommendations() {
        List<String> recommendationsMethodNames = getMethodNames(topThreeCandidates);
        return recommendationsMethodNames;
    }

    public boolean containsTopThree(IndexDocument document) {
        for (IndexDocument candidateDocument : topThreeCandidates) {
            if (candidateDocument.getMethodCall().equals(document.getMethodCall())) return true;
        }

        return false;
    }

    public boolean contains(IndexDocument document) {
        for (ScoredIndexDocument candidate : scoredCandidates) {
            if (candidate.getMethodCall().equals(document.getMethodCall())) return true;
        }

        return false;
    }
}
