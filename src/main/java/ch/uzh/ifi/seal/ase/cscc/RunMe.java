package ch.uzh.ifi.seal.ase.cscc;

import ch.uzh.ifi.seal.ase.cscc.index.Indexer;

public class RunMe {

    private static final String eventsDir = "Data/Events";

    private static final String contextsDir = "Data/Contexts";

    public static void main(String[] args) {
        Indexer.indexAllContexts(contextsDir);
    }

}