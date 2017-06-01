package com.programmerdan.minecraft.devotion;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.ClassPath;
import com.programmerdan.minecraft.devotion.commands.CommandHandler;
import com.programmerdan.minecraft.devotion.dao.Flyweight;
import com.programmerdan.minecraft.devotion.dao.flyweight.FlyweightFactory;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.monitors.Monitor;
import com.programmerdan.minecraft.devotion.util.NameAPIUUIDResolver;
import com.programmerdan.minecraft.devotion.util.UUIDResolver;

/**
 * <p>Devotion quietly and un-obtrusively tracks everything everyone does. Check the README for details.</p>
 *
 * @author ProgrammerDan <programmerdan@gmail.com>
 * @since 1.0.0
 */
public class Devotion extends JavaPlugin {
	private CommandHandler commandHandler;
	private static Devotion instance;
	private Logger logger;
	private boolean debug = false;
	
	private UUIDResolver resolver;

	private Vector<Monitor> activeMonitors;
	
	private Vector<DataHandler> dataHandlers;
	
	public CommandHandler commandHandler() {
		return this.commandHandler;
	}

	public static Logger logger() {
		return Devotion.instance.getLogger();
	}

	/**
	 * @return Gets the singleton instance of this plugin.
	 */
	public static Devotion instance() {
		return Devotion.instance;
	}

	/**
	 * @return Returns if this plugin is in debug mode. 
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * Sets the debug mode
	 * @param debug new mode.
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Registers a new Monitor (which tracks player / game actions).
	 * This does NOT activate a monitor. 
	 * @see #onEnable() for that functionality.
	 *  
	 * @param monitor the Monitor to register
	 */
	public void registerMonitor(Monitor monitor) {
		activeMonitors.add(monitor);
	}

	/**
	 * Gets all the monitors currently registered.
	 * 
	 * @return Vector of all monitors
	 */
	public Vector<Monitor> getMonitors() {
		return activeMonitors;
	}
	
	/**
	 * Registers a new Data Handler (which stores player / game actions).
	 * Be careful how many you register as each new handler will introduce its own
	 * drain on system resources/time.
	 * 
	 * @param handler the DataHandler to register
	 */
	public void registerDataHandler(DataHandler handler) {
		dataHandlers.add(handler);
	}
	
	/**
	 * Gets all the currently registered Handlers.
	 * 
	 * @return Vector of DataHandler objects.
	 */
	public Vector<DataHandler> getHandlers() {
		return dataHandlers;
	}
	
	public void insert(Flyweight data){
		for (DataHandler dh : dataHandlers) {
			dh.insert(data);
		}
	}

	/**
	 * Called by Bukkit to set up this plugin.
	 * In term this plugin sets up its command handler,
	 *  activates its Data Handlers by calling the {@link DataHandler#begin()} function,
	 *  and activates its monitors by callingthe {@link Monitor#onEnable()} function.
	 */
	@Override
	public void onEnable() {
		// setting a couple of static fields so that they are available elsewhere
		instance = this;
		logger = this.getLogger();
		resolver = (Bukkit.getPluginManager().isPluginEnabled("NameLayer") ? new NameAPIUUIDResolver() : new UUIDResolver());
		commandHandler = new CommandHandler(this);
		activeMonitors = new Vector<Monitor>();
		dataHandlers = new Vector<DataHandler>();
		Bukkit.getPluginManager().registerEvents(new WatchListener(), this);
		
		FlyweightFactory.init();

		if (prepareConfig() && dataHandlers.size() > 0 && activeMonitors.size() > 0) {
			for (DataHandler dh : dataHandlers) {
				dh.begin();
			}
			for (Monitor m : activeMonitors) {
				m.onEnable();
			}
		} else {
			getLogger().severe("Unable to configure Devotion, no monitors active. Fix configuration and reload.");
			this.setEnabled(false);
			return;
		}
	}
	
