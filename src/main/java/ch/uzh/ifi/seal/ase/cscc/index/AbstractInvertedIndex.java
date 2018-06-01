package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class implementing {@link IInvertedIndex}
 * Indexes documents using Apache Lucene's {@link IndexWriter}
 */
public abstract class AbstractInvertedIndex implements IInvertedIndex {

    // fields for indexing in Lucene index
    private static final String DOC_ID_FIELD = "docID";
    private static final String OVERALL_CONTEXT_FIELD = "overallContext";
    private static final String TYPE_FIELD = "type";
    private StringField docIdField = new StringField(DOC_ID_FIELD, "", Field.Store.YES);
    private StringField typeField = new StringField(TYPE_FIELD, "", Field.Store.NO);

    private Directory indexDirectory;
    private IndexWriter indexWriter;
    private IndexSearcher searcher;

    private void initializeDirectory() {
        try {
            indexDirectory = getIndexDirectory();
        } catch (LockObtainFailedException e) {
            e.printStackTrace();
            System.exit(1); // can't write to indexDirectory, abort
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puts an IndexDocument in the index.
     *
     * @param doc document to store in index
     */
    public void indexDocument(IndexDocument doc) {
        if (isIndexed(doc)) {
            // do not put identical documents in index twice
//            System.out.println("doc " + doc.getId() + " is already indexed");
            return;
        }
        try {
            serializeIndexDocument(doc);
            addDocToLuceneIndex(doc);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // exit on IOException
        }
    }

    /**
     * Checks if a document is already in the index.
     *
     * @param doc The document to check wether it is indexed or not
     */
    abstract boolean isIndexed(IndexDocument doc);

    /**
     * Serialize a given document
     *
     * @param doc The document to serialize
     * @throws IOException
     */
    abstract void serializeIndexDocument(IndexDocument doc) throws IOException;

    /**
     * Get the directory of this document. Either the RAM directory (if index is in-memory index) or the FSDirectory (if index is disk index).
     *
     * @return The directory of this document
     * @throws IOException
     */
    abstract Directory getIndexDirectory() throws IOException;

    /**
     * Stores docID and the overall context in the Lucene index. The overall context will be what we search for at
     * retrieval time, the docID will be the result of the retrieval.
     *
     * @param doc The document that should be added to the index
     * @throws IOException
     */
    void addDocToLuceneIndex(IndexDocument doc) throws IOException {
        Document luceneDoc = new Document();
        docIdField.setStringValue(doc.getId());
        luceneDoc.add(docIdField);
        typeField.setStringValue(doc.getType());
        luceneDoc.add(typeField);
        // store all terms in the overall context as tokens in the index
        // StringField: no tokenization
        // TextField: tokenization
        for (String term : doc.getOverallContext()) {
            StringField overallContextField = new StringField(OVERALL_CONTEXT_FIELD, term, Field.Store.NO);
            luceneDoc.add(overallContextField);
        }
//        indexWriter.addDocument(luceneDoc); // this will add duplicates to an existing index
        indexWriter.updateDocument(new Term(DOC_ID_FIELD, doc.getId()), luceneDoc); // don't index docs with same docID twice
    }

    /**
     * Searches the Lucene index for documents which match doc's type and which contain similar terms in the overall
     * context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     *
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in
     * common with doc's overall context
     */
    public Set<IndexDocument> search(IndexDocument doc) {
        Set<IndexDocument> answers = new HashSet<>();
        try {
            BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
            boolQueryBuilder.setMinimumNumberShouldMatch(1);
            Query queryForType = new TermQuery(new Term(TYPE_FIELD, doc.getType()));
            boolQueryBuilder.add(queryForType, BooleanClause.Occur.MUST);
            for (String termStr : doc.getOverallContext()) {
                Term term = new Term(OVERALL_CONTEXT_FIELD, termStr);
                Query queryForOverallContext = new TermQuery(term);
                boolQueryBuilder.add(queryForOverallContext, BooleanClause.Occur.SHOULD);
            }
            Query boolQuery = boolQueryBuilder.build();
            List<Integer> docs = new ArrayList<>();
            Collector collector = new Collector() {
                @Override
                public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                    return new LeafCollector() {
                        @Override
                        public void setScorer(Scorer scorer) throws IOException {

                        }

                        @Override
                        public void collect(int doc) throws IOException {
                            docs.add(doc);
                        }
                    };
                }

                @Override
                public boolean needsScores() {
                    return false;
                }
            };
            searcher.search(boolQuery, collector);
            for (Integer luceneDocID : docs) {
                Document luceneDoc = searcher.doc(luceneDocID);
                String docID = luceneDoc.get(DOC_ID_FIELD);
//                System.out.println(docID);
                IndexDocument matchingDoc = deserializeIndexDocument(docID);
                answers.add(matchingDoc);
            }
        } catch (IndexNotFoundException e) {
            // if there is no inverted index structure for the type we're looking for we can't make a
            // completion recommendation
//            System.out.println("INDEX NOT AVAILABLE FOR TYPE " + doc.getType() + ". NO SUGGESTION POSSIBLE!");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // exit on IOException
        }
        return answers;
    }

    /**
     * deserialize IndexDocument object with the given docID
     *
     * @param docID
     * @return
     */
    abstract IndexDocument deserializeIndexDocument(String docID) throws IOException;

    @Override
    public void startIndexing() {
        initializeDirectory();
        IndexWriterConfig config = new IndexWriterConfig();
        // CREATE_OR_APPEND creates a new index if one does not exist, otherwise it opens the index and documents will be appended
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            indexWriter = new IndexWriter(indexDirectory, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishIndexing() {
        try {
            indexWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startSearching() {
        initializeDirectory();
        try {
            IndexReader reader = DirectoryReader.open(indexDirectory);
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishSearching() {
        // nothing to do here
    }

}
