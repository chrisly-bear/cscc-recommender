package ch.uzh.ifi.seal.ase.cscc.index;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.commons.model.naming.impl.v0.codeelements.MethodName;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import cc.kave.rsse.calls.ICallsRecommender;
import cc.kave.rsse.calls.datastructures.Tuple;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

public class KaVeRecommender implements ICallsRecommender<IndexDocument> {

    private final IInvertedIndex index;
    private List<IndexDocument> baseCandidates;
    private List<IndexDocument> refinedCandidates;
    private List<ScoredIndexDocument> scoredCandidates;

    /**
     * @param index       inverted index structure (model) with which to suggest code completions
     */
    public KaVeRecommender(IInvertedIndex index) {
        this.index = index;
    }

    private void processQuery(IndexDocument receiverObj) {
        baseCandidates = getBaseCandidates(index, receiverObj);
        refinedCandidates = getRefinedCandidates(baseCandidates, receiverObj);
        scoredCandidates = sortRefinedCandidates(refinedCandidates, receiverObj);
    }

    private static List<IndexDocument> getBaseCandidates(IInvertedIndex index, IndexDocument receiverObj) {
        List<IndexDocument> baseCandidates = new LinkedList<>();
        baseCandidates.addAll(index.search(receiverObj));
        return baseCandidates;
    }

    private static List<IndexDocument> getRefinedCandidates(List<IndexDocument> baseCandidates, IndexDocument receiverObj) {
        int switchToLineContextThreshold = 30; // TODO: this threshold was picked at random and was never tested (maybe a good value is mentioned in the paper?)
        int k = 200;

        List<ScoredIndexDocument> scoredBaseCandidates = new LinkedList<>();
        for (IndexDocument baseCandidate : baseCandidates) {
            int lineContextDistance = baseCandidate.lineContextHammingDistanceToOther(receiverObj);
            int overallContextDistance = baseCandidate.overallContextHammingDistanceToOther(receiverObj);
            int viableDistance = overallContextDistance > switchToLineContextThreshold ? lineContextDistance : overallContextDistance;
            // we take the negative distance so that after sorting, candidates with lower distance (i.e. higher score) will come first
            ScoredIndexDocument scoredDoc = new ScoredIndexDocument(baseCandidate, -viableDistance, 0);
            scoredBaseCandidates.add(scoredDoc);
        }
        scoredBaseCandidates.sort(null); // compare using the Comparable interface implemented in ScoredIndexDocument
        // get the k top candidates
        List<ScoredIndexDocument> refinedScoredCandidates = scoredBaseCandidates.subList(0, Math.min(k, scoredBaseCandidates.size()));
        List<IndexDocument> refinedCandidates = new LinkedList<>();
        for (ScoredIndexDocument sid : refinedScoredCandidates) {
            refinedCandidates.add(sid.getIndexDocumentWithoutScores());
        }
        return refinedCandidates;
    }

    private static List<ScoredIndexDocument> sortRefinedCandidates(List<IndexDocument> refinedCandidates, IndexDocument receiverObj) {
        double filteringThreshold = 0.30;

        List<ScoredIndexDocument> sortedRefinedScoredCandidates = new LinkedList<>();
        for (IndexDocument refinedCandidate : refinedCandidates) {
            double normLCS = refinedCandidate.normalizedLongestCommonSubsequenceLengthOverallContextToOther(receiverObj);
            if (normLCS > filteringThreshold) {
                double normLev = refinedCandidate.normalizedLevenshteinDistanceLineContextToOther(receiverObj);
                ScoredIndexDocument scoredDoc = new ScoredIndexDocument(refinedCandidate, normLCS, normLev);
                sortedRefinedScoredCandidates.add(scoredDoc);
            }
        }
        sortedRefinedScoredCandidates.sort(null); // compare using the Comparable interface implemented in ScoredIndexDocument
        // remove duplicates
        removeDuplicates(sortedRefinedScoredCandidates);

        return sortedRefinedScoredCandidates;
    }

