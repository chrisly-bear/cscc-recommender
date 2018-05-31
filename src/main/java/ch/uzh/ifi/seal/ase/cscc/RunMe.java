package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.rsse.calls.datastructures.Tuple;
import ch.uzh.ifi.seal.ase.cscc.index.*;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RunMe {

    public static void main(String[] args) {

        /**
         * 10-fold cross validation using Contexts data set, make sure to use enough zips (≥ 20) in
         * {@link ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration#LIMIT_ZIPS} to get a better model and more
         * meaningful results.
         */
        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).performTenFoldCrossValidation();

        /*
         * train model from Contexts data set and store it on disk
         */
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).trainModel(CSCCConfiguration.PERSISTENCE_LOCATION);

        /*
         * evaluate trained model
         */
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).evaluateModel(CSCCConfiguration.PERSISTENCE_LOCATION);

        /*
         * print KaVE contexts to console
         */
//        new RecommenderHelper(CSCCConfiguration.CONTEXTS_DIR, CSCCConfiguration.EVENTS_DIR).printSSTsAndInvocationExpressions();

        /*
         * You can get a code completion for a context like so:
         */
        IInvertedIndex index = new DiskBasedInvertedIndex(CSCCConfiguration.PERSISTENCE_LOCATION);
        IndexDocument context = new IndexDocument(
                null, // we don't know the method, that's what we want to get a completion for -> leave null
                "System.Math",
                Arrays.asList(""), // line context
                Arrays.asList("Max", "Round", "double", "int")); // overall context
        Recommender csccRecommender = new Recommender(index, context);
        List<String> csccCompletion = csccRecommender.getTopThreeRecommendations();
        System.out.println("Suggested methods (CSCC): " + csccCompletion);

        /*
         * Or if you want to work with KaVE contexts, the KaVERecommender implements the ICallsRecommender interface:
         */
        KaVERecommender kaveRecommender = new KaVERecommender(index);
        Set<Tuple<IMethodName, Double>> kaveCompletion = kaveRecommender.query(context);
        System.out.println("Suggested methods (KaVE): " + kaveCompletion);
//        System.out.println("Size of model in bytes: " + kaveRecommender.getSize());
    }

}