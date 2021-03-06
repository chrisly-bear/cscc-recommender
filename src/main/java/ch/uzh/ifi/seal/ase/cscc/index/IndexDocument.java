package ch.uzh.ifi.seal.ase.cscc.index;

import com.github.tomtung.jsimhash.SimHashBuilder;
import com.github.tomtung.jsimhash.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.LongestCommonSubsequence;

import java.io.Serializable;
import java.util.*;

/**
 * Class representing the context around a given method call and the type of the receiver object
 * in a document as described by the paper {@see <a href="http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.703.9376&rep=rep1&type=pdf">}
 * <p>
 * Important: Due to how the id is created, different IndexDocument objects will have the same {@link IndexDocument#id}
 * if they have the same {@link IndexDocument#type}, {@link IndexDocument#methodCall}, and {@link IndexDocument#overallContext}
 */
public class IndexDocument implements Serializable {

    private transient SimHashBuilder simHashBuilder;
    private String id;
    private String methodCall;
    private String type;
    private Set<String> lineContext;
    private Set<String> overallContext;
    private long lineContextSimhash;
    private long overallContextSimhash;

    /**
     * Creates a new IndexDocument storing the given information and assigns it an id based on
     * {@link IndexDocument#type}, {@link IndexDocument#methodCall}, and {@link IndexDocument#overallContext}
     */
    public IndexDocument(String methodCall, String type, Collection<String> lineContext, Collection<String> overallContext) {
        if (type == null || type.equals("")) {
            throw new IllegalArgumentException("Parameter 'type' of IndexDocument must not be null or empty!");
        }
        this.methodCall = methodCall;
        this.type = type;
        // We use a TreeSet so that documents with duplicate words in the context and different order of words in the
        // context have the same structure and thus create the same ID. Removing duplicate words is mentioned explicitly
        // in the paper. The order does not contain any relevant information for our algorithm either, because when
        // creating the base candidate list we only search for documents which contain the same words in the context,
        // no matter their order in the context (bag of words retrieval).
        this.lineContext = new TreeSet<>(lineContext);
        this.overallContext = new TreeSet<>(overallContext);
        this.simHashBuilder = new SimHashBuilder();
        this.lineContextSimhash = createSimhashFromStrings(setToList(this.lineContext));
        this.overallContextSimhash = createSimhashFromStrings(setToList(this.overallContext));
        // We create a unique, deterministic identifier by combining type, method call, and overall context.
        // The id should be deterministic so that when we run the indexing several times, we don't add duplicates
        // to our index. We use SHA256 hashing to limit the length of the id to 64 characters. This is important
        // because we use the id as a file name when serializing the IndexDocument to disk and want to avoid file
        // names that are too long for the operating system to handle. SHA256 hashing should not cause any colli-
        // sions (at least not before the universe comes to an end).
        String uniqueDeterministicId = type + "_" + (methodCall == null ? "-" : methodCall) + "_" + concatenate(setToList(this.overallContext));
        this.id = DigestUtils.sha256Hex(uniqueDeterministicId);
    }

    /**
     * Creates a new IndexDocument with the given information.
     * <p>
     * Use this constructor only if you are loading IndexDocument instances from a stored model
     * and you already know their docID and simhashes.
     */
    public IndexDocument(String docId, String methodCall, String type, Collection<String> lineContext, Collection<String> overallContext, long lineContextSimhash, long overallContextSimhash) {
        id = docId;
        this.methodCall = methodCall;
        this.type = type;
        this.lineContext = new TreeSet<>(lineContext);
        this.overallContext = new TreeSet<>(overallContext);
        this.lineContextSimhash = lineContextSimhash;
        this.overallContextSimhash = overallContextSimhash;
    }

    /*
      Getters
     */

    public String getId() {
        return id;
    }

    public String getMethodCall() {
        return methodCall;
    }

    public String getType() {
        return type;
    }

    public List<String> getLineContext() {
        return setToList(lineContext);
    }

    public List<String> getOverallContext() {
        return setToList(overallContext);
    }

