package ch.uzh.ifi.seal.ase.cscc;

import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;

public class RunMe {

    public static volatile boolean keepRunning = true;

    public static void main(String[] args) {

        addShutdownHook();
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).performTenFoldCrossValidation();
        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).learnModel(CSCCConfiguration.PERSISTENCE_LOCATION);
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).evaluateModel(CSCCConfiguration.PERSISTENCE_LOCATION);
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).printSSTsAndInvocationExpressions();
    }

    /**
     * Shuts down DiskBasedInvertedIndex gracefully so that indexing can continue when it is started again.
     */
    private static void addShutdownHook() {
        if (CSCCConfiguration.INDEX_IMPL == CSCCConfiguration.IndexImplementation.DiskBasedInvertedIndex) {
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println(" === SHUTTING DOWN GRACEFULLY ===");
                keepRunning = false;
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
        }
    }

}