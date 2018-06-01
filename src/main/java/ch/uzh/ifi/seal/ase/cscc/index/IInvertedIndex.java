package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.Set;

/**
 * Interface for an index that can be used to index the documents needed for the model
 */
public interface IInvertedIndex {

    /**
     * Puts an IndexDocument in the index.
     * Make sure you call {@link IInvertedIndex#startIndexing()} first and call {@link IInvertedIndex#finishIndexing()}
     * when done.
     * @param doc document to store in the index.
     */
    void indexDocument(IndexDocument doc);

    /**
     * Searches documents similar to {@code doc}. Make sure you call {@link IInvertedIndex#startSearching()} first and
     * call {@link IInvertedIndex#finishSearching()} when done.
     * @param doc document for which to find similar documents
     * @return
     */
    Set<IndexDocument> search(IndexDocument doc);

    /**
     * Call this before using {@link IInvertedIndex#indexDocument(IndexDocument)}.
     */
    void startIndexing();

    /**
     * Call this when you're done with indexing.
     */
    void finishIndexing();

    /**
     * Call this before using {@link IInvertedIndex#search(IndexDocument)}.
     */
    void startSearching();

    /**
     * Call this when you're done with searching.
     */
    void finishSearching();

}
