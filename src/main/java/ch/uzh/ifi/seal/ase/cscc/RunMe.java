package ch.uzh.ifi.seal.ase.cscc;

public class RunMe {

    private static final String eventsDir = "Data/Events";
    private static final String contextsDir = "Data/Contexts";

    public static void main(String[] args) {

//        new RecommenderHelper(contextsDir, eventsDir).performTenFoldCrossValidation();
        new RecommenderHelper(contextsDir, eventsDir).learnModel("Data/Model");
//        new RecommenderHelper(contextsDir, eventsDir).evaluateModel("Data/Model");
//        new RecommenderHelper(contextsDir, eventsDir).printSSTsAndInvocationExpressions();
    }

}