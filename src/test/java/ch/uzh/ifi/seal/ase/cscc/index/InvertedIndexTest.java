package ch.uzh.ifi.seal.ase.cscc.index;

import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InvertedIndexTest {

    private static final String INVERTED_INDEX_DIR_NAME = "CSCCInvertedIndex";
    private List<IndexDocument> docsToIndex = new LinkedList<>();
    private InvertedIndex index;
    private InMemoryInvertedIndex luceneIndexInMemory;
    private DiskBasedInvertedIndex luceneIndexDiskBased;

    private IndexDocument receiverObj1 = new IndexDocument(null, "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
            "toLowerCase", "context"
    ));

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

    @Before
    public void setUp() {

        // create test docs
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

        // create inverted index
        index = new InvertedIndex();
        luceneIndexInMemory = new InMemoryInvertedIndex();
        luceneIndexDiskBased = new DiskBasedInvertedIndex(CSCCConfiguration.PERSISTENCE_LOCATION_TEST);
        for (IndexDocument doc : docsToIndex) {
            index.indexDocument(doc);
            luceneIndexInMemory.indexDocument(doc);
            luceneIndexDiskBased.indexDocument(doc);
        }
    }

    @After
    public void cleanUp() throws IOException {
        System.out.println("Removing index files...");
        FileUtils.deleteDirectory(new File(CSCCConfiguration.PERSISTENCE_LOCATION_TEST + "/" + INVERTED_INDEX_DIR_NAME));
    }

    @Test
    public void search() {
        Set<IndexDocument> answers = index.search(receiverObj1);
//        System.out.println("Answers: ");
//        printAnswers(answers);
        makeAssertions(answers);
    }

    @Test
    public void searchLuceneInMemory() throws IOException {
        Set<IndexDocument> answers = luceneIndexInMemory.search(receiverObj1);
        makeAssertions(answers);
    }

    @Test
    public void searchLuceneDiskBased() throws IOException {
        Set<IndexDocument> answers = luceneIndexDiskBased.search(receiverObj1);
        makeAssertions(answers);
    }

    private void makeAssertions(Set<IndexDocument> answers) {
        assertEquals(3, answers.size());
        Set<String> methodNames = getMethodNames(answers);
        assertTrue(methodNames.contains("explode"));
        assertTrue(methodNames.contains("flyAway"));
        assertTrue(methodNames.contains("identify"));
    }

    @Test
    public void persist() throws IOException {
        index.persistToDisk(CSCCConfiguration.PERSISTENCE_LOCATION_TEST);
    }

    @Test
    public void initializeFromDisk() throws IOException {
        index.persistToDisk(CSCCConfiguration.PERSISTENCE_LOCATION_TEST);
        InvertedIndex indexFromDisk = new InvertedIndex();
        indexFromDisk.initializeFromDisk(CSCCConfiguration.PERSISTENCE_LOCATION_TEST);

        Set<IndexDocument> answers = index.search(receiverObj1);
        Set<IndexDocument> answersIndexFromDisk = indexFromDisk.search(receiverObj1);
        assertEquals(answers.size(), answersIndexFromDisk.size());
        Set<String> methodNames = getMethodNames(answersIndexFromDisk);
        assertTrue(methodNames.contains("explode"));
        assertTrue(methodNames.contains("flyAway"));
        assertTrue(methodNames.contains("identify"));

        FileUtils.deleteDirectory(new File(CSCCConfiguration.PERSISTENCE_LOCATION_TEST + "/" + INVERTED_INDEX_DIR_NAME));
    }
}