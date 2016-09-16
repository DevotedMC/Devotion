package com.programmerdan.minecraft.devotion.monitors;

import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.programmerdan.minecraft.devotion.Devotion;

/**
 * Base class for Monitor instances, each which will hold self-contained monitors of specific types of things
 * but leverage the larger DAO / persistence fire-and-forget backplane.
 * 
 * This is a goodly bit more "raw" then the DataHandler class.
 * 
 * @author ProgrammerDan
 */
public abstract class Monitor {

	private final String name;
	private boolean debug = false;
	private boolean enabled = false;
	
	/**
	 * Leveraged by subclasses to set the name of this Monitor
	 * @param name
	 */
	public Monitor(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the internal name of this Monitor.
	 * @return
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Convenience method for implementations, easy access to plugin
	 */
	protected Devotion devotion() {
		return Devotion.instance();
	}

	/**
	 * Convenience method for implementations, easy {@link Level} logging
	 */
	protected void log(Level level, String msg) {
		Devotion.logger().log(level, msg);
	}
	
	/**
	 * Convenience method for implementations, easy {@link Level} logging w/ format and data.
	 */
	protected void log(Level level, String format, Object...terms) {
		Devotion.logger().log(level, format, terms);
	}

	/**
	 * Convenience method for instances, easy error logging
	 */
	protected void log(Level level, String msg, Throwable err) {
		Devotion.logger().log(level, msg, err);
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
	 * Check if this monitor is active or not
	 */
	public final boolean isEnabled() {
		return enabled;
	}

	/**
	 * Method exposed to implementations to allow control of enabled status.
	 */
	protected final void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * @return true if debug mode is on, false otherwise.
	 */
	public final boolean isDebug() {
		return debug;
	}
	
	/**
	 * Can be used by manager or subclasses to set debug status. Default is off.
	 * 
	 * @param debug the new Debug mode to establish.
	 */
	public final void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Implementations must implement this with appropriate steps including configuration,
	 *   listener registration, task execution, DAO registration, and anything else necessary.
	 */
	public abstract void onEnable();
	
	/**
	 * Implementations must implement this with appropriate steps including unregistering
	 *   any listeners, halting any tasks, DAO unregistration, etc.
	 */
	public abstract void onDisable();
	
	/**
	 * Implementations may implement this if they intend to use a MonitorSamplingThread.
	 * 
	 * TODO: may be necessary for Monitor to extend bukkit runnable as well so that doSample() can be
	 * replaced with run() against a synchronized bukkit call...
	 * 
	 * Should be used to actually _do_ sampling if sampling is active.
	 */
	protected abstract void doSample();
	
	public final boolean canWriteLog(Player player) {
		return !player.hasPermission("Devotion.invisible");
	}
}
