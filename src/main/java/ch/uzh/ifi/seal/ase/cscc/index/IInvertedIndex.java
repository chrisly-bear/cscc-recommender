package ch.uzh.ifi.seal.ase.cscc.index;

import java.io.IOException;
import java.util.Set;

public interface IInvertedIndex {

    void indexDocument(IndexDocument doc);
    Set<IndexDocument> search(IndexDocument doc);
    void persistToDisk(String targetDir) throws IOException;
    void initializeFromDisk(String sourceDir) throws IOException;

}
