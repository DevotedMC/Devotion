package com.programmerdan.minecraft.devotion.datahandlers.impl;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.dao.Flyweight;
import com.programmerdan.minecraft.devotion.dao.database.SqlDatabase;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.util.FlowHelper;

/**
 * Database handler for all Flyweight types that properly extend
 * {@link Flyweight}.
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 * @author Aleksey-Terzi
 */
public class DatabaseDataHandler extends DataHandler {
	private FlowHelper statistics; 
	private long lastSampleTime;

	private SqlDatabase db;
	
	public FlowHelper getStatistics() {
		return statistics;
	}


	private DatabaseDataHandler() {
		super("database");
	}

	/**
	 * Generates a new DatabaseDataHandler from a config section.
	 * 
	 * @param config
	 *            the config to use
	 * @return a new DatabaseDataHandler, or null if setup failed.
	 */
	public static DatabaseDataHandler generate(ConfigurationSection config) {
		if (config == null) {
			Devotion.logger().log(Level.SEVERE,
							"Null configuration passed; DatabaseDataHandler not created");
			return null;
		}

		DatabaseDataHandler ddh = new DatabaseDataHandler();

		String host = config.getString("host");
		int port = config.getInt("port", 2306);
		String username = config.getString("username");
		String password = config.getString("password");
		String database = config.getString("database");
		// String schema = config.getString("schema");

		ddh.db = new SqlDatabase(host, port, database, username, password,Devotion.logger());

		if (!ddh.db.connect())
			return null;

		// is adaptive!
		ddh.setup(config.getLong("delay", 500l), config.getLong("max_run", 250l), true,
				config.getBoolean("debug"));

		// Target is sufficient samples for min 60 seconds of windowed average.
		int samples = Math.max(10, (ddh.getDelay() < 1 ? 10 : 60000 / (int) ddh.getDelay()) );
		ddh.statistics = new FlowHelper(samples);
		
		return ddh;
	}

	@Override
	public void teardown() {
		this.db.close();
	}

	@Override
	public void buildUp() {
		if (!this.db.initDb()) {
			this.db.close();
			setActive(false);
		}
		this.lastSampleTime = System.currentTimeMillis();
	}

	/**
	 * Simple process, generalized for all serializable Flyweights.
	 * 
	 * - Gets the datastream {@link getStream()} - Gets a Flyweight from the
	 * asynch FIFO queue {@link insertQueue} - Writes the flyweight {@link
	 * Flyweight.serialize(DataOutputStream)} - Continues until: - Nothing left
	 * in the queue - Time exceeded for write operations - Bytes exceed limit
	 * per cycle
	 * 
	 * Using atomic statistics handler. TODO: self-configure based on results.
	 */
	@Override
	protected void process() {
		debug(Level.INFO, "DatabaseDataHandler: Starting commit...");
		long in = System.currentTimeMillis();

		int records = 0;

		if (!isQueueEmpty()) {
			try {
				this.db.begin();
	
				while (System.currentTimeMillis() < in + this.getMaxRun() && 
						records < getMaxRun() && !isQueueEmpty()) {
					Flyweight obj = pollFromQueue();
	
					obj.serialize(this.db);
	
					records++;
				}
	
				this.db.commit();
	
			} catch (SQLException e) {
				debug(Level.WARNING, "Failed while serializing / commiting to DB", e);
			}
		} else {
			debug(Level.INFO, "DatabaseDataHandler: Event queue is empty, nothing to commit.");
		}

		long sTime = System.currentTimeMillis();
		statistics.sample(sTime - this.lastSampleTime, super.getAndClearInsertCount(), records);
			
		in = sTime - in;
		this.lastSampleTime = sTime;
		debug(Level.INFO, "DatabaseDataHandler: Done commit {0} records in {1} milliseconds",
				records, in);
		
		debug(Level.INFO, "DatabaseDataHandler: Inflow {0} -- Outflow {1} over {2} ms", 
				statistics.totalInflow(), statistics.totalOutflow(), statistics.totalSampleTime());

	}
}
