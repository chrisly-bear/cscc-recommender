package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ParallelizedInvertedIndex implements IInvertedIndex {

    /*
      CLASS & INSTANCE VARIABLES
     */

    // fields for indexing in Lucene index
    private static final String DOC_ID_FIELD = "docID";
    private static final String OVERALL_CONTEXT_FIELD = "overallContext";

    private static final String SQL_TABLE_NAME = "indexdocuments";
    private final Logger LOGGER = Logger.getLogger(DiskBasedInvertedIndex.class.getName());

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String SERIALIZED_INDEX_DOCUMENTS_SQLITE_FILE_NAME = "IndexDocuments.db";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures_Lucene";

    // directory where the Lucene index is persisted on disk
    private String indexRootDir;

    // true: we store IndexDocuments in SQLite database
    // false: we serialize IndexDocuments to disk as files with .ser ending
    private boolean USE_SQLITE = true;

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    private ConcurrentHashMap<Directory, IndexWriter> filePathToIndexWriter = new ConcurrentHashMap<>();

    /*
      CONSTRUCTOR METHODS
     */

    /**
     * Constructor.
     * Uses an SQLite database to store the IndexDocument objects.
     * @param indexDir directory in which the inverted index will be stored.
     */
    public ParallelizedInvertedIndex(String indexDir) {
        this(indexDir, true);
    }

    /**
     * Constructor
     * @param indexDir directory in which the inverted index will be stored.
     * @param useRelationalDatabase true: we store IndexDocuments in SQLite database,
     *                              false: we serialize IndexDocuments to disk as files with .ser ending
     */
    public ParallelizedInvertedIndex(String indexDir, boolean useRelationalDatabase) {
        indexRootDir = indexDir + "/" + INDEX_ROOT_DIR_NAME;
        createDirectoryIfNotExists(new File(indexRootDir));
        this.USE_SQLITE = useRelationalDatabase;
        if (USE_SQLITE) {
            try {
                createDBSchemaIfNotExists(openSQLConnection());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Connection openSQLConnection() {
        String sqlUrl = "jdbc:sqlite:" + indexRootDir + "/" + SERIALIZED_INDEX_DOCUMENTS_SQLITE_FILE_NAME;
        Connection dbConn = null;
        try {
            dbConn = DriverManager.getConnection(sqlUrl);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1); // exit on exception
        }
        return dbConn;
    }

    private void createDBSchemaIfNotExists(Connection sqlConnection) throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS " + this.SQL_TABLE_NAME
                + "("
                + "   docid                  CHAR(64) PRIMARY KEY,"
                + "   type                   VARCHAR(1) NOT NULL," // SQLite does not enforce length of VARCHAR
                + "   method                 VARCHAR(1) NOT NULL,"
                + "   linecontext            VARCHAR(1),"
                + "   overallcontext         VARCHAR(1),"
                + "   linecontextsimhash     BIGINT,"
                + "   overallcontextsimhash  BIGINT"
                + ")";
        Statement stmt = sqlConnection.createStatement();
        stmt.execute(sqlCreate);
        stmt.close();
    }

    /**
     * Call this method when this instance is not used anymore. It closes the ExecuterService.
     */
    public void cleanUp() {
        executorService.shutdown();
    }

    /**
     * Puts an IndexDocument in the index.
     * @param doc document to store in index
     */
    @Override
    public void indexDocument(IndexDocument doc) {
        // open a new db connection for each index task
        try {
            Connection dbConn = openSQLConnection();
            dbConn.setAutoCommit(false);
            if (ParallelizedInvertedIndex.this.isIndexed(dbConn, doc)) {
            // do not put identical documents in index twice
//            System.out.println("doc " + doc.getId() + " is already indexed");
            dbConn.close(); // doc is already in index, we're done here. close db connection.
            return;
        }
            serializeIndexDocument(dbConn, doc);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        executorService.execute(new Runnable() {
            private int attempts = 0;
            @Override
            public void run() {
                attempts++;
                if (attempts > 50) {
                    System.out.println("attempt: " + attempts + "\n" + this.toString());
                }
                try {

                    Directory indexDirectory = ParallelizedInvertedIndex.this.getIndexDirectory(doc);
                    IndexWriterConfig config = new IndexWriterConfig();
                    // CREATE_OR_APPEND creates a new index if one does not exist, otherwise it opens the index and documents will be appended
                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    IndexWriter w;
                    if (filePathToIndexWriter.containsKey(indexDirectory)) {
                        w = filePathToIndexWriter.get(indexDirectory);
                        w.flush();
                        ParallelizedInvertedIndex.this.addDocToLuceneIndex(w, doc);
                        w.close();
                    } else {
                        w = new IndexWriter(indexDirectory, config);
                        filePathToIndexWriter.put(indexDirectory, w);
                        w.flush();
                        ParallelizedInvertedIndex.this.addDocToLuceneIndex(w, doc);
                        w.close();
                    }
                } catch (LockObtainFailedException lockObtainFailedException) {
                    // there's a lock on the Lucene index
                    // Resubmit this task
                    executorService.execute(this);
                    System.out.println("LockObtainFailedException encountered. Keeping task in queue. DocID = " + doc.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                    executorService.shutdown();
                    System.exit(1); // exit on IOException
                } catch (Throwable t) {
                    LOGGER.severe("UNCAUGHT EXCEPTION");
                    t.printStackTrace();
                }
            }
        });
    }

    void addDocToLuceneIndex(IndexWriter w, IndexDocument doc) throws IOException {
        Document luceneDoc = new Document();
        luceneDoc.add(new StringField(DOC_ID_FIELD, doc.getId(), Field.Store.YES));
        // store all terms in the overall context as tokens in the index
        // StringField: no tokenization
        // TextField: tokenization
        for (String term : doc.getOverallContext()) {
            luceneDoc.add(new StringField(OVERALL_CONTEXT_FIELD, term, Field.Store.NO));
        }
//        w.addDocument(luceneDoc); // this will add duplicates to an existing index
        w.updateDocument(new Term(DOC_ID_FIELD, doc.getId()), luceneDoc); // don't index docs with same docID twice
    }

    @Override
    public Set<IndexDocument> search(IndexDocument doc) {
        // TODO: implement search (you can probably just copy it from DiskBasedInvertedIndex)
        LOGGER.severe("Search for ParallelizedInvertedIndex not implemented!");
        return null;
    }

    /*
      AbstractInvertedIndex IMPLEMENTATIONS
     */

    boolean isIndexed(Connection dbConn, IndexDocument doc) throws SQLException {
        if (USE_SQLITE) {
            return isIndexedInDB(dbConn, doc);
        } else {
            return isIndexedAsFile(doc);
        }
    }

    private boolean isIndexedInDB(Connection dbConn, IndexDocument doc) throws SQLException {
        String sqlSelect = "SELECT docid FROM " + this.SQL_TABLE_NAME + " WHERE docid=\"" + doc.getId() + "\"";
        dbConn.setAutoCommit(false);
        Statement stmt = dbConn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlSelect);
        boolean hasItems = rs.isBeforeFirst();
//        System.out.println(doc.getId() + " already indexed? " + hasItems);
        rs.close();
        stmt.close();
        return hasItems;
    }

    private boolean isIndexedAsFile(IndexDocument doc) {
        File f = new File(getPathToFileForIndexDocument(doc.getId()));
        return f.exists();
    }

    private String getPathToFileForIndexDocument(String docID) {
        return indexRootDir + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME + "/" + docID + ".ser";
    }

    void serializeIndexDocument(Connection dbConn, IndexDocument doc) throws IOException, SQLException {
        if (USE_SQLITE) {
            serializeToSQLite(dbConn, doc);
        } else {
            serializeToFile(doc);
        }
    }

    private void serializeToSQLite(Connection dbConn, IndexDocument doc) throws SQLException {
//        dbConn.setAutoCommit(false); // we've already turned off autocommit in the 'isIndexedInDB', i.e. at this point we still have an open transaction
        String sqlInsert = "INSERT INTO " + this.SQL_TABLE_NAME + " VALUES(?,?,?,?,?,?,?)";
        PreparedStatement prepStmt = dbConn.prepareStatement(sqlInsert);
        prepStmt.setString(1, doc.getId());
        prepStmt.setString(2, doc.getType());
        prepStmt.setString(3, doc.getMethodCall());
        prepStmt.setString(4, serializeContext(doc.getLineContext()));
        prepStmt.setString(5, serializeContext(doc.getOverallContext()));
        prepStmt.setLong(6, doc.getLineContextSimhash());
        prepStmt.setLong(7, doc.getOverallContextSimhash());
        int rowAffected = prepStmt.executeUpdate();
        if (rowAffected != 1) {
            dbConn.rollback();
            LOGGER.severe("NO ROWS AFFECTED. ROLLING BACK!");
            throw new SQLException("No rows affected, rolling back!", null, 5);
        } else {
//            LOGGER.info("Doc " + doc.getId() + " successfully added to SQLite DB");
        }
        dbConn.commit(); // end the transaction that we've started in the 'isIndexedInDB' call
        prepStmt.close();
        dbConn.close(); // if we are at this point there has not been any exception which means the transaction was successful. we can close the db connection.
    }

    private void serializeToFile(IndexDocument doc) throws IOException {
        String contextsDirPath = indexRootDir + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME;
        createDirectoryIfNotExists(new File(contextsDirPath));
        FileOutputStream fileOut = new FileOutputStream(getPathToFileForIndexDocument(doc.getId()));
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(doc);
        out.close();
        fileOut.close();
    }

    Directory getIndexDirectory(IndexDocument doc) throws IOException {
        String docType = doc.getType();
        String luceneIndexDirPath = indexRootDir + "/" + INVERTED_INDEX_STRUCTURES_DIR_NAME + "/" + docType;
        FSDirectory fileDirectory = FSDirectory.open(new File(luceneIndexDirPath).toPath());
        return fileDirectory;
    }

    IndexDocument deserializeIndexDocument(String docID) throws IOException {
        if (USE_SQLITE) {
            return deserializeFromSQLite(docID);
        } else {
            return deserializeFromFile(docID);
        }
    }

    private IndexDocument deserializeFromSQLite(String docID) {
        String sqlSelect = "SELECT * FROM " + this.SQL_TABLE_NAME + " WHERE docid=\"" + docID + "\"";
        try {
            Connection dbConn = openSQLConnection();
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            boolean hasItems = rs.isBeforeFirst();
            if (hasItems) {
                String methodCall = rs.getString("method");
                String type = rs.getString("type");
                List<String> lineContext = deserializeContext(rs.getString("linecontext"));
                List<String> overallContext = deserializeContext(rs.getString("overallcontext"));
                long lineContextSimhash = rs.getLong("linecontextsimhash");
                long overallContextSimhash = rs.getLong("overallcontextsimhash");
                IndexDocument doc = new IndexDocument(docID, methodCall, type, lineContext, overallContext, lineContextSimhash, overallContextSimhash);
                return doc;
            }
            rs.close();
            stmt.close();
            dbConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String serializeContext(List<String> context) {
        StringBuilder sb = new StringBuilder();
        for (String s : context) {
            sb.append(s.length()); sb.append("~"); sb.append(s);
        }
        return sb.toString();
    }

    private List<String> deserializeContext(String context) {
        List<String> result = new LinkedList<>();
        int position = 0;
        while (position < context.length()) {
            int tildePosition = position + context.substring(position).indexOf("~");
            int wordLength = Integer.valueOf(context.substring(position, tildePosition));
            String s = context.substring(tildePosition+1, tildePosition+1+wordLength);
            result.add(s);
            position = tildePosition + wordLength + 1;
        }
        return result;
    }

    private IndexDocument deserializeFromFile(String docID) throws IOException {
        IndexDocument doc = null;
        FileInputStream fileIn = new FileInputStream(getPathToFileForIndexDocument(docID));
        ObjectInputStream in = new ObjectInputStream(fileIn);
        try {
            doc = (IndexDocument) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1); // exit on exception
        }
        in.close();
        fileIn.close();
        return doc;
    }


    /*
      FILE HANDLING METHODS
     */

    private void createDirectoryIfNotExists(File dir) {
        if (!dir.exists()) {
            System.out.println("'" + dir + "' does not exist yet. Creating it... ");
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1); // exit on IOException
            }
        }
    }

}
