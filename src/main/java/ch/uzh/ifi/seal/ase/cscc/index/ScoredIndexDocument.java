package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.List;

/**
 * Same as IndexDocument, but has an additional fields 'score1' and 'score2' which can be useful for comparing and sorting IndexDocuments
 */
public class ScoredIndexDocument implements Comparable<ScoredIndexDocument> {

    private IndexDocument docWithoutScore;
    private double score1;
    private double score2;

    public ScoredIndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext, double score1, double score2) {
        this.docWithoutScore = new IndexDocument(methodCall, type, lineContext, overallContext);
        this.score1 = score1;
        this.score2 = score2;
    }

    public ScoredIndexDocument(IndexDocument doc, double score1, double score2) {
        this.docWithoutScore = doc;
        this.score1 = score1;
        this.score2 = score2;
    }

    public double getScore1() {
        return score1;
    }

    public double getScore2() {
        return score2;
    }

    /**
     * Removes the scores and returns only the underlying IndexDocument
     * @return IndexDocument without the scores
     */
    public IndexDocument getIndexDocumentWithoutScore() {
        return this.docWithoutScore;
    }

    /**
     * higher score than other means it's coming before other when sorting
     */
    @Override
    public int compareTo(ScoredIndexDocument o) {
        if (this.getScore1() > o.getScore1()) {
            return -1;
        } else if (this.getScore1() < o.getScore1()) {
            return 1;
        } else {
            // in case of equality in score1, compare score2
            if (this.getScore2() > o.getScore2()) {
                return -1;
            } else if (this.getScore2() < o.getScore2()) {
                return 1;
            } else {
                // tie in both score1 and score2, this and other are equal
                return 0;
            }
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
        return docWithoutScore.normalizedLevenshteinDistanceLineContextToOther(other.getIndexDocumentWithoutScore());
    }

    public double longestCommonSubsequenceOverallContextToOther(ScoredIndexDocument other) {
        return docWithoutScore.normalizedLongestCommonSubsequenceLengthOverallContextToOther(other.getIndexDocumentWithoutScore());
    }

}
