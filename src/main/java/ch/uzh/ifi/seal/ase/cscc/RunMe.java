package ch.uzh.ifi.seal.ase.cscc;

import ch.uzh.ifi.seal.ase.cscc.index.Indexer;

import java.io.IOException;

public class RunMe {

    private static final String eventsDir = "Data/Events";
    private static final String contextsDir = "Data/Contexts";

    public static void main(String[] args) {
        try {
            new RecommenderHelper(contextsDir, eventsDir).performTenFoldCrossValidation();
            //new RecommenderHelper(contextsDir, eventsDir).learnModel("Data/Model");
            //new RecommenderHelper(contextsDir, eventsDir).evaluateModel("Data/Model");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}