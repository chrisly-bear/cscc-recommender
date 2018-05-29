package ch.uzh.ifi.seal.ase.cscc.utils;

public class CSCCConfiguration {

    // location of the KaVE dataset
    public static final String EVENTS_DIR = "Data/Events";
    public static final String CONTEXTS_DIR = "Data/Contexts";

    // location where the index should be stored
    public static final String PERSISTENCE_LOCATION = "Data/Model";
    public static final String PERSISTENCE_LOCATION_TEST = "/tmp";

    // number of statements to consider for overall context
    public static final int LAST_N_CONSIDERED_STATEMENTS = 6;

    // print progress about which zip file is currently being processed
    public static final boolean PRINT_PROGRESS = false;
    // print progress about contexts being processed within a zip file
    public static final boolean PRINT_PROGRESS_CONTEXTS = true;
    // limit the amount of training data, 0 (or smaller) for all data
    public static final int LIMIT_ZIPS = 10;


    /*
      DO NOT CONFIGURE ANYTHING BELOW THIS POINT
     */

    // global variable which is set to false when an interrupt (Ctrl + C) is recognized,
    // used to make a graceful shutdown during training phase
    public static volatile boolean keepRunning = true;

}
