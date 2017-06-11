package com.programmerdan.minecraft.devotion.datahandlers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import net.minecraft.server.v1_12_R1.MinecraftServer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.dao.Flyweight;


/**
 * Abstract interface for data handling
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 */
public abstract class DataHandler extends BukkitRunnable {
	
	private final String name;
	private long delay = -1;
	private boolean adaptive = false;
	private long maxRun;
	private ConcurrentLinkedQueue<Flyweight> insertQueue = new ConcurrentLinkedQueue<Flyweight>(); 
	long insertCount = 0l;
	
	private boolean debug = false;
	private boolean active = false;
	
	/**
	 * Allows subclasses to give it a name
	 * @param name
	 */
	protected DataHandler(String name) {
		this.name = name;
	}
	
	/**
	 * Get the name of the handler
	 * 
	 * @return handler name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return Indicates if this handler is live and handling flyweights.
	 */
	public final boolean isActive() {
		return this.active;
	}
	
	/**
	 * Sets the active mode for this handler.
	 * @param value true is active, false is inactive (and begins soft-teardown).
	 */
	protected final void setActive(boolean value) {
		this.active = value;
	}
	
	/**
	 * @return Indicates if this handler is in debug mode.
	 */
	public final boolean isDebug() {
		return this.debug;
	}
	
	/**
	 * Sets the new debug mode
	 * @param debug true if debug, false if not.
	 */
	public final void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	/**
	 * Gets the max running time, if applicable. Basically specifies limits on how long
	 * a data handler should be active during each execution cycle.
	 * Some handlers may ignore this.
	 * 
	 * @return the max running time per cycle, in milliseconds.
	 */
	protected long getMaxRun() {
		return this.maxRun;
	}

	/**
	 * Sets up the core attributes for all DataHandlers.
	 * 
	 * @param delay Delay between each execution of a DataHandler. This is in server Ticks (20 per sec) if 
	 *   not adaptive, in milliseconds if adaptive.
	 * 
	 * @param maxRun Max running time during each execution. See {@link #getMaxRun()}
	 * 
	 * @param adaptive Indicates if the delay between executions should be approximate to milliseconds,
	 *   regardless of server tick
	 *   
	 * @param debug Indicates if debug mode is active, and debug logging is output.
	 */
	protected void setup(long delay, long maxRun, boolean adaptive, boolean debug) {
		this.delay = delay;
		this.maxRun = maxRun;
		this.adaptive = adaptive;
		this.debug = debug;
	}

	/**
	 * @return The delay between each execution of a DataHandler. This is in server ticks if not adaptive,
	 *   in milliseconds if adaptive.
	 */
	protected long getDelay() {
		return delay;
	}

	/**
	 * Helpful wrapper for debug messages, respecting the current {@link #debug} setting
	 * 
	 * @param logLevel the actual {@link Level} to log as. 
	 * @param message the message to log.
	 */
	protected void debug(Level logLevel, String message) {
		if (debug) {
			Devotion.logger().log(logLevel, message);
		}
	}
	
	/**
	 * Helpful wrapper for debug messages, respecting the current {@link #debug} setting
	 * 
	 * @param logLevel The actual {@link Level} to log as.
	 * @param message the message (with formatting) to log.
	 * @param fill a set of objects to use with the format message.
	 */
	protected void debug(Level logLevel, String message, Object... fill) {
		if (debug) {
			Devotion.logger().log(logLevel, message, fill);
		}
	}

	/**
	 * Helpful wrapper for debug messages, respecting the current {@link #debug} setting
	 * 
	 * @param logLevel The actual {@link Level} to log as.
	 * @param message the message to log.
	 * @param thrown the {@link Throwable} to report
	 */
	protected void debug(Level logLevel, String message, Throwable thrown) {
		if (debug) {
			Devotion.logger().log(logLevel, message, thrown);
		}
	}

	/**
	 * Called by Devotion to start Data Handling.
	 * 
	 * @return true if able to begin, false otherwise.
	 */
	public final boolean begin() {
		if (active) {
			Devotion.logger().warning("Handler is already active");
			return true;
		}
		if (delay < 0) {
			Devotion.logger().warning("Cannot begin before calling setup()");
			return false;
		}
		
		buildUp();

		if (adaptive) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(Devotion.instance(), (Runnable) this, convertToTicks(this.delay) ); // convert milliseconds into ticks
		} else {
			this.runTaskTimerAsynchronously(Devotion.instance(), this.delay, this.delay);
		}
		active = true;

		return true;
	}

	/**
	 * Helper that uses the minecraft internal tps handler to convert an expected delay in milliseconds into 
	 * an actual delay in ticks.
	 */
	protected final long convertToTicks(long milliseconds) {
		double curtick = 20.0;
		try {
			curtick = MinecraftServer.getServer().recentTps[0];
		} catch (NullPointerException npe) {}
		return (long) ( (double) milliseconds / (1000.0 / curtick));
	}

	/**
	 * Called by Devotion plugin to end processing.
	 */
	public final void end() {
		if (active) {
			active = false;
			teardown();
		}
	}
	
	/**
	 * Asynchronous method called by listeners, puts data on the queue and returns quickly.
	 */
	public void insert(Flyweight data) {
		this.insertQueue.add(data);
		insertCount++;
	}
	
	/**
	 * Quickly checks if the queue is empty.
	 * 
	 * @return true if empty, false otherwise.
	 */
	protected boolean isQueueEmpty() {
		return this.insertQueue.isEmpty();
	}
	
	/**
	 * Polls the queue for an entry. Does not block. Wrapper for {@link ConcurrentLinkedQueue#poll()}
	 * 
	 * @return the oldest Flyweight on the queue, or null.
	 */
	protected Flyweight pollFromQueue() {
		return this.insertQueue.poll();
	}
	
	/**
	 * TODO: evaluate risk of not having this synchronized.
	 * TODO: potentially use AtomicLong.
	 * @return the latest insertCount.
	 */
	protected long getAndClearInsertCount() {
		long saveCount = insertCount;
		insertCount = 0;
		return saveCount;
	}
	
	/**
	 * Called by end() of the handler class; used by the Devotion plugin to end processing
	 * 
	 * Teardown is called AFTER {@link #active} is set to false.
	 */
	public abstract void teardown();
	
	/**
	 * Called by begin() of the handler class; used by the Devotion plugin to begin processing.
	 * 
	 * Buildup is called BEFORE {@link #active} is set to true.
	 */
	public abstract void buildUp();
	
	/**
	 * Subclasses must implement this method; they should use it to describe the actual
	 * functioning that a handler performs on a per-execution basis.
	 * 
	 * Ideally they should respect {@link #maxRun} using {@link #getMaxRun()}
	 */
	protected abstract void process();

	/**
	 * Called by the scheduler.
	 */
	public final void run() {
		if (!active) {
			this.cancel();
			return;
		}
		
		process();

		if (adaptive && active) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(Devotion.instance(), (Runnable) this, convertToTicks(this.delay) ); // convert milliseconds into ticks
		} else if (!active) {
			this.cancel();
		}
	}

}
