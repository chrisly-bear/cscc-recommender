package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.model.events.completionevents.CompletionEvent;
import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.events.completionevents.IProposal;
import cc.kave.commons.model.events.completionevents.TerminationState;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import ch.uzh.ifi.seal.ase.cscc.CompletionModel.CompletionModel;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.utils.IoHelper;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RecommenderHelper {
    private static final boolean PRINT_PROGRESS = false;
    private static final int LIMIT_ZIPS = 10;

    private String contextsDir;
    private String eventsDir;

    public RecommenderHelper(String contextsDir, String eventsDir) {
        this.contextsDir = contextsDir;
        this.eventsDir = eventsDir;
    }

    /**
     * Perform 10-fold cross-validation on the Contexts data set.
     * Used the local method isCompletionProposalSuccess to determine if a proposal was good.
     * Default behavior is to count it as a success if the chosen method name completion was among the top
     * 3 proposals.
     * Test results are printed to the console.
     */
    public void performTenFoldCrossValidation() throws IOException {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipsTotal = getNumZips(zips);

        // let's just divide the zips in 10 buckets, naively assuming they all contain about the
        // same number of contexts

        int bucketSize = zipsTotal / 10;

        for (int i = 0; i < 10; i++) {
            CompletionModel completionModel = new CompletionModel();
            System.out.printf("training model %d\n", i);

            modelFromTrainingBuckets(bucketSize, i, completionModel);

            performCrossValidation(bucketSize, i, completionModel);
        }
    }

    /**
     * Learn the model on the full Contexts data set.
     *
     * @param modelOutputDir an empty directory where to store the learned model
     */
    public void learnModel(String modelOutputDir) {

    }

    /**
     * Evaluates a learned model against the Events data set.
     * Used the local method isCompletionProposalSuccess to determine if a proposal was good.
     * Default behavior is to count it as a success if the chosen method name completion was among the top
     * 3 proposals.
     * Test results are printed to the console.
     *
     * @param modelDir directory of the learned model
     */
    public void evaluateModel(String modelDir) {
        foreachCompletionEvent((CompletionEvent event) -> {
            Context ctx = event.context;
            if (event.terminatedState == TerminationState.Applied && event.getLastSelectedProposal() != null) {
                System.out.println("completion event, out of proposals");
                for (IProposal proposal : event.proposalCollection) {
                    System.out.println("  " + proposal.getName());
                }
                System.out.println("selected " + event.getLastSelectedProposal().getName());
            }
        });
    }

    /**
     * Return success if method name completion is in top 3 recommendations
     */
    private boolean isCompletionProposalSuccess(CompletionModel model, IndexDocument document) {
        List<String> recommendations = model.recommendFor(document);

        for (String recommendation : recommendations) {
            if (document.getMethodCall().equals(recommendation)) return true;
        }

        return false;
    }

    private int getNumZips(List<String> zips) {
        if (LIMIT_ZIPS > 0) {
            return LIMIT_ZIPS;
        } else {
            return zips.size();
        }
    }

    private void modelFromTrainingBuckets(int bucketSize, int testBucketNum, CompletionModel completionModel) {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;

        for (String zip : zips) {

            if (PRINT_PROGRESS) {
                double perc = 100 * zipCount / (double) zipTotal;
                System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            // if we are in one of the training buckets
            if (zipCount < bucketSize * testBucketNum || zipCount >= (bucketSize * testBucketNum + 1)) {
                try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                    while (ra.hasNext()) {
                        Context ctx = ra.getNext(Context.class);

                        completionModel.train(ctx);
                    }
                }
            }

            if (zipCount++ >= zipTotal) break;
        }
    }

    private void performCrossValidation(int bucketSize, int testBucketNum, CompletionModel completionModel) {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;
        int samples = 0;
        int successes = 0;

        for (String zip : zips) {
            if (PRINT_PROGRESS) {
                double perc = 100 * zipCount / (double) zipTotal;
                System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            // if we are in the test bucket
            if (zipCount >= bucketSize * testBucketNum && zipCount < bucketSize * (testBucketNum + 1)) {
                try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                    while (ra.hasNext()) {
                        Context ctx = ra.getNext(Context.class);

                        ISST sst = ctx.getSST();

                        IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
                        List<IndexDocument> indexDocuments = new LinkedList<>();

                        sst.accept(indexDocumentExtractionVisitor, indexDocuments);

                        for (IndexDocument document : indexDocuments) {
                            if (isCompletionProposalSuccess(completionModel, document)) {
                                successes++;
                            }
                            samples++;
                        }
                    }
                }
            }

            if (zipCount++ >= zipTotal) break;
        }

        System.out.printf("success rate = %.0f%%\n", (100.f * successes) / samples);
    }

    private void foreachCompletionEvent(OnEventFoundCallback callback) {
        List<String> zips = IoHelper.findAllZips(eventsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;
        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;

            if (PRINT_PROGRESS) {
                System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                while (ra.hasNext()) {
                    IDEEvent evt = ra.getNext(IDEEvent.class);

                    if (evt instanceof CompletionEvent) {
                        callback.onEventFound((CompletionEvent) evt);
                    }
                }
            }

            if (zipCount++ >= zipTotal) break;
        }
    }
}
