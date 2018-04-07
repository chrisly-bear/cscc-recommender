package ch.uzh.ifi.seal.ase.cscc.index;

import com.github.tomtung.jsimhash.Util;
import org.junit.jupiter.api.BeforeEach;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexDocumentTest {

    IndexDocument doc;

    @BeforeEach
    void setUp() {
        List<String> lineContext = new LinkedList<>();
        lineContext.add("Romeo");
        lineContext.add("Julia");
        List<String> overallContext = new LinkedList<>();
        overallContext.add("lorem");
        overallContext.add("ipsum");
        overallContext.add("dolor");
        overallContext.add("sit");
        overallContext.add("amet");
        overallContext.add("consectetur");
        overallContext.add("adipiscing");
        overallContext.add("elit");
        doc = new IndexDocument("testMethod", "com.something.util.test.TestClass", lineContext, overallContext);
    }

    @org.junit.jupiter.api.Test
    void getLineContextSimhash() {
        long simhash = doc.getLineContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("LineContextSimhash: " + simhashString);
        String expectedString = "0011000011001101000000110101010111111110100001010101100000100001";
        assertEquals(expectedString, simhashString);
    }

    @org.junit.jupiter.api.Test
    void getOverallContextSimhash() {
        long simhash = doc.getOverallContextSimhash();
        String simhashString = Util.simHashToString(simhash);
        System.out.println("OverallContextSimhash: " + simhashString);
        String expectedString = "0010011101001010111011110001001110100111011100000000000000110110";
        assertEquals(expectedString, simhashString);
    }

}