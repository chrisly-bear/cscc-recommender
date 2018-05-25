package ch.uzh.ifi.seal.ase.cscc.utils;

import ch.uzh.ifi.seal.ase.cscc.index.IInvertedIndex;
import ch.uzh.ifi.seal.ase.cscc.index.InvertedIndex;

public class CSCCConfiguration {

    public static final String PERSISTENCE_LOCATION = "Data/Model";

    public static IInvertedIndex getNewInvertedIndexInstance() {
        return new InvertedIndex();
    }

}
