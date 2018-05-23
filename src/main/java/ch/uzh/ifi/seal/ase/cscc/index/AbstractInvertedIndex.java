package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.*;
import java.util.*;

public abstract class AbstractInvertedIndex {

    private static final String DOC_ID_FIELD = "docID";
    private static final String OVERALL_CONTEXT_FIELD = "overallContext";

    /**
     * Puts an IndexDocument in the index.
     * @param doc document to store in index
     * @throws IOException if the index can't be written to disk
     */
    public void indexDocument(IndexDocument doc) throws IOException {
        serializeIndexDocument(doc);
        Directory indexDirectory = getIndexDirectory(doc);
        IndexWriterConfig config = new IndexWriterConfig();
        // CREATE_OR_APPEND creates a new index if one does not exist, otherwise it opens the index and documents will be appended
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter w = new IndexWriter(indexDirectory, config);
        addDocToLuceneIndex(w, doc);
        w.close();
    }

    abstract void serializeIndexDocument(IndexDocument doc);

    /**
     * Returns either the RAM directory (if index is in-memory index) or the FSDirectory (if index is disk index).
     * @param doc
     * @return
     * @throws IOException
     */
    abstract Directory getIndexDirectory(IndexDocument doc) throws IOException;

    /**
     * Stores docID and the overall context in the Lucene index. The overall context will be what we search for at
     * retrieval time, the docID will be the result of the retrieval.
     * @param w
     * @param doc
     * @throws IOException
     */
    private void addDocToLuceneIndex(IndexWriter w, IndexDocument doc) throws IOException {
        Document luceneDoc = new Document();
        luceneDoc.add(new StringField(DOC_ID_FIELD, doc.getId(), Field.Store.YES));
        // store all terms in the overall context as tokens in the index
        // StringField: no tokenization
        // TextField: tokenization
        for (String term : doc.getOverallContext()) {
            luceneDoc.add(new StringField(OVERALL_CONTEXT_FIELD, term, Field.Store.YES));
        }
        w.addDocument(luceneDoc);
    }

    /**
     * Searches the Lucene index for documents which match doc's type and which contain similar terms in the overall
     * context as doc.
     * The query equals a boolean OR query of all terms in the overall context of doc.
     * @param doc document for which to find similar documents
     * @return documents which are similar to doc, i.e. documents whose overall context has at least one term in
     * common with doc's overall context
     * @throws IOException
     */
    public Set<IndexDocument> search(IndexDocument doc) throws IOException {
        Set<IndexDocument> answers = new HashSet<>();
        Directory indexDirectory = getIndexDirectory(doc);
        IndexReader reader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        for (String termStr : doc.getOverallContext()) {
            Term term = new Term(OVERALL_CONTEXT_FIELD, termStr);
            Query query = new TermQuery(term);
            boolQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
        }
        Query boolQuery = boolQueryBuilder.build();
        TopDocs docs = searcher.search(boolQuery, Integer.MAX_VALUE); // TODO: not sure if Integer.MAX_VALUE is such a good idea. There is probably a reason why Lucene does not offer to retrieve all matches at once.
        ScoreDoc[] hits = docs.scoreDocs;
        for (ScoreDoc hit : hits) {
            int luceneDocID = hit.doc;
            Document luceneDoc = searcher.doc(luceneDocID);
            String docID = luceneDoc.get(DOC_ID_FIELD);
            System.out.println(docID);
            IndexDocument matchingDoc = deserializeIndexDocument(docID);
            answers.add(matchingDoc);
        }
        return answers;
    }

    /**
     * deserialize IndexDocument object with the given docID
     * @param docID
     * @return
     */
    abstract IndexDocument deserializeIndexDocument(String docID);

    private static boolean luceneIndexExistsAndIsReadable(Directory indexDir) {
        boolean existsAndIsReadable = false;
        try {
            IndexReader reader = DirectoryReader.open(indexDir);
            existsAndIsReadable = true;
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return existsAndIsReadable;
    }

}
