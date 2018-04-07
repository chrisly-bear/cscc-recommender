package ch.uzh.ifi.seal.ase.cscc.index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoredIndexDocumentTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void sortingTest() {
        List<ScoredIndexDocument> scoredIndexDocs = new LinkedList<>();
        scoredIndexDocs.add(new ScoredIndexDocument(null, -1));
        scoredIndexDocs.add(new ScoredIndexDocument(null, -3));
        scoredIndexDocs.add(new ScoredIndexDocument(null, -2));
        scoredIndexDocs.sort(null);
        String sortedString = "";
        for (int i = 0; i < scoredIndexDocs.size(); i++) {
            sortedString += scoredIndexDocs.get(i).getScore();
            sortedString += " ";
        }
        assertEquals("-1.0 -2.0 -3.0 ", sortedString);
    }

}