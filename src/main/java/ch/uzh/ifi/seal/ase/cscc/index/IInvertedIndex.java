package ch.uzh.ifi.seal.ase.cscc.index;

import java.util.Set;

public interface IInvertedIndex {

    void indexDocument(IndexDocument doc);
    Set<IndexDocument> search(IndexDocument doc);
    void persistToDisk(String targetDir);
    void initializeFromDisk(String sourceDir);

}
