package ch.uzh.ifi.seal.ase.cscc.index;

import org.uncommons.maths.binary.BitString;

import java.util.List;
import java.util.UUID;

public class IndexDocument {

    private static String id;
    private String methodCall;
    private String type;
    private List<String> lineContext;
    private List<String> overallContext;
    private BitString lineContextSimhash;
    private BitString overallContextSimhash;

    public IndexDocument(String methodCall, String type, List<String> lineContext, List<String> overallContext) {
        this.id = UUID.randomUUID().toString();
        this.methodCall = methodCall;
        this.type = type;
        this.lineContext = lineContext;
        this.overallContext = overallContext;
        this.lineContextSimhash = createSimhashFromStrings(lineContext);
        this.overallContextSimhash = createSimhashFromStrings(overallContext);
    }

    /*
      Getters
     */
    public static String getId() {
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

    public BitString getLineContextSimhash() {
        return lineContextSimhash;
    }

    public BitString getOverallContextSimhash() {
        return overallContextSimhash;
    }

    private BitString createSimhashFromStrings(List<String> strings) {
        String concatenatedString = concatenate(strings);
        // TODO: use Jenkin hash function to create 64 bit simhash
        // [26] M. S. Uddin, C. K. Roy, K. A. Schneider, and A. Hindle, “On the Effectiveness of Simhash for Detecting Near-Miss Clones in Large Scale Software Systems”, in Proc. WCRE, 2011, pp. 13-22.
        // C implementation: https://github.com/vilda/shash
        return null;
    }

    private String concatenate(List<String> strings) {
        String concatenatedString = "";
        for (String s : strings) {
            concatenatedString += s;
        }
        return concatenatedString;
    }

}