	private boolean prepareConfig() {
		logger.info("Loading configuration");

		instance.saveDefaultConfig();
		instance.reloadConfig();
		FileConfiguration conf = instance.getConfig();

		instance.setDebug(conf.getBoolean("debug", false));

		if (instance.isDebug()) {
			logger.info("Devotion Debug mode active");
		}
		
		// Discover and configure Monitors
		ConfigurationSection monitors = conf.getConfigurationSection("monitors");
		ClassPath getClassesPath = null;
		try {
			getClassesPath = ClassPath.from(this.getClassLoader());
		} catch (IOException ioe) {
			logger.log(Level.WARNING, "Failed to get classloader!", ioe);
			return false;
		}

		for (ClassPath.ClassInfo clsInfo : getClassesPath.getTopLevelClasses("com.programmerdan.minecraft.devotion.monitors.impl")) {
			Monitor mon = null;
			Class<?> clazz = clsInfo.load();
			logger.log(Level.INFO, "Found a potential monitor in {0}, attempting to find a suitable constructor", clazz.getName());
			try {
				if (clazz != null && Monitor.class.isAssignableFrom(clazz)) {
					Method montMethod = clazz.getMethod("generate", ConfigurationSection.class);
					mon = (Monitor) montMethod.invoke(null, 
							monitors.getConfigurationSection(clazz.getSimpleName()));
				} else {
					logger.log(Level.INFO, "Found class {0} but is not a valid monitor. Skipping.", clazz);
				}
			} catch (NoSuchMethodException msme) {
				logger.log(Level.INFO, "Monitor defined as {0} is not well formed, lacks generate function.", clazz);
			} catch (SecurityException se) {
				logger.log(Level.INFO, "Monitor defined as {0} has reflection-inhibiting security.", clazz);
			} catch (IllegalAccessException iae) {
				logger.log(Level.INFO, "Monitor defined as {0} has access-inhibiting security.", clazz);
			} catch (IllegalArgumentException iae2) {
				logger.log(Level.INFO, "Monitor defined as {0} does not accept the correct parameters.", clazz);
			} catch (InvocationTargetException ite) {
				logger.log(Level.WARNING, "While provisioning Monitor from " + clazz + ", it threw an exception.", ite);
			} catch (NullPointerException npe) {
				logger.log(Level.WARNING, "While provisioning Monitor from " + clazz + ", it threw an NPE.", npe);
			}

			if (mon != null) { 
				logger.log(Level.INFO, "Monitor {0} instantiated, registering.", mon);
				this.registerMonitor(mon);
			}
		}
		
		// Discover and configure DAO
		ConfigurationSection dao = conf.getConfigurationSection("dao");

		for (ClassPath.ClassInfo clsInfo : getClassesPath.getTopLevelClasses("com.programmerdan.minecraft.devotion.datahandlers.impl")) {
			DataHandler han = null;
			Class<?> clazz = clsInfo.load();
			logger.log(Level.INFO, "Found a potential Data Handler in {0}, attempting to find a suitable constructor", clazz.getName());
			try {
				ConfigurationSection handler = dao.getConfigurationSection(clazz.getSimpleName());
				if (handler == null){
					logger.log(Level.WARNING, "No configuration supplied for potential Data Handler {0}, skipping.\nIf you want to use it, add a section {0}: under dao: in config.", clazz.getSimpleName());
				} else {
					if (DataHandler.class.isAssignableFrom(clazz)) {
						Method handMethod = clazz.getMethod("generate", ConfigurationSection.class);
						han = (DataHandler) handMethod.invoke(null, handler);
					} else {
						logger.log(Level.INFO, "Found class {0} but is not a valid DataHandler. Skipping.", clazz);
					}					
				}
			} catch (NoSuchMethodException msme) {
				logger.log(Level.INFO, "DataHandler defined as {0} is not well formed, lacks generate function.", clazz);
			} catch (SecurityException se) {
				logger.log(Level.INFO, "DataHandler defined as {0} has reflection-inhibiting security.", clazz);
			} catch (IllegalAccessException iae) {
				logger.log(Level.INFO, "DataHandler defined as {0} has access-inhibiting security.", clazz);
			} catch (IllegalArgumentException iae2) {
				logger.log(Level.INFO, "DataHandler defined as {0} does not accept the correct parameters.", clazz);
			} catch (InvocationTargetException ite) {
				logger.log(Level.WARNING,"While provisioning DataHandler from " + clazz + ", it threw an exception.", ite);
			} catch (NullPointerException npe) {
				logger.log(Level.WARNING,"While provisioning DataHandler from " + clazz + ", it threw an NPE.", npe);
			}
			
			if (han != null) { 
				logger.log(Level.INFO, "DataHandler {0} instantiated, registering.", clazz);
				Devotion.instance().registerDataHandler(han);
			}
		}
		
		return true;
	}

	/**
	 * Trying to be a good Bukkit resident. Fully supported teardown; first
	 * turns off registered Monitors by calling {@link Monitor#onDisable()} then
	 * turns off registered DataHandlers by calling {@link DataHandler#teardown()}.
	 * Each registered list is cleared, and the instance is nulled.
	 * Bukkit handles the rest.  
	 */
	@Override
	public void onDisable() {
		// end monitors
		for (Monitor m : activeMonitors) {
			m.onDisable();
		}
		for (DataHandler dh : dataHandlers) {
			dh.teardown();
		}
		activeMonitors.clear();
		dataHandlers.clear();
		instance = null;
	}

	public UUIDResolver getUUIDResolver() {
		return this.resolver;
	}
}
