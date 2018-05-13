package ch.uzh.ifi.seal.ase.cscc.index;

import com.github.tomtung.jsimhash.Util;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IndexDocumentTest {

    IndexDocument doc1;
    IndexDocument doc2;

    @Before
    public void setUp() {
        List<String> lineContext1 = new LinkedList<>();
        lineContext1.add("Romeo");
        lineContext1.add("Juliet");
        List<String> overallContext1 = new LinkedList<>();
        overallContext1.add("Lorem");
        overallContext1.add("ipsum");
        overallContext1.add("dolor");
        overallContext1.add("sit");
        overallContext1.add("amet");
        overallContext1.add("consectetur");
        overallContext1.add("adipiscing");
        overallContext1.add("elit");
        doc1 = new IndexDocument("testMethod1", "com.something.util.test.TestClass1", lineContext1, overallContext1);

        List<String> lineContext2 = new LinkedList<>();
        lineContext2.add("Adam");
        lineContext2.add("Eve");
        List<String> overallContext2 = new LinkedList<>();
        overallContext2.add("Pellentesque");
        overallContext2.add("in");
        overallContext2.add("ipsum");
        overallContext2.add("id");
        overallContext2.add("orci");
        overallContext2.add("porta");
        overallContext2.add("dapibus");
        overallContext2.add("curabitur");
        doc2 = new IndexDocument("testMethod2", "com.something.util.test.TestClass2", lineContext2, overallContext2);
    }

    @Test
    public void getLineContextSimhash() {
        long simhash = doc1.getLineContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("LineContextSimhash: " + simhashString);
        String expectedString = "1111001101000101111011111000101000000010100010010000100111011001";
        assertEquals(expectedString, simhashString);
    }

    @Test
    public void getOverallContextSimhash() {
        long simhash = doc1.getOverallContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("OverallContextSimhash: " + simhashString);
        String expectedString = "0101100010011100001111110011000000000111011000101101110101110101";
        assertEquals(expectedString, simhashString);
    }

    @Test
    public void lineContextHammingDistanceToOther() {
        // hamming distance to self is 0
        assertEquals(0, doc1.lineContextHammingDistanceToOther(doc1));
        // hamming distance must be symmetric
        assertEquals(doc2.lineContextHammingDistanceToOther(doc1), doc1.lineContextHammingDistanceToOther(doc2));
        int expected = 29;
        assertEquals(expected, doc1.lineContextHammingDistanceToOther(doc2));
        assertEquals(expected, doc2.lineContextHammingDistanceToOther(doc1));
    }

    @Test
    public void overallContextHammingDistanceToOther() {
        // hamming distance to self is 0
        assertEquals(0, doc1.overallContextHammingDistanceToOther(doc1));
        // hamming distance must be symmetric
        assertEquals(doc2.overallContextHammingDistanceToOther(doc1), doc1.overallContextHammingDistanceToOther(doc2));
        int expected = 32;
        assertEquals(expected, doc1.overallContextHammingDistanceToOther(doc2));
        assertEquals(expected, doc2.overallContextHammingDistanceToOther(doc1));
    }

    private String concatenate(List<String> strings) {
        StringBuilder concatenatedString = new StringBuilder();
        for (String s : strings) {
            concatenatedString.append(s);
        }
        return concatenatedString.toString();
    }

    @Test
    public void longestCommonSubsequenceLengthOverallContextToOther() throws Exception {
        double delta = 0.0;

        // normalized LCS to self is 1
        double longestCommonSubsequenceLength = doc1.normalizedLongestCommonSubsequenceLengthOverallContextToOther(doc1);
        assertEquals(1, longestCommonSubsequenceLength, delta);

        // LCS must be symmetric
        assertEquals(doc1.normalizedLongestCommonSubsequenceLengthOverallContextToOther(doc2), doc2.normalizedLongestCommonSubsequenceLengthOverallContextToOther(doc1), delta);

        double expected = 0.425531914893617;
        assertEquals(expected, doc1.normalizedLongestCommonSubsequenceLengthOverallContextToOther(doc2), delta);
        assertEquals(expected, doc2.normalizedLongestCommonSubsequenceLengthOverallContextToOther(doc1), delta);
    }

    @Test
    public void levenshteinDistanceLineContextToOther() throws Exception {
        double delta = 0.0;

        // normalized Levenshtein to self is 1
        double levenshteinDistanceLineContextToOther = doc1.normalizedLevenshteinDistanceLineContextToOther(doc1);
        assertEquals(1, levenshteinDistanceLineContextToOther, delta);

        // Levenshtein must be symmetric
        assertEquals(doc1.normalizedLevenshteinDistanceLineContextToOther(doc2), doc2.normalizedLevenshteinDistanceLineContextToOther(doc1), delta);

        double expected = 0.09090909090909094;
        assertEquals(expected, doc1.normalizedLevenshteinDistanceLineContextToOther(doc2), delta);
        assertEquals(expected, doc2.normalizedLevenshteinDistanceLineContextToOther(doc1), delta);
    }
}