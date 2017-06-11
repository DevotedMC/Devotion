package com.programmerdan.minecraft.devotion.dao.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.programmerdan.minecraft.devotion.dao.info.PatchInfo;
import com.programmerdan.minecraft.devotion.util.ResourceHelper;

/**
 * @author Aleksey Terzi
 *
 */

public class SqlDatabase {
	private String host;
    private int port;
    private String db;
    private String user;
    private String password;
    private Logger logger;
    private Connection connection;
    
    private ArrayList<Source> sourceList;
    
    private PatchSource patchSource;
    public PatchSource getPatchSource() {
    	return this.patchSource;
    }

    private PlayerSource playerEventSource;
    public PlayerSource getPlayerEventSource() {
    	return this.playerEventSource;
    }
	
    private PlayerLoginSource playerLoginSource;
    public PlayerLoginSource getPlayerLoginSource() {
    	return this.playerLoginSource;
    }
    
    private PlayerQuitSource playerQuitSource;
    public PlayerQuitSource getPlayerQuitSource() {
    	return this.playerQuitSource;
    }

    private PlayerInteractSource playerInteractSource;
    public PlayerInteractSource getPlayerInteractSource() {
    	return this.playerInteractSource;
    }

    private PlayerKickSource playerKickSource;
    public PlayerKickSource getPlayerKickSource() {
    	return this.playerKickSource;
    }

    private PlayerTeleportSource playerTeleportSource;
    public PlayerTeleportSource getPlayerTeleportSource() {
    	return this.playerTeleportSource;
    }

    private PlayerRespawnSource playerRespawnSource;
    public PlayerRespawnSource getPlayerRespawnSource() {
    	return this.playerRespawnSource;
    }

    private PlayerToggleSource playerToggleSource;
    public PlayerToggleSource getPlayerToggleSource() {
    	return this.playerToggleSource;
    }

    private PlayerVelocitySource playerVelocitySource;
    public PlayerVelocitySource getPlayerVelocitySource() {
    	return this.playerVelocitySource;
    }

    private PlayerBedSource playerBedSource;
    public PlayerBedSource getPlayerBedSource() {
    	return this.playerBedSource;
    }

    private PlayerBucketSource playerBucketSource;
    public PlayerBucketSource getPlayerBucketSource() {
    	return this.playerBucketSource;
    }

    private PlayerDropItemSource playerDropItemSource;
    public PlayerDropItemSource getPlayerDropItemSource() {
    	return this.playerDropItemSource;
    }

    private PlayerEditBookSource playerEditBookSource;
    public PlayerEditBookSource getPlayerEditBookSource() {
    	return this.playerEditBookSource;
    }

    private PlayerEggThrowSource playerEggThrowSource;
    public PlayerEggThrowSource getPlayerEggThrowSource() {
    	return this.playerEggThrowSource;
    }

    private PlayerExpChangeSource playerExpChangeSource;
    public PlayerExpChangeSource getPlayerExpChangeSource() {
    	return this.playerExpChangeSource;
    }
    
    private PlayerFishSource playerFishSource;
    public PlayerFishSource getPlayerFishSource() {
    	return this.playerFishSource;
    }

    private PlayerGameModeChangeSource playerGameModeChangeSource;
    public PlayerGameModeChangeSource getPlayerGameModeChangeSource() {
    	return this.playerGameModeChangeSource;
    }

    private PlayerInteractEntitySource playerInteractEntitySource;
    public PlayerInteractEntitySource getPlayerInteractEntitySource() {
    	return this.playerInteractEntitySource;
    }

    private PlayerItemBreakSource playerItemBreakSource;
    public PlayerItemBreakSource getPlayerItemBreakSource() {
    	return this.playerItemBreakSource;
    }
    
    private PlayerItemConsumeSource playerItemConsumeSource;
    public PlayerItemConsumeSource getPlayerItemConsumeSource() {
    	return this.playerItemConsumeSource;
    }

    private PlayerItemHeldSource playerItemHeldSource;
    public PlayerItemHeldSource getPlayerItemHeldSource() {
    	return this.playerItemHeldSource;
    }
    
    private PlayerLevelChangeSource playerLevelChangeSource;
    public PlayerLevelChangeSource getPlayerLevelChangeSource() {
    	return this.playerLevelChangeSource;
    }

