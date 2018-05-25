package ch.uzh.ifi.seal.ase.cscc.utils;

import ch.uzh.ifi.seal.ase.cscc.index.IInvertedIndex;
import ch.uzh.ifi.seal.ase.cscc.index.InvertedIndex;

public class CSCCConfiguration {

    // location of the KaVE dataset
    public static final String EVENTS_DIR = "Data/Events";
    public static final String CONTEXTS_DIR = "Data/Contexts";

    // location where the index should be stored
    public static final String PERSISTENCE_LOCATION = "Data/Model";
    public static final String PERSISTENCE_LOCATION_TEST = "/tmp";

    // number of statements to consider for overall context
    public static final int LAST_N_CONSIDERED_STATEMENTS = 6;

    // print progress during training
    public static final boolean PRINT_PROGRESS = false;
    // limit the amount of training data, 0 (or smaller) for all data
    public static final int LIMIT_ZIPS = 10;

    // default inverted index implementation to use
    public static IInvertedIndex getNewInvertedIndexInstance() {
        return new InvertedIndex();
    }

}
