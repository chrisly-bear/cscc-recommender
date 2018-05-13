package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.model.events.completionevents.CompletionEvent;
import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.events.completionevents.IProposal;
import cc.kave.commons.model.events.completionevents.TerminationState;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import ch.uzh.ifi.seal.ase.cscc.utils.IoHelper;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;

import java.io.File;
import java.util.Date;
import java.util.List;

public class RecommenderHelper {
    private static final boolean PRINT_PROGRESS = false;

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
     *
     * @param modelOutputDir an empty directory where to store the learned model for each of the 10 rounds. The directory is cleared afterwards.
     */
    public void performTenFoldCrossValidation(String modelOutputDir) {
        foreachContext((Context context) -> {
            ISST sst = context.getSST();

            IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
            sst.accept(indexDocumentExtractionVisitor, null);
        });
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

    private boolean isCompletionProposalSuccess() {
        return true;
    }

    private void foreachContext(OnContextFoundCallback callback) {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = zips.size();
        int zipCount = 0;
        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;
            zipCount++;

            if (PRINT_PROGRESS) {
                System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                while (ra.hasNext()) {
                    Context ctx = ra.getNext(Context.class);

                    callback.onContextFound(ctx);
                }
            }
        }
    }

    private void foreachCompletionEvent(OnEventFoundCallback callback) {
        List<String> zips = IoHelper.findAllZips(eventsDir);
        int zipTotal = zips.size();
        int zipCount = 0;
        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;
            zipCount++;

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
        }
    }
}
