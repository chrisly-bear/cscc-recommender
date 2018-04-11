package ch.uzh.ifi.seal.ase.cscc.index;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ScoredIndexDocumentTest {

    @Before
    public void setUp() {
    }

    @Test
    public void sortingTest() {
        List<ScoredIndexDocument> scoredIndexDocs = new LinkedList<>();
        scoredIndexDocs.add(new ScoredIndexDocument(null, -1, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(null, -3, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(null, -2, 0));
        scoredIndexDocs.sort(null);
        String sortedString = "";
        for (int i = 0; i < scoredIndexDocs.size(); i++) {
            sortedString += scoredIndexDocs.get(i).getScore1();
            sortedString += " ";
        }
        assertEquals("-1.0 -2.0 -3.0 ", sortedString);
    }

}