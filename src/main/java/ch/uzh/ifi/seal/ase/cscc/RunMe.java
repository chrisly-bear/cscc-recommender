package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.naming.types.ITypeName;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import cc.kave.commons.utils.ssts.SSTPrintingUtils;
import com.google.common.collect.Sets;
import ch.uzh.ifi.seal.ase.cscc.visitors.*;
import ch.uzh.ifi.seal.ase.cscc.utils.*;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RunMe {

	/*
	 * download the interaction data and unzip it into the root of this project (at
	 * the level of the pom.xml). Unpack it, you should now have a folder that
	 * includes a bunch of folders that have dates as names and that contain .zip
	 * files.
	 */
	public static String eventsDir = "Data/Events";

	/*
	 * download the context data and follow the same instructions as before.
	 */
	public static String contextsDir = "Data/Contexts";

	public static void main(String[] args) {
        processAllContexts();
	}

    private static void processAllContexts() {
        List<String> zips = IoHelper.findAllZips(contextsDir);
        int zipTotal = zips.size();
        int zipCount = 0;
        for (String zip : zips) {
            double perc = 100 * zipCount / (double) zipTotal;
            zipCount++;

            System.out.printf("## %s, processing %s... (%d/%d, %.1f%% done)\n", new Date(), zip, zipCount, zipTotal,
                    perc);

            try (IReadingArchive ra = new ReadingArchive(new File(zip))) {
                while (ra.hasNext()) {
                    Context ctx = ra.getNext(Context.class);
                    ISST sst = ctx.getSST();

                    IndexDocumentExtractionVisitor indexDocumentExtractionVisitor = new IndexDocumentExtractionVisitor();
                    sst.accept(indexDocumentExtractionVisitor, null);

//                    System.out.println("------------- (beginning of context) -------------");

                    // print a string representation of the SST to console
//                    printSST(sst);

                    // print all object types seen in the context
//                    collectTypes(sst);

                    // TODO 1: How many method invocations are typically called in a method body?
                    //double avgNumberOfMethodInvocationsInMethodBody = averageNumberOfMethodInvocationsInMethodBody(sst);
                    //System.out.println("Avg of method invocations: " + avgNumberOfMethodInvocationsInMethodBody);

                    // TODO 2: What fraction of classes of the dataset overrides the Equals method?
                    // ...

                    // TODO 3: How often do developers abort a code completion invocation on string variables?
                    // ...

//                    System.out.println("---------------- (end of context) ----------------");
                }
            }
        }
    }

    /**
     * print an SST to the terminal
     */
    private static void printSST(ISST sst) {
        System.out.println(SSTPrintingUtils.printSST(sst));
    }

    /**
     * traverse SST to find all types
     */
    private static void collectTypes(ISST sst) {
        // this example uses a simple visitor that collects types from a
        // provided syntax tree. please refer to the implementation to see
        // details.

        Set<ITypeName> seenTypes = Sets.newHashSet();
        sst.accept(new TypeCollectionVisitor(), seenTypes);

        // and do something with the types
        for (ITypeName type : seenTypes) {
            System.out.println(type.getFullName());
        }
    }

    /**
     * traverse SST to count number of method invocations within a method body
     */
    private static double averageNumberOfMethodInvocationsInMethodBody(ISST sst) {
        List<Integer> methodInvocationCounts = new LinkedList<>();
        sst.accept(new MethodInvocationVisitor(), methodInvocationCounts);
        return calculateAverage(methodInvocationCounts);
    }

    /**
     * returns 'NaN' if list has no elements
     */
    private static double calculateAverage(List<Integer> numbers) {
        int sum = 0;
        double size = numbers.size();
        for (int i : numbers) {
            sum += i;
        }
        return sum/size;
    }

}