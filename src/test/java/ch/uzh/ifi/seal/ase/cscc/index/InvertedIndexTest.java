package ch.uzh.ifi.seal.ase.cscc.index;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class InvertedIndexTest {

    private List<IndexDocument> docsToIndex = new LinkedList<>();
    private String persistenceLocation = "/tmp/";

    @Before
    public void setUp() {
        // test docs
        docsToIndex.add(new IndexDocument("methodCall", "java.util.List", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "this", "is", "an", "overall", "context"
        )));
        docsToIndex.add(new IndexDocument("flyAway", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "toString", "new"
        )));
        docsToIndex.add(new IndexDocument("explode", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "setTimer", "getTarget", "try"
        )));
        docsToIndex.add(new IndexDocument("identify", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "context"
        )));
        docsToIndex.add(new IndexDocument("diveDeep", "org.entity.Submarine", new LinkedList<>(), Arrays.asList(
                "equals", "toString", "pressureIsInSafeRange", "lookForFish", "getWaterTemperature"
        )));
    }

    @Test
    public void search() {
        // create inverted index
        InvertedIndex index = new InvertedIndex();
        for (IndexDocument doc : docsToIndex) {
            index.indexDocument(doc);
        }

        // search in inverted index
        IndexDocument receiverObj1 = new IndexDocument(null, "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "context"
        ));
        Set<IndexDocument> answers = index.search(receiverObj1);
//        System.out.println("Answers: ");
//        printAnswers(answers);
        assertEquals(3, answers.size());
        Set<String> methodNames = getMethodNames(answers);
        assertTrue(methodNames.contains("explode"));
        assertTrue(methodNames.contains("flyAway"));
        assertTrue(methodNames.contains("identify"));
    }

    private static void printAnswers(Set<IndexDocument> answers) {
        for (IndexDocument doc : answers) {
            System.out.println("   " + doc);
        }
    }

    private static Set<String> getMethodNames(Set<IndexDocument> docs) {
        Set<String> methodNames = new HashSet<>();
        for (IndexDocument doc : docs) {
            methodNames.add(doc.getMethodCall());
        }
        return methodNames;
    }

    @Test
    public void persist() {
        // create inverted index
        InvertedIndex index = new InvertedIndex();
        for (IndexDocument doc : docsToIndex) {
            index.indexDocument(doc);
        }
        // write to disk
        index.persistToDisk(persistenceLocation);
    }
}