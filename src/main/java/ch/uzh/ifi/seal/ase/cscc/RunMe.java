package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.naming.types.ITypeName;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import cc.kave.commons.utils.ssts.SSTPrintingUtils;
import ch.uzh.ifi.seal.ase.cscc.index.Indexer;
import com.google.common.collect.Sets;
import ch.uzh.ifi.seal.ase.cscc.visitors.*;
import ch.uzh.ifi.seal.ase.cscc.utils.*;

import java.io.File;
import java.util.*;

public class RunMe {

	private static final String eventsDir = "Data/Events";

	private static final String contextsDir = "Data/Contexts";

	public static void main(String[] args) {
	    Indexer.indexAllContexts(contextsDir);
	}

}