    public long getLineContextSimhash() {
        return lineContextSimhash;
    }

    public long getOverallContextSimhash() {
        return overallContextSimhash;
    }

    public String getLineContextConcatenated() {
        return concatenate(setToList(lineContext));
    }

    public String getOverallContextConcatenated() {
        return concatenate(setToList(overallContext));
    }

    private <T> List<T> setToList(Set<T> set) {
        List<T> result = new LinkedList<>();
        result.addAll(set);
        return result;
    }

    private long createSimhashFromStrings(List<String> strings) {
        // Paper mentions Jenkin hash function to create 64 bit simhash:
        //    [26] M. S. Uddin, C. K. Roy, K. A. Schneider, and A. Hindle, “On the Effectiveness of Simhash for Detecting Near-Miss Clones in Large Scale Software Systems”, in Proc. WCRE, 2011, pp. 13-22.
        //    C implementation: https://github.com/vilda/shash
        // We are using https://github.com/tomtung/jsimhash here
        String concatenatedString = concatenate(strings);
        simHashBuilder.reset();
//        concatenatedString = concatenatedString.replaceAll("[^\\w,]+", " ").toLowerCase();
        simHashBuilder.addStringFeature(concatenatedString);
        return simHashBuilder.computeResult();
    }

    private String concatenate(List<String> strings) {
        StringBuilder concatenatedString = new StringBuilder();
        for (String s : strings) {
            concatenatedString.append(s);
        }
        return concatenatedString.toString();
    }

    /**
     * Calculates the hamming distance between the line contexts of this IndexDocument another one.
     *
     * @param other The other document
     * @return The hamming distance between this document's and the other's line context
     */
    public int lineContextHammingDistanceToOther(IndexDocument other) {
        return Util.hammingDistance(this.getLineContextSimhash(), other.getLineContextSimhash());
    }

    /**
     * Calculates the hamming distance between the overall contexts of this IndexDocument another one.
     *
     * @param other The other document
     * @return The hamming distance between this document's and the other's overall context
     */
    public int overallContextHammingDistanceToOther(IndexDocument other) {
        return Util.hammingDistance(this.getOverallContextSimhash(), other.getOverallContextSimhash());
    }

    /**
     * Compares the overall context of `this` to `other` using LCS (Longest Common Subsequence)
     *
     * @param other document to compare this one to
     * @return value between [0,1] where
     * 0 means documents' overall contexts are completely different,
     * 1 means documents' overall contexts are identical
     */
    public double normalizedLongestCommonSubsequenceLengthOverallContextToOther(IndexDocument other) {
        String left = concatenate(getOverallContext());
        String right = concatenate(other.getOverallContext());
        int maxLength = Math.max(left.length(), right.length());
        double lcs = new LongestCommonSubsequence().apply(left, right);
        double lcsNorm = lcs / maxLength;
        return lcsNorm;
    }

    /**
     * Compares the line context of `this` to `other` using Levenshtein Distance
     *
     * @param other document to compare this one to
     * @return value between [0,1] where
     * 0 means documents' line contexts are completely different,
     * 1 means documents' line contexts are identical
     */
    public double normalizedLevenshteinDistanceLineContextToOther(IndexDocument other) {
        String left = concatenate(getLineContext());
        String right = concatenate(other.getLineContext());
        int maxLength = Math.max(left.length(), right.length());
        double lev = LevenshteinDistance.getDefaultInstance().apply(left, right);
        double levNorm = 1 - (lev / maxLength);
        return levNorm;
    }

    @Override
    public String toString() {
        return "IndexDocument{" +
                "id='" + id + '\'' +
                ", methodCall='" + methodCall + '\'' +
                ", type='" + type + '\'' +
                ", lineContext=" + lineContext +
                ", overallContext=" + overallContext +
                ", lineContextSimhash=" + lineContextSimhash +
                ", overallContextSimhash=" + overallContextSimhash +
                '}';
    }

    // equals method required for detecting already indexed documents (to avoid duplicate elements in index)
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexDocument) {
            IndexDocument other = (IndexDocument) obj;
            return other.getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }
}
