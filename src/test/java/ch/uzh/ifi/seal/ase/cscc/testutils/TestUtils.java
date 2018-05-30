package ch.uzh.ifi.seal.ase.cscc.testutils;

import ch.uzh.ifi.seal.ase.cscc.index.IndexDocument;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TestUtils {

    /**
     * Creates test documents and puts them in the list.
     * @param list list to fill with test documents.
     */
    public static void fillWithTestDocuments(List<IndexDocument> list) {
        list.add(new IndexDocument("methodCall", "java.util.List", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "this", "is", "an", "overall", "context"
        )));
        list.add(new IndexDocument("flyAway", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "toString", "new"
        )));
        list.add(new IndexDocument("explode", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "setTimer", "getTarget", "try"
        )));
        list.add(new IndexDocument("identify", "org.entity.RocketShip", new LinkedList<>(), Arrays.asList(
                "toLowerCase", "context"
        )));
        list.add(new IndexDocument("diveDeep", "org.entity.Submarine", new LinkedList<>(), Arrays.asList(
                "equals", "toString", "pressureIsInSafeRange", "lookForFish", "getWaterTemperature"
        )));
    }

}
