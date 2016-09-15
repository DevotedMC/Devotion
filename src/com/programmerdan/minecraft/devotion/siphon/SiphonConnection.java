package com.programmerdan.minecraft.devotion.siphon;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Soft wrapper for Connection
 * 
 */
public class SiphonConnection {
	private Connection connection;

	public static final SiphonConnection FAILURE = new SiphonConnection(null);

	// Each slice is nominally a transaction that cannot overlap with an attempt to store another slice. 
	// So we create and destroy a table specifically to earmark the beginning and end of the transaction.
	public static final String TRANS_TABLE = "slicetrans";
	public static final String TRANS_CREATE = "CREATE TABLE IF NOT EXISTS slicetrans (dev_player_id BIGINT(20) NOT NULL, event_time DATETIME NOT NULL) SELECT dev_player_id, event_time FROM dev_player WHERE dev_player_id = ?";
	public static final String TRANS_SELECT = "SELECT * FROM slicetrans";
	public static final String TRANS_REMOVE = "DROP TABLE IF EXISTS slicetrans";

	// So some testing reveals that constructing the slice table using precise times will be very expensive on the whole.
	// The goal of Siphon is to get the data out; not to be precise in its splits.
	// With that in mind we'll go a new route; leveraging dev_player_id field within dev_player to find in O(ln n) where 
	// to "stop" our retrieval (within some target "nearness" of real time) and use those dev_player_id values to 
	// build the slice_table. If follows my testing, this will be _very_ fast.
	public static final String BOUNDS = "SELECT min(dev_player_id), max(dev_player_id) FROM dev_player";
	public static final String SAMPLE_DATE = "SELECT event_time FROM dev_player WHERE dev_player_id = ?";

	// using the above three tests, and assuming that _roughly_ event_time is monotonically increasing w.r.t dev_player_id, 
	// we should be able to get very close to precise with only a few samples.
	public static final String SLICE_TABLE_NAME = "slicetable";
	public static final String SLICE_TABLE_SIZE = "SELECT count(*) FROM slicetable";
	public static final String REMOVE_SLICE_INDEX = "DROP INDEX IF EXISTS slice_table_idx ON slicetable";
	public static final String GET_SLICE_TABLE = "CREATE TABLE IF NOT EXISTS slicetable (trace_id VARCHAR(36) NOT NULL, dev_player_id BIGINT NOT NULL) SELECT trace_id, dev_player_id FROM dev_player WHERE dev_player_id <= ?";
	public static final String REMOVE_SLICE_TABLE = "DROP TABLE IF EXISTS slicetable";
	public static final String ADD_SLICE_INDEX = "CREATE INDEX IF NOT EXISTS slice_table_idx ON slicetable (trace_id, dev_player_id)";

	public static final String SLICE_DUMP_NAME = "slicedump_%1$s";
	public static final String FILE_SELECT = "CREATE TABLE slicedump_%1$s SELECT * FROM %1$s WHERE %1$s_id <= (SELECT MIN(%1$s_id) + ? FROM %1$s)";
	public static final String FILE_INDEX = "CREATE INDEX slicedump_%1$s_idx ON slicedump_%1$s (trace_id, %1$s_id)";
	public static final String FILE_SHRINK = "DELETE FROM slicedump_%1$s WHERE trace_id NOT IN (SELECT trace_id FROM slicetable)";
	public static final String FILE_DUMP = "SELECT * FROM slicedump_%1$s INTO OUTFILE '%3$s%1$s_%2$s.dat' FIELDS TERMINATED BY \";\" OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\\n'";

	public static final String GENERAL_DELETE = "DELETE FROM %1$s WHERE %1$s_id <= (SELECT MAX(%1$s_id) FROM slicedump_%1$s) AND %1$s_id IN (SELECT %1$s_id FROM slicedump_%1$s) LIMIT ?";
	public static final String SPECIAL_DELETE = "DELETE FROM %1$s WHERE %1$s_id <= (SELECT MAX(%1$s_id) FROM slicedump_%1$s) LIMIT ?";
	public static final String FILE_CLEANUP = "DROP TABLE IF EXISTS slicedump_%1$s";
	public static final String FILE_REMOVE_INDEX = "DROP INDEX IF EXISTS slicedump_%1$s_idx ON slicedump_%1$s";

	public SiphonConnection(Connection connection) {
		this.connection = connection;
	}
    
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            System.err.println("An error occured while closing the connection.");
			ex.printStackTrace();
        }
    }
    
    public boolean isConnected() {
		if (connection == null) return false;

        try {
            return connection.isValid(5);
        } catch (SQLException ex) {
            System.err.println("isConnected error!");
			ex.printStackTrace();
        }
        return false;
    }
    
    public Connection getConnection() {
    	return this.connection;
    }
    
    public PreparedStatement prepareStatement(String sqlStatement) throws SQLException {
		if (connection == null) {
			throw new SQLException("Connection not available.");
		}
        return connection.prepareStatement(sqlStatement);
    }

	public boolean checkTableExists(String tablename) throws SQLException {
		if (connection == null) {
			throw new SQLException("Connection not available.");
		}
		DatabaseMetaData metadata = connection.getMetaData();
		ResultSet tabledata = metadata.getTables(null, null, tablename, null);
		boolean ret = tabledata.next();
		tabledata.close();
		return ret;
	}
}

