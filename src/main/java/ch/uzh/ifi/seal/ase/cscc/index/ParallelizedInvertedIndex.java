package ch.uzh.ifi.seal.ase.cscc.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ParallelizedInvertedIndex extends AbstractInvertedIndex {

    /*
      CLASS & INSTANCE VARIABLES
     */

    private static final String SQL_TABLE_NAME = "indexdocuments";
    private final Logger LOGGER = Logger.getLogger(DiskBasedInvertedIndex.class.getName());

    private static final String INDEX_ROOT_DIR_NAME = "CSCCInvertedIndex";
    private static final String SERIALIZED_INDEX_DOCUMENTS_DIR_NAME = "IndexDocuments";
    private static final String SERIALIZED_INDEX_DOCUMENTS_SQLITE_FILE_NAME = "IndexDocuments.db";
    private static final String INVERTED_INDEX_STRUCTURES_DIR_NAME = "InvertedIndexStructures_Lucene";

    // directory where the Lucene index is persisted on disk
    private String indexRootDir;

    // connection to SQLite database
    private Connection dbConn;

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
            openSQLConnection();
        }
    }

    private void openSQLConnection() {
        String sqlUrl = "jdbc:sqlite:" + indexRootDir + "/" + SERIALIZED_INDEX_DOCUMENTS_SQLITE_FILE_NAME;
        try {
            dbConn = DriverManager.getConnection(sqlUrl);
            if (dbConn != null) {
                dbConn.setAutoCommit(false);
                createDBSchemaIfNotExists(dbConn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1); // exit on exception
        }
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
     * Call this method when this instance is not used anymore. It closes the SQLite connection.
     */
    public void cleanUp() {
        if (USE_SQLITE && dbConn != null) {
            try {
                dbConn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
    }

    /**
     * Puts an IndexDocument in the index.
     * @param doc document to store in index
     */
    @Override
    public void indexDocument(IndexDocument doc) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ParallelizedInvertedIndex.this.isIndexed(doc)) {
                        // do not put identical documents in index twice
//            System.out.println("doc " + doc.getId() + " is already indexed");
                        return;
                    }

                    serializeIndexDocument(doc);

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
                    // Resubmit this task
                    executorService.execute(this);
                } catch (IOException e) {
                    e.printStackTrace();
                    executorService.shutdown();
                    System.exit(1); // exit on IOException
                }
            }
        });
    }

    /*
      AbstractInvertedIndex IMPLEMENTATIONS
     */

    @Override
    boolean isIndexed(IndexDocument doc) {
        if (USE_SQLITE) {
            return isIndexedInDB(doc);
        } else {
            return isIndexedAsFile(doc);
        }
    }

    private boolean isIndexedInDB(IndexDocument doc) {
        String sqlSelect = "SELECT docid FROM " + this.SQL_TABLE_NAME + " WHERE docid=\"" + doc.getId() + "\"";
        try {
            dbConn.setAutoCommit(false);
            Statement stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            try {
                dbConn.commit();
            } catch (Exception ignored) {
            }
            boolean hasItems = rs.isBeforeFirst();
//            System.out.println(doc.getId() + " already indexed? " + hasItems);
            rs.close();
            stmt.close();
            return hasItems;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isIndexedAsFile(IndexDocument doc) {
        File f = new File(getPathToFileForIndexDocument(doc.getId()));
        return f.exists();
    }

    private String getPathToFileForIndexDocument(String docID) {
        return indexRootDir + "/" + SERIALIZED_INDEX_DOCUMENTS_DIR_NAME + "/" + docID + ".ser";
    }

    @Override
    void serializeIndexDocument(IndexDocument doc) throws IOException {
        if (USE_SQLITE) {
            try {
                serializeToSQLite(doc);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage()); // TODO: it's probably not best practise to turn an SQLException into an IOException
            }
        } else {
            serializeToFile(doc);
        }
    }

    private void serializeToSQLite(IndexDocument doc) throws SQLException {
        dbConn.setAutoCommit(false);
        String sqlInsert = "INSERT INTO " + this.SQL_TABLE_NAME + " VALUES(?,?,?,?,?,?,?)";
        PreparedStatement prepStmt = dbConn.prepareStatement(sqlInsert);
        prepStmt.setString(1, doc.getId());
        prepStmt.setString(2, doc.getType());
        prepStmt.setString(3, doc.getMethodCall());
        prepStmt.setString(4, serializeContext(doc.getLineContext()));
        prepStmt.setString(5, serializeContext(doc.getOverallContext()));
        prepStmt.setLong(6, doc.getLineContextSimhash());
        prepStmt.setLong(7, doc.getOverallContextSimhash());
        try {
            dbConn.commit();
        } catch (Exception ignored) {
        }
        prepStmt.close();
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

    @Override
    Directory getIndexDirectory(IndexDocument doc) throws IOException {
        String docType = doc.getType();
        String luceneIndexDirPath = indexRootDir + "/" + INVERTED_INDEX_STRUCTURES_DIR_NAME + "/" + docType;
        FSDirectory fileDirectory = FSDirectory.open(new File(luceneIndexDirPath).toPath());
        return fileDirectory;
    }

    @Override
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
