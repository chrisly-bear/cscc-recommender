package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.model.events.completionevents.CompletionEvent;
import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.events.completionevents.TerminationState;
import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import ch.uzh.ifi.seal.ase.cscc.CompletionModel.CompletionModel;
import ch.uzh.ifi.seal.ase.cscc.CompletionModel.CompletionModelEval;
import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;
import ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration;
import ch.uzh.ifi.seal.ase.cscc.utils.IoHelper;
import ch.uzh.ifi.seal.ase.cscc.visitors.IndexDocumentExtractionVisitor;
import ch.uzh.ifi.seal.ase.cscc.visitors.InvocationExpressionVisitor;
import org.apache.commons.lang.mutable.MutableInt;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class RecommenderHelper {

    private String contextsDir;
    private String eventsDir;
    private final Logger LOGGER = Logger.getLogger(RecommenderHelper.class.getName());

    public RecommenderHelper(String contextsDir, String eventsDir) {
        this.contextsDir = contextsDir;
        this.eventsDir = eventsDir;
    }

    /**
     * Perform 10-fold cross-validation on the Contexts data set.
     * Test results are printed to the console.
     */
    public void performTenFoldCrossValidation() {
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

        boolean isDiskBasedInvertedIndex = (CSCCConfiguration.INDEX_IMPL == CSCCConfiguration.IndexImplementation.DiskBasedInvertedIndex);
        String continueWithZip = null;
        if (isDiskBasedInvertedIndex) {
            continueWithZip = readProgressFile(modelOutputDir);
        }

        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;

        CompletionModel completionModel = new CompletionModel();

        for (String zip : zips) {

            if (++zipCount > zipTotal || !RunMe.keepRunning ) break;

            if (continueWithZip != null && !continueWithZip.equals("")) {
                if (!zip.equals(continueWithZip)) {
                    continue;
                } else {
                    LOGGER.info("Found previous indexing state. Continuing indexing with zip " +
                                    zipCount + "/" + zipTotal + "(" + continueWithZip + ")");
                    continueWithZip = null; // reset variable, otherwise we won't get to the remaining zips
                    continue; // progress file stores the last fully processed zip file -> continue with next one
                }
            }

            if (CSCCConfiguration.PRINT_PROGRESS) {
                double perc = 100 * zipCount / (double) zipTotal;
                System.out.printf("## %s, processing %s...\n (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                while (ra.hasNext() && RunMe.keepRunning) {
                    Context ctx = ra.getNext(Context.class);

                    completionModel.train(ctx);
                }
            }

            // only store progress if current zip file processing has not been interrupted
            if (isDiskBasedInvertedIndex && RunMe.keepRunning) {
                writeProgressFile(modelOutputDir, zip);
            }
        }

        completionModel.store(modelOutputDir);
    }

    private String readProgressFile(String modelOutputDir) {
        File progressFile = new File(modelOutputDir + "/CSCCInvertedIndex/progress.txt");
        if (progressFile.exists() && progressFile.isFile()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(progressFile));
                String text = r.readLine();
                r.close();
                return text;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void writeProgressFile(String modelOutputDir, String text) {
        File progressFile = new File(modelOutputDir + "/CSCCInvertedIndex/progress.txt");
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(progressFile));
            w.write(text);
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Evaluates a learned model against the Events data set.
     * Test results are printed to the console.
     *
     * @param modelDir directory of the learned model
     */
    public void evaluateModel(String modelDir) {
        List<String> zips = IoHelper.findAllZips(eventsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;

        CompletionModel model = CompletionModel.fromDisk(modelDir);
        CompletionModelEval eval = new CompletionModelEval(model);

        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;

            if (CSCCConfiguration.PRINT_PROGRESS) {
                System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                        perc);
            }

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {

                while (ra.hasNext()) {
                    IDEEvent evt = ra.getNext(IDEEvent.class);

                    if (evt instanceof CompletionEvent) {
                        CompletionEvent event = (CompletionEvent) evt;

                        if (event.terminatedState == TerminationState.Applied && event.getLastSelectedProposal() != null) {
                            if (event.getLastSelectedProposal().getName() instanceof IMethodName) {
                                IMethodName methodName = (IMethodName) event.getLastSelectedProposal().getName();

                                IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
                                List<IndexDocument> indexDocuments = new LinkedList<>();

                                event.context.getSST().accept(indexDocumentExtractionVisitor, indexDocuments);

                                for (IndexDocument document : indexDocuments) {
                                    // only evaluate for the IndexDocument that correlates with the given method call

                                    if (document.getMethodCall().equals(methodName.getName())) {
                                        eval.evaluate(document);
                                    }
                                }
                            }

                            /*System.out.println("completion event, out of proposals");
                            for (IProposal proposal : event.proposalCollection) {
                                System.out.println("  " + proposal.getName().getIdentifier());
                            }
                            System.out.println("selected " + event.getLastSelectedProposal().getName().getIdentifier());*/
                        }
                    }
                }
            }

            if (zipCount++ >= zipTotal) break;
        }

        System.out.printf("precision = %.0f%%, recall = %.0f%%\n", eval.getPrecision(), eval.getRecall());
    }

    /**
     * Helper method which prints out SSTs to console. Can be useful to get an idea of how the SSTs are structured.
     * It also prints the InvocationExpressions (= method calls), which is what we're interested in.
     */
    public void printSSTsAndInvocationExpressions() {
        MutableInt invocationExpressionCount = new MutableInt(0);
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipsTotal = getNumZips(zips);
        int zipCount = 0;
        for (String zip : zips) {
            int ctxCount = 0;
            System.out.println("Processing " + zip);
            IReadingArchive ra = new ReadingArchive(new File(zip));
            while (ra.hasNext()) {
                Context ctx = ra.getNext(Context.class);
                // print SST
                ISST sst = ctx.getSST();
                System.out.println("================\nSST " + zipCount + "-" + ctxCount + ":\n================\n" + sst.toString());
                // print invocation expressions found by visitor
                System.out.println("================\nINVOCATION EXPRESSIONS " + zipCount + "-" + ctxCount + ":\n================\n");
                sst.accept(new InvocationExpressionVisitor(), invocationExpressionCount);
                ctxCount++;
            }
            ra.close();
            if (zipCount++ >= zipsTotal) break;
        }
        System.out.println("Total #InvocationExpressions: " + invocationExpressionCount);
    }

    private int getNumZips(List<String> zips) {
        if (CSCCConfiguration.LIMIT_ZIPS > 0) {
            return CSCCConfiguration.LIMIT_ZIPS;
        } else {
            return zips.size();
        }
    }

    private void modelFromTrainingBuckets(int bucketSize, int testBucketNum, CompletionModel completionModel) {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = getNumZips(zips);
        int zipCount = 0;

        for (String zip : zips) {

            if (CSCCConfiguration.PRINT_PROGRESS) {
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

        CompletionModelEval eval = new CompletionModelEval(completionModel);

        for (String zip : zips) {
            if (CSCCConfiguration.PRINT_PROGRESS) {
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
                            eval.evaluate(document);
                        }
                    }
                }
            }

            if (zipCount++ >= zipTotal) break;
        }

        System.out.printf("precision = %.0f%%, recall = %.0f%%\n", eval.getPrecision(), eval.getRecall());
    }
}
