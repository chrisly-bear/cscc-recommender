package ch.uzh.ifi.seal.ase.cscc.index;

import ch.uzh.ifi.seal.ase.cscc.testutils.TestUtils;
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
    private InMemoryInvertedIndex luceneIndexInMemory;
    private DiskBasedInvertedIndex luceneIndexDiskBased;
    private DiskBasedInvertedIndex luceneIndexDiskBasedNoSQL;

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
        // create test documents
        TestUtils.fillWithTestDocuments(docsToIndex);

        // create inverted index
        luceneIndexInMemory = new InMemoryInvertedIndex();
        luceneIndexDiskBased = new DiskBasedInvertedIndex(CSCCConfiguration.PERSISTENCE_LOCATION_TEST);
        luceneIndexDiskBasedNoSQL = new DiskBasedInvertedIndex(CSCCConfiguration.PERSISTENCE_LOCATION_TEST, false);
        for (IndexDocument doc : docsToIndex) {
            luceneIndexInMemory.indexDocument(doc);
            luceneIndexDiskBased.indexDocument(doc);
            luceneIndexDiskBasedNoSQL.indexDocument(doc);
        }
    }

    @After
    public void cleanUp() throws IOException {
        System.out.println("Removing index files...");
        FileUtils.deleteDirectory(new File(CSCCConfiguration.PERSISTENCE_LOCATION_TEST + "/" + INVERTED_INDEX_DIR_NAME));
    }

    @Test
    public void search_InMemoryInvertedIndex() {
        Set<IndexDocument> answers = luceneIndexInMemory.search(receiverObj1);
        makeAssertions(answers);
    }

    @Test
    public void search_DiskBasedInvertedIndex () {
        Set<IndexDocument> answers = luceneIndexDiskBased.search(receiverObj1);
        makeAssertions(answers);
        luceneIndexDiskBased.cleanUp();
    }

    @Test
    public void search_DiskBasedInvertedIndexNoSQL () {
        Set<IndexDocument> answers = luceneIndexDiskBasedNoSQL.search(receiverObj1);
        makeAssertions(answers);
    }

    private void makeAssertions(Set<IndexDocument> answers) {
        assertEquals(3, answers.size());
        Set<String> methodNames = getMethodNames(answers);
        assertTrue(methodNames.contains("explode"));
        assertTrue(methodNames.contains("flyAway"));
        assertTrue(methodNames.contains("identify"));
    }

}