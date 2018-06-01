package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.Set;

/**
 * Interface for an index that can be used to index the documents needed for the model
 */
public interface IInvertedIndex {

    void indexDocument(IndexDocument doc);
    Set<IndexDocument> search(IndexDocument doc);

}
