package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.List;

/**
 * Same as IndexDocument, but has an additional fields 'score1' and 'score2' which can be useful for comparing and sorting IndexDocuments
 */
public class ScoredIndexDocument implements Comparable<ScoredIndexDocument> {

    private IndexDocument docWithoutScores;
    private double score1;
    private double score2;

    public ScoredIndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext, double score1, double score2) {
        this.docWithoutScores = new IndexDocument(methodCall, type, lineContext, overallContext);
        this.score1 = score1;
        this.score2 = score2;
    }

    public ScoredIndexDocument(IndexDocument doc, double score1, double score2) {
        this.docWithoutScores = doc;
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
     *
     * @return IndexDocument without the scores
     */
    public IndexDocument getIndexDocumentWithoutScores() {
        return this.docWithoutScores;
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
        return docWithoutScores.getOverallContextSimhash();
    }

    public List<String> getOverallContext() {
        return docWithoutScores.getOverallContext();
    }

    public long getLineContextSimhash() {
        return docWithoutScores.getLineContextSimhash();
    }

    public List<String> getLineContext() {
        return docWithoutScores.getLineContext();
    }

    public int lineContextHammingDistanceToOther(ScoredIndexDocument other) {
        return docWithoutScores.lineContextHammingDistanceToOther(other.getIndexDocumentWithoutScores());
    }

    public int overallContextHammingDistanceToOther(ScoredIndexDocument other) {
        return docWithoutScores.overallContextHammingDistanceToOther(other.getIndexDocumentWithoutScores());
    }

    public String getMethodCall() {
        return docWithoutScores.getMethodCall();
    }

    public String getType() {
        return docWithoutScores.getType();
    }

    public double levenshteinDistanceLineContextToOther(ScoredIndexDocument other) {
        return docWithoutScores.normalizedLevenshteinDistanceLineContextToOther(other.getIndexDocumentWithoutScores());
    }

    public double longestCommonSubsequenceOverallContextToOther(ScoredIndexDocument other) {
        return docWithoutScores.normalizedLongestCommonSubsequenceLengthOverallContextToOther(other.getIndexDocumentWithoutScores());
    }

    @Override
    public String toString() {
        return "ScoredIndexDocument{" +
                "id='" + docWithoutScores.getId() + '\'' +
                ", methodCall='" + docWithoutScores.getMethodCall() + '\'' +
                ", type='" + docWithoutScores.getType() + '\'' +
                ", lineContext=" + docWithoutScores.getLineContext() +
                ", overallContext=" + docWithoutScores.getOverallContext() +
                ", lineContextSimhash=" + docWithoutScores.getLineContextSimhash() +
                ", overallContextSimhash=" + docWithoutScores.getOverallContextSimhash() +
                ", score1=" + score1 +
                ", score2=" + score2 +
                '}';
    }
}
