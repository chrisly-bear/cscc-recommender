package ch.uzh.ifi.seal.ase.cscc;

import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;

public class RunMe {

    public static void main(String[] args) {

//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).performTenFoldCrossValidation();
        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).learnModel(CSCCConfiguration.PERSISTENCE_LOCATION);
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).evaluateModel(CSCCConfiguration.PERSISTENCE_LOCATION);
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).printSSTsAndInvocationExpressions();
    }

}