    /**
     * Removes documents with same method call name while leaving the order of the list unchanged.
     */
    private static void removeDuplicates(List<ScoredIndexDocument> sortedRefinedCandidates) {
        Set<String> uniqueMethodNames = new HashSet<>();
        for (int i = 0; i < sortedRefinedCandidates.size(); i++) {
            String methodName = sortedRefinedCandidates.get(i).getMethodCall();
            if (uniqueMethodNames.contains(methodName)) {
                sortedRefinedCandidates.remove(i);
                i--;
            } else {
                uniqueMethodNames.add(methodName);
            }
        }
    }

    /*
      ICallsRecommender IMPLEMENTATIONS
     */

    /**
     * Use the recommender-specific query format to query proposals.
     * This is the recommended way to get code completions.
     *
     * @param query
     *            the query in a format specfic to the recommender
     * @return a sorted set of the proposed methods plus probability
     */
    @Override
    public Set<Tuple<IMethodName, Double>> query(IndexDocument query) {
        processQuery(query);
        Set<Tuple<IMethodName, Double>> result = new LinkedHashSet<>();
        final int CANDIDATES_TO_SUGGEST = 3;
        // get the top three
        for (ScoredIndexDocument scoreDoc : scoredCandidates.subList(0, Math.min(CANDIDATES_TO_SUGGEST, scoredCandidates.size()))) {
            IMethodName methodName = new MethodName(scoreDoc.getMethodCall());
            Double score = scoreDoc.getScore1();
            Tuple<IMethodName, Double> tuple = Tuple.newTuple(methodName, score);
            result.add(tuple);
        }
        return result;
    }

    /**
     * Query proposals by providing a context.
     *
     * NOTE: Since the CSCC algorithm works on single receiver objects of one specific type and not entire
     *       contexts which may contain several method invocations on receiver objects of different types, this method
     *       combines the contexts of all method invocations into one, then makes a recommendation based on this combined
     *       context and the type of the last receiver object in the context. CSCC is not intended to be used this way
     *       and the results may not be very meaningful.
     *
     * @param ctx
     *            the query as a Context
     * @return a sorted set of the proposed methods plus probability
     */
    @Override
    public Set<Tuple<IMethodName, Double>> query(Context ctx) {
        ISST sst = ctx.getSST();
        ISSTNodeVisitor visitor = new IndexDocumentExtractionVisitor();
        List<IndexDocument> methodInvocations = new LinkedList<>();
        sst.accept(visitor, methodInvocations);
        IndexDocument mergedContexts = mergeContexts(methodInvocations);
        return query(mergedContexts);
    }

    private IndexDocument mergeContexts(List<IndexDocument> contexts) {
        String lastType = contexts.get(contexts.size() - 1).getType();
        List<String> combinedLineContext = new LinkedList<>();
        List<String> combinedOverallContext = new LinkedList<>();
        for (IndexDocument doc : contexts) {
            combinedLineContext.addAll(doc.getLineContext());
            combinedOverallContext.addAll(doc.getOverallContext());
        }
        return new IndexDocument(null, lastType, combinedLineContext, combinedOverallContext);
    }

    /**
     * Query proposals by providing a context and the proposals given by the IDE.
     *
     * NOTE 1: Same as {@link KaVeRecommender#query(Context)}. The IDE proposals are
     *         ignored.
     *
     * NOTE 2: Since the CSCC algorithm works on single receiver objects of one specific type and not entire
     *         contexts which may contain several method invocations on receiver objects of different types, this method
     *         combines the contexts of all method invocations into one, then makes a recommendation based on this combined
     *         context and the type of the last receiver object in the context. CSCC is not intended to be used this way
     *         and the results may not be very meaningful.
     *
     * @param ctx
     *            the query as a Context
     * @param ideProposals
     *            the proposal given by the IDE
     * @return a sorted set of the proposed methods plus probability
     */
    @Override
    public Set<Tuple<IMethodName, Double>> query(Context ctx, List ideProposals) {
        return query(ctx);
    }

    /**
     * Request the size of the underlying model.
     * Might take a long time for large models because it scans the model directory recursively.
     * For large models, the int return value cannot store the size of the model. In this case -1 is returned.
     *
     * @return model size in bytes
     */
    @Override
    public int getSize() {
        try {
            return FileUtils.sizeOfAsBigInteger(new File(CSCCConfiguration.PERSISTENCE_LOCATION)).intValueExact();
        } catch (ArithmeticException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