    private PlayerPickupItemSource playerPickupItemSource;
    public PlayerPickupItemSource getPlayerPickupItemSource() {
    	return this.playerPickupItemSource;
    }

    private PlayerResourcePackStatusSource playerResourcePackStatusSource;
    public PlayerResourcePackStatusSource getPlayerResourcePackStatusSource() {
    	return this.playerResourcePackStatusSource;
    }
    
    private PlayerShearEntitySource playerShearEntitySource;
    public PlayerShearEntitySource getPlayerShearEntitySource() {
    	return this.playerShearEntitySource;
    }
    
    private PlayerStatisticIncrementSource playerStatisticIncrementSource;
    public PlayerStatisticIncrementSource getPlayerStatisticIncrementSource() {
    	return this.playerStatisticIncrementSource;
    }

    private PlayerDeathSource playerDeathSource;
    public PlayerDeathSource getPlayerDeathSource() {
    	return this.playerDeathSource;
    }

    private BlockPlaceSource blockPlaceSource;
    public BlockPlaceSource getBlockPlaceSource() {
    	return this.blockPlaceSource;
    }

    private BlockBreakSource blockBreakSource;
    public BlockBreakSource getBlockBreakSource() {
    	return this.blockBreakSource;
    }

    private DropItemSource dropItemSource;
    public DropItemSource getDropItemSource() {
    	return this.dropItemSource;
    }
    
    private PlayerVehicleSource vehicleSource;
    public PlayerVehicleSource getPlayerVehicleSource() {
    	return this.vehicleSource;
    }

    public SqlDatabase(String host, int port, String db, String user, String password, Logger logger) {
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.password = password;
        this.logger = logger;
    }
    
