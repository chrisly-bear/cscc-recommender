package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.List;

/**
 * Same as IndexDocument, but has an additional field 'score' which can be useful for comparing and sorting IndexDocuments
 */
public class ScoredIndexDocument implements Comparable<ScoredIndexDocument> {

    private IndexDocument docWithoutScore;
    private double score;

    public ScoredIndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext, double score) {
        this.docWithoutScore = new IndexDocument(methodCall, type, lineContext, overallContext);
        this.score = score;
    }

    public ScoredIndexDocument(IndexDocument doc, double score) {
        this.docWithoutScore = doc;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    /**
     * Removes the score and returns only the underlying IndexDocument
     * @return IndexDocument without the score
     */
    public IndexDocument getIndexDocumentWithoutScore() {
        return this.docWithoutScore;
    }

    /**
     * higher score than other means it's coming before other when sorting
     */
    @Override
    public int compareTo(ScoredIndexDocument o) {
        if (this.getScore() > o.getScore()) {
            return -1;
        } else if (this.getScore() < o.getScore()) {
            return 1;
        } else {
            return 0;
        }
    }

    /*
      delegated IndexDocument methods
     */

    public long getOverallContextSimhash() {
        return docWithoutScore.getOverallContextSimhash();
    }

    public List<String> getOverallContext() {
        return docWithoutScore.getOverallContext();
    }

    public long getLineContextSimhash() {
        return docWithoutScore.getLineContextSimhash();
    }

    public List<String> getLineContext() {
        return docWithoutScore.getLineContext();
    }

    public int lineContextHammingDistanceToOther(ScoredIndexDocument other) {
        return docWithoutScore.lineContextHammingDistanceToOther(other.getIndexDocumentWithoutScore());
    }

    public int overallContextHammingDistanceToOther(ScoredIndexDocument other) {
        return docWithoutScore.overallContextHammingDistanceToOther(other.getIndexDocumentWithoutScore());
    }

    public String getMethodCall() {
        return docWithoutScore.getMethodCall();
    }

    public String getType() {
        return docWithoutScore.getType();
    }

    public double levenshteinDistanceLineContextToOther(ScoredIndexDocument other) {
        return docWithoutScore.levenshteinDistanceLineContextToOther(other.getIndexDocumentWithoutScore());
    }

    public double longestCommonSubsequenceOverallContextToOther(ScoredIndexDocument other) {
        return docWithoutScore.longestCommonSubsequenceOverallContextToOther(other.getIndexDocumentWithoutScore());
    }

}
