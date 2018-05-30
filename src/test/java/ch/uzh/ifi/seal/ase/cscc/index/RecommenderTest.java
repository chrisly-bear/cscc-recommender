package ch.uzh.ifi.seal.ase.cscc.index;

import ch.uzh.ifi.seal.ase.cscc.testutils.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

public class RecommenderTest {

    private List<IndexDocument> docsToIndex = new LinkedList<>();

    public RecommenderTest() {
        TestUtils.fillWithTestDocuments(docsToIndex);
    }

    private InMemoryInvertedIndex getTestIndex() {
        InMemoryInvertedIndex index = new InMemoryInvertedIndex();
        for (IndexDocument doc : docsToIndex) {
            index.indexDocument(doc);
        }
        return index;
    }

    @Test
    public void contains() {
        IndexDocument receiverObj = docsToIndex.get(0);
        Recommender recommender = new Recommender(getTestIndex(), receiverObj);
        // the recommendations should contain the document which we previously put in the index
        assertTrue(recommender.contains(receiverObj));
    }

    @Test
    public void containsTopThree() {
        IndexDocument receiverObj = docsToIndex.get(0);
        Recommender recommender = new Recommender(getTestIndex(), receiverObj);
        // the top three recommendations should contain the document which we previously put in the index
        assertTrue(recommender.containsTopThree(receiverObj));
    }

    @Test
    public void getTopThreeRecommendations() {
        IndexDocument receiverObj = docsToIndex.get(0);
        Recommender recommender = new Recommender(getTestIndex(), receiverObj);
        // the method name for our test document should be recommended
        assertTrue(recommender.getTopThreeRecommendations().contains(receiverObj.getMethodCall()));
    }
}
