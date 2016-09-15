package com.programmerdan.minecraft.devotion.siphon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database config wrapper and connection source.
 * @author ProgrammerDan
 *
 */

public class SiphonDatabase {
	private String host;
    private int port;
    private String db;
    private String user;
    private String password;
    
    public SiphonDatabase(String host, int port, String db, String user, String password) {
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.password = password;

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            throw new SiphonFailure("Failed to initialize JDBC driver.");
        }
    }
    
    public SiphonConnection connect() {
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db + "?user=" + user + "&password=" + password;
        
        try {
            Connection connection = DriverManager.getConnection(jdbc);
            
            return new SiphonConnection(connection);
        } catch (SQLException ex) { //Error handling below:
            throw new SiphonFailure("Could not connnect to the database! Connection string: " + jdbc, ex);
        }
    }
}
