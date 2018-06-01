package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.*;
import java.util.*;

public abstract class AbstractInvertedIndex implements IInvertedIndex {

    // fields for indexing in Lucene index
    private static final String DOC_ID_FIELD = "docID";
    private static final String OVERALL_CONTEXT_FIELD = "overallContext";
    private static final String TYPE_FIELD = "type";
    private StringField docIdField = new StringField(DOC_ID_FIELD, "", Field.Store.YES);
    private StringField overallContextField = new StringField(OVERALL_CONTEXT_FIELD, "", Field.Store.NO);
    private StringField typeField = new StringField(TYPE_FIELD, "", Field.Store.NO);

    private Directory indexDirectory;

    void initializeIndexDirectory() {
        try {
            this.indexDirectory = getIndexDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puts an IndexDocument in the index.
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
            IndexWriterConfig config = new IndexWriterConfig();
            // CREATE_OR_APPEND creates a new index if one does not exist, otherwise it opens the index and documents will be appended
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter w = new IndexWriter(indexDirectory, config);
            addDocToLuceneIndex(w, doc);
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // exit on IOException
        }
    }

    /**
     * Checks if a document is already in the index.
     * @param doc
     */
    abstract boolean isIndexed(IndexDocument doc);

    abstract void serializeIndexDocument(IndexDocument doc) throws IOException;

    /**
     * Returns either the RAM directory (if index is in-memory index) or the FSDirectory (if index is disk index).
     * @param doc
     * @return
     * @throws IOException
     */
    abstract Directory getIndexDirectory() throws IOException;

    /**
     * Stores docID and the overall context in the Lucene index. The overall context will be what we search for at
     * retrieval time, the docID will be the result of the retrieval.
     * @param w
     * @param doc
     * @throws IOException
     */
    void addDocToLuceneIndex(IndexWriter w, IndexDocument doc) throws IOException {
        Document luceneDoc = new Document();
        docIdField.setStringValue(doc.getId());
        luceneDoc.add(docIdField);
        typeField.setStringValue(doc.getType());
        luceneDoc.add(typeField);
        // store all terms in the overall context as tokens in the index
        // StringField: no tokenization
        // TextField: tokenization
        for (String term : doc.getOverallContext()) {
            overallContextField.setStringValue(term);
            luceneDoc.add(overallContextField);
        }
//        w.addDocument(luceneDoc); // this will add duplicates to an existing index
        w.updateDocument(new Term(DOC_ID_FIELD, doc.getId()), luceneDoc); // don't index docs with same docID twice
    }

    /**
     * Searches the Lucene index for documents which match doc's type and which contain similar terms in the overall
     * context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in
     * common with doc's overall context
     */
    public Set<IndexDocument> search(IndexDocument doc) {
        Set<IndexDocument> answers = new HashSet<>();
        try {
            IndexReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
            Query queryForType = new TermQuery(new Term(TYPE_FIELD, doc.getType()));
            boolQueryBuilder.add(queryForType, BooleanClause.Occur.MUST);
            for (String termStr : doc.getOverallContext()) {
                Term term = new Term(OVERALL_CONTEXT_FIELD, termStr);
                Query queryForOverallContext = new TermQuery(term);
                boolQueryBuilder.add(queryForOverallContext, BooleanClause.Occur.SHOULD);
            }
            Query boolQuery = boolQueryBuilder.build();
            TopDocs docs = searcher.search(boolQuery, Integer.MAX_VALUE); // TODO: not sure if Integer.MAX_VALUE is such a good idea. There is probably a reason why Lucene does not offer to retrieve all matches at once.
            ScoreDoc[] hits = docs.scoreDocs;
            for (ScoreDoc hit : hits) {
                int luceneDocID = hit.doc;
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
     * @param docID
     * @return
     */
    abstract IndexDocument deserializeIndexDocument(String docID) throws IOException;

}
