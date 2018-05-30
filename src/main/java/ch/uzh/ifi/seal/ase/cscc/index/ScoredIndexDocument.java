package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.List;

/**
 * Same as IndexDocument, but has an additional fields 'score1' and 'score2' which can be useful for comparing and sorting IndexDocuments
 */
public class ScoredIndexDocument extends IndexDocument implements Comparable<ScoredIndexDocument> {

    private double score1;
    private double score2;

    public ScoredIndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext, double score1, double score2) {
        super(methodCall, type, lineContext, overallContext);
        this.score1 = score1;
        this.score2 = score2;
    }

    public ScoredIndexDocument(IndexDocument doc, double score1, double score2) {
        this(doc.getMethodCall(), doc.getType(), doc.getLineContext(), doc.getOverallContext(), score1, score2);
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
        return new IndexDocument(this.getId(), this.getMethodCall(), this.getType(), this.getLineContext(), this.getOverallContext(), this.getLineContextSimhash(), this.getOverallContextSimhash());
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

    @Override
    public String toString() {
        return "ScoredIndexDocument{" +
                "id='" + this.getId() + '\'' +
                ", methodCall='" + this.getMethodCall() + '\'' +
                ", type='" + this.getType() + '\'' +
                ", lineContext=" + this.getLineContext() +
                ", overallContext=" + this.getOverallContext() +
                ", lineContextSimhash=" + this.getLineContextSimhash() +
                ", overallContextSimhash=" + this.getOverallContextSimhash() +
                ", score1=" + score1 +
                ", score2=" + score2 +
                '}';
    }
}
