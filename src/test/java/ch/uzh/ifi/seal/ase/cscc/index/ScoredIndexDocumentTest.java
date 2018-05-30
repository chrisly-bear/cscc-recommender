package ch.uzh.ifi.seal.ase.cscc.index;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ScoredIndexDocumentTest {

    private IndexDocument emptyDoc = new IndexDocument("", "empty", new LinkedList<>(), new LinkedList<>());

    @Before
    public void setUp() {
    }

    @Test
    public void sortingTestScore1() {
        List<ScoredIndexDocument> scoredIndexDocs = new LinkedList<>();
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, -1, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, -3, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, -2, 0));
        scoredIndexDocs.sort(null);
        String sortedString = "";
        for (int i = 0; i < scoredIndexDocs.size(); i++) {
            sortedString += scoredIndexDocs.get(i).getScore1();
            sortedString += " ";
        }
        assertEquals("-1.0 -2.0 -3.0 ", sortedString);
    }

    @Test
    public void sortingTestScore1AndScore2() {
        List<ScoredIndexDocument> scoredIndexDocs = new LinkedList<>();
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, 2, 0.2));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, 1, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, 2, 0.0));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, 3, 0));
        scoredIndexDocs.add(new ScoredIndexDocument(emptyDoc, 2, 0.1));
        scoredIndexDocs.sort(null);
        String sortedString = "";
        for (int i = 0; i < scoredIndexDocs.size(); i++) {
            sortedString += scoredIndexDocs.get(i).getScore1();
            sortedString += " ";
            sortedString += scoredIndexDocs.get(i).getScore2();
            sortedString += " ";
        }
        assertEquals("3.0 0.0 2.0 0.2 2.0 0.1 2.0 0.0 1.0 0.0 ", sortedString);
    }

    @Test
    public void toStringTest() {
        ScoredIndexDocument scoredDoc = new ScoredIndexDocument(emptyDoc, 1, 2);
        String expected = "ScoredIndexDocument{id='fc51fdcdc912a4771c94fadb49f8d37e620f25d44022af3e4422a5d97221a479', methodCall='', type='empty', lineContext=[], overallContext=[], lineContextSimhash=338333539836370388, overallContextSimhash=338333539836370388, score1=1.0, score2=2.0}";
        assertEquals(expected, scoredDoc.toString());
    }
}