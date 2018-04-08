package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.List;
import java.util.UUID;
import com.github.tomtung.jsimhash.*;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.apache.commons.text.similarity.LongestCommonSubsequenceDistance;

public class IndexDocument {

    private SimHashBuilder simHashBuilder;
    // would it make sense to use one of the simhashes as id?
    private String id;
    private String methodCall;
    private String type;
    private List<String> lineContext;
    private List<String> overallContext;
    private long lineContextSimhash;
    private long overallContextSimhash;

    public IndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext) {
        this.id = UUID.randomUUID().toString();
        this.methodCall = methodCall;
        this.type = type;
        this.lineContext = lineContext;
        this.overallContext = overallContext;
        this.simHashBuilder = new SimHashBuilder();
        this.lineContextSimhash = createSimhashFromStrings(lineContext);
        this.overallContextSimhash = createSimhashFromStrings(overallContext);
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
        return lineContext;
    }

    public List<String> getOverallContext() {
        return overallContext;
    }

    public long getLineContextSimhash() {
        return lineContextSimhash;
    }

    public long getOverallContextSimhash() {
        return overallContextSimhash;
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

    public int lineContextHammingDistanceToOther(IndexDocument other) {
        return Util.hammingDistance(this.getLineContextSimhash(), other.getLineContextSimhash());
    }

    public int overallContextHammingDistanceToOther(IndexDocument other) {
        return Util.hammingDistance(this.getOverallContextSimhash(), other.getOverallContextSimhash());
    }

    public int longestCommonSubsequenceLengthOverallContextToOther(IndexDocument other) {
        String left = concatenate(getOverallContext());
        String right = concatenate(other.getOverallContext());
        return new LongestCommonSubsequence().apply(left, right);
    }

    public int levenshteinDistanceLineContextToOther(IndexDocument other) {
        String left = concatenate(getLineContext());
        String right = concatenate(other.getLineContext());
        return LevenshteinDistance.getDefaultInstance().apply(left, right);
    }

}