    public boolean connect() {
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db + "?user=" + user + "&password=" + password;
        
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize JDBC driver.");
        }
        try {
            this.connection = DriverManager.getConnection(jdbc);
            
            initDataSources();
            
            this.logger.log(Level.INFO, "Connected to database!");
            return true;
        } catch (SQLException ex) { //Error handling below:
            this.logger.log(Level.SEVERE, "Could not connnect to the database! Connection string: " + jdbc, ex);
            return false;
        }
    }
    
    private void initDataSources() {
    	this.sourceList = new ArrayList<Source>();
    	
    	this.sourceList.add(this.patchSource = new PatchSource(this));
    	this.sourceList.add(this.playerEventSource = new PlayerSource(this));
    	this.sourceList.add(this.playerLoginSource = new PlayerLoginSource(this));
    	this.sourceList.add(this.playerInteractSource = new PlayerInteractSource(this));
    	this.sourceList.add(this.playerKickSource = new PlayerKickSource(this));
    	this.sourceList.add(this.playerQuitSource = new PlayerQuitSource(this));
    	this.sourceList.add(this.playerTeleportSource = new PlayerTeleportSource(this));
    	this.sourceList.add(this.playerRespawnSource = new PlayerRespawnSource(this));
    	this.sourceList.add(this.playerToggleSource = new PlayerToggleSource(this));
    	this.sourceList.add(this.playerVelocitySource = new PlayerVelocitySource(this));
    	this.sourceList.add(this.playerBedSource = new PlayerBedSource(this));
    	this.sourceList.add(this.playerBucketSource = new PlayerBucketSource(this));
    	this.sourceList.add(this.playerDropItemSource = new PlayerDropItemSource(this));
    	this.sourceList.add(this.playerEditBookSource = new PlayerEditBookSource(this));
    	this.sourceList.add(this.playerEggThrowSource = new PlayerEggThrowSource(this));
    	this.sourceList.add(this.playerExpChangeSource = new PlayerExpChangeSource(this));
    	this.sourceList.add(this.playerFishSource = new PlayerFishSource(this));
    	this.sourceList.add(this.playerGameModeChangeSource = new PlayerGameModeChangeSource(this));
    	this.sourceList.add(this.playerInteractEntitySource = new PlayerInteractEntitySource(this));
    	this.sourceList.add(this.playerItemBreakSource = new PlayerItemBreakSource(this));
    	this.sourceList.add(this.playerItemConsumeSource = new PlayerItemConsumeSource(this));
    	this.sourceList.add(this.playerItemHeldSource = new PlayerItemHeldSource(this));
    	this.sourceList.add(this.playerLevelChangeSource = new PlayerLevelChangeSource(this));
    	this.sourceList.add(this.playerPickupItemSource = new PlayerPickupItemSource(this));
    	this.sourceList.add(this.playerResourcePackStatusSource = new PlayerResourcePackStatusSource(this));
    	this.sourceList.add(this.playerShearEntitySource = new PlayerShearEntitySource(this));
    	this.sourceList.add(this.playerStatisticIncrementSource = new PlayerStatisticIncrementSource(this));
    	this.sourceList.add(this.playerDeathSource = new PlayerDeathSource(this));
    	this.sourceList.add(this.blockPlaceSource = new BlockPlaceSource(this));
    	this.sourceList.add(this.blockBreakSource = new BlockBreakSource(this));
    	this.sourceList.add(this.dropItemSource = new DropItemSource(this));
    	this.sourceList.add(this.vehicleSource = new PlayerVehicleSource(this));
    }
    
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "An error occured while closing the connection.", ex);
        }
    }
    
    public boolean isConnected() {
        try {
            return connection.isValid(5);
        } catch (SQLException ex) {
            this.logger.log(Level.SEVERE, "isConnected error!", ex);
        }
        return false;
    }
    
    public PreparedStatement prepareStatement(String sqlStatement) throws SQLException {
        return connection.prepareStatement(sqlStatement);
    }
    
    public void begin() throws SQLException {
    	this.connection.setAutoCommit(false);
    	
    	for(Source source : this.sourceList) {
    		source.startBatch();
    	}
    }
    
    public void commit() throws SQLException {
    	boolean hasUpdates = false;
    	
    	for(Source source : this.sourceList) {
    		if(source.hasUpdates()) {
    			source.executeBatch();
    			hasUpdates = true;
    		}
    	}
    	
    	if(hasUpdates) {
    		this.connection.commit();
    	}
    	
    	this.connection.setAutoCommit(true);
    }
    
    public boolean initDb() {
    	this.logger.log(Level.INFO, "Database initialization started...");
    	
    	ArrayList<String> list = ResourceHelper.readScriptList("/create_db.txt");
    	
		for(String script : list) {
			try {
				prepareStatement(script).execute();
	    	} catch (SQLException e) {
	    		this.logger.log(Level.SEVERE, "Database is NOT initialized.");
	    		this.logger.log(Level.SEVERE, "Failed script: \n" + script);
				e.printStackTrace();
				return false;
			}
		}
		
		this.logger.log(Level.INFO, "Database initialized.");
		
		this.logger.log(Level.INFO, "Applying patches to database...");
		
		try {
			applyPatches();
    	} catch (SQLException e) {
    		this.logger.log(Level.SEVERE, "Failed to apply patches.");
			e.printStackTrace();
			return false;
		}
		
		this.logger.log(Level.INFO, "Patches applied to database.");
		
		return true;
    }
    
    private void applyPatches() throws SQLException {
    	int patchIndex = 1;
    	String patchName = String.format("patch_%03d.txt", patchIndex);
    	ArrayList<String> scripts;
    	
    	while((scripts = ResourceHelper.readScriptList("/" + patchName)) != null) {
    		this.logger.log(Level.INFO, "Found patch " + patchName);
    		
    		if(this.patchSource.isExist(patchName)) {
    			this.logger.log(Level.INFO, "Skipping patch.");
    		} else {
	    		this.logger.log(Level.INFO, "Applying patch...");
	    		
	    		for(String script : scripts) {
	    			prepareStatement(script).execute();
	    		}
	    		
	    		PatchInfo patchInfo = new PatchInfo();
	    		patchInfo.patchName = patchName;
	    		patchInfo.appliedDate = new Timestamp(System.currentTimeMillis());
	    		
	    		begin();
	    		this.patchSource.insert(patchInfo);
	    		commit();
	    		
	    		this.logger.log(Level.INFO, "Patch applied.");
    		}
    		
    		patchName = String.format("patch_%03d.txt", ++patchIndex);
    	}
    }
}
