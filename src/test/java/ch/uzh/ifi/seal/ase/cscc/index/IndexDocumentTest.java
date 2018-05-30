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
    IndexDocument doc2_twin;

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

        // same as doc2 but with different order of words in contexts and duplicate words in context
        List<String> lineContext3 = new LinkedList<>();
        lineContext3.add("Eve");
        lineContext3.add("Eve");
        lineContext3.add("Adam");
        lineContext3.add("Adam");
        List<String> overallContext3 = new LinkedList<>();
        overallContext3.add("curabitur");
        overallContext3.add("dapibus");
        overallContext3.add("porta");
        overallContext3.add("orci");
        overallContext3.add("id");
        overallContext3.add("ipsum");
        overallContext3.add("in");
        overallContext3.add("Pellentesque");
        overallContext3.add("Pellentesque");
        overallContext3.add("in");
        overallContext3.add("ipsum");
        overallContext3.add("id");
        overallContext3.add("orci");
        overallContext3.add("porta");
        overallContext3.add("dapibus");
        overallContext3.add("curabitur");
        doc2_twin = new IndexDocument("testMethod2", "com.something.util.test.TestClass2", lineContext3, overallContext3);
    }

    @Test
    public void getLineContextSimhash() {
        long simhash = doc1.getLineContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("LineContextSimhash: " + simhashString);
        String expectedString = "0110110110001100100011000000011010101110001001110111111010000100";
        assertEquals(expectedString, simhashString);
    }

    @Test
    public void getOverallContextSimhash() {
        long simhash = doc1.getOverallContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("OverallContextSimhash: " + simhashString);
        String expectedString = "0011001111101111010011001011101011001101100101111101111011000011";
        assertEquals(expectedString, simhashString);
    }

    @Test
    public void lineContextHammingDistanceToOther() {
        // hamming distance to self is 0
        assertEquals(0, doc1.lineContextHammingDistanceToOther(doc1));
        // hamming distance must be symmetric
        assertEquals(doc2.lineContextHammingDistanceToOther(doc1), doc1.lineContextHammingDistanceToOther(doc2));
        int expected = 27;
        assertEquals(expected, doc1.lineContextHammingDistanceToOther(doc2));
        assertEquals(expected, doc2.lineContextHammingDistanceToOther(doc1));
    }

    @Test
    public void overallContextHammingDistanceToOther() {
        // hamming distance to self is 0
        assertEquals(0, doc1.overallContextHammingDistanceToOther(doc1));
        // hamming distance must be symmetric
        assertEquals(doc2.overallContextHammingDistanceToOther(doc1), doc1.overallContextHammingDistanceToOther(doc2));
        int expected = 20;
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

        double expected = 0.3829787234042553;
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

    @Test
    public void identicalDocumentsMustHaveSameID() {
        assertEquals(doc2.getId(), doc2_twin.getId());
    }

    @Test
    public void identicalDocumentsMustBeEqual() {
        assertEquals(doc2, doc2_twin);
    }

    @Test
    public void toStringTest() {
        String expected = "IndexDocument{id='69f823f36cc590f1ebb2006fbe3d5963802e54242af4fd14a7ca5921b7b5d860', methodCall='testMethod1', type='com.something.util.test.TestClass1', lineContext=[Juliet, Romeo], overallContext=[Lorem, adipiscing, amet, consectetur, dolor, elit, ipsum, sit], lineContextSimhash=7893838207193153156, overallContextSimhash=3742294180565081795}";
        assertEquals(expected, doc1.toString());
    }

}