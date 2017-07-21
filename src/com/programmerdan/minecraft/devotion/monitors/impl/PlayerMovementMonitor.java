package com.programmerdan.minecraft.devotion.monitors.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.config.PlayerMovementMonitorConfig;
import com.programmerdan.minecraft.devotion.dao.Flyweight;
import com.programmerdan.minecraft.devotion.dao.flyweight.FlyweightFactory;
import com.programmerdan.minecraft.devotion.monitors.Monitor;
import com.programmerdan.minecraft.devotion.monitors.MonitorSamplingThread;
import com.programmerdan.minecraft.devotion.monitors.SamplingMethod;

/**
 * Player Movement Monitor -- tracks movement related calls.
 * 
 * Current:
 * <ul>
 *  <li>Player Login</li>
 *  <li>Player Join</li>
 *  <li>Player Quit</li>
 *  <li>Player Move</li>
 *  <li>Player Teleport - subclass of move, but with extra data</li>
 *  <li>Player Kick</li>
 *  <li>PlayerChangedWorldEvent</li>
 *  <li>PlayerRespawnEvent</li>
 *  <li>PlayerToggleFlightEvent</li>
 *  <li>PlayerToggleSneakEvent</li>
 *  <li>PlayerToggleSprintEvent</li>
 *  <li>PlayerVelocityEvent</li>
 * </ul>
 * 
 * @author ProgrammerDan<programmerdan@gmail.com>
 * @author Aleksey Terzi
 *
 */
public class PlayerMovementMonitor extends Monitor implements Listener {

	private ConcurrentLinkedQueue<UUID> playersToMonitor;
	private ConcurrentHashMap<UUID,Boolean> playersToRemove;
	private MonitorSamplingThread asynch;
	
	private boolean onlyAsynch;
	private boolean onlyEvent;
	
	private ConcurrentHashMap<UUID, Long> lastMovementSample;
	
	private PlayerMovementMonitorConfig config;
	
	private AtomicBoolean isSampling = new AtomicBoolean(false);
	
	protected PlayerMovementMonitorConfig getConfig() {
		return config;
	}
	
	private PlayerMovementMonitor(PlayerMovementMonitorConfig config) {
		super("PlayerMovementMonitor");
		this.config = config;
	}
	
	public static PlayerMovementMonitor generate(ConfigurationSection config) {
		if (config == null) return null;
		PlayerMovementMonitorConfig pmmc = new PlayerMovementMonitorConfig();
		pmmc.technique = SamplingMethod.valueOf(config.getString("sampling", "onevent"));
		if (pmmc.technique.equals(SamplingMethod.roundrobin)) {
			Devotion.logger().log(Level.WARNING, "sampling of roundrobin is not supported for PlayerMovementMonitor, using periodic instead");
			pmmc.technique = SamplingMethod.periodic;
		}
		pmmc.timeoutBetweenSampling = config.getLong("sampling_period", 1000l);
		pmmc.sampleSize = config.getInt("sampling_size", 50);
		PlayerMovementMonitor pmm = new PlayerMovementMonitor(pmmc);
		pmm.setDebug(config.getBoolean("debug", Devotion.instance().isDebug()));
		pmm.initWatch(config);
		
		return pmm;
	}

	@Override
	public void onEnable() {
		if (super.isEnabled()) {
			return;
		}
		
		playersToMonitor = new ConcurrentLinkedQueue<UUID>();
		playersToRemove = new ConcurrentHashMap<UUID, Boolean>(5000, .75f, 5); // large pre-claimed space, default load factor, est. 5 concurrent threads
		
		onlyAsynch = !SamplingMethod.onevent.equals(this.config.technique); // Onevent is only non-asynch sampling technique.
		if (!onlyAsynch) {
			lastMovementSample = new ConcurrentHashMap<UUID, Long>(5000, .75f, 5);
			onlyEvent = true;
			super.setEnabled(true);
		} else{
			super.setEnabled(true);
			asynch = new MonitorSamplingThread(this);
			if (SamplingMethod.continuous.equals(this.config.technique)) {
				asynch.startAdaptive(this.config.timeoutBetweenSampling);
			} else {
				asynch.startPeriodic(this.config.timeoutBetweenSampling);
			}
		}
		
		Devotion.instance().getServer().getPluginManager().registerEvents(this, Devotion.instance());
	}

	@Override
	public void onDisable() {
		if (!super.isEnabled()) {
			return;
		}

		// Note: if asynch is set, it auto-self-terminates when this monitor is disabled.
		//  all the same, calling cancel explicitly here (for now: TODO)
		if (asynch != null) {
			try {
				asynch.cancel();
			} catch (IllegalStateException e) {
				Devotion.logger().log(Level.WARNING, "Asynch thread never started -- weird.");
			}
		}

		super.setEnabled(false);
		
		super.commitWatch();
		
		if (playersToMonitor != null) playersToMonitor.clear();
		if (playersToRemove != null) playersToRemove.clear();
		if (lastMovementSample != null) lastMovementSample.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
		checkAdd(event.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
		checkAdd(event.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
		setRemove(event.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (onlyAsynch) {
			checkAdd(event.getPlayer().getUniqueId());
			return;
		}
		
		if (onlyEvent) {
			UUID p = event.getPlayer().getUniqueId();
			if (this.config.timeoutBetweenSampling <= 0) { // bypass throttling by setting timeout to 0.
				insert(event);
			} else {
				Long lastSample = lastMovementSample.get(p);
				long timePassed = lastSample != null ? System.currentTimeMillis() - lastSample: config.timeoutBetweenSampling;
				
				if (timePassed < this.config.timeoutBetweenSampling) return;
				
				insert(event);
				lastMovementSample.put(p, System.currentTimeMillis());
			}
		}
	}
	
	/**
	 * Captures movement events for vehicles, too!
	 * @see #onPlayerMove(PlayerMoveEvent)
	 * 
	 * @param event
	 */
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onVehicleMove(VehicleMoveEvent event) {
		Entity e = event.getVehicle().getPassenger();
		// TODO: 1.11 supports retrieval of passenger, 1.10 doesn't without NBT
		if (e instanceof Player) {
			Player p = (Player) e;

			if (!canWriteLog(p)) return;
			if (onlyAsynch) {
				checkAdd(p.getUniqueId());
				return;
			}
			
			if (onlyEvent) {
				if (this.config.timeoutBetweenSampling <= 0) { // bypass throttling by setting timeout to 0.
					insert(event);
				} else {
					UUID u = p.getUniqueId();
					Long lastSample = lastMovementSample.get(u);
					long timePassed = lastSample != null ? System.currentTimeMillis() - lastSample: config.timeoutBetweenSampling;
					
					if (timePassed < this.config.timeoutBetweenSampling) return;
					
					insert(event);
					lastMovementSample.put(u, System.currentTimeMillis());
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onVehicleEnter(VehicleEnterEvent event) {
		Entity e = event.getEntered();
		// TODO: 1.11 supports retrieval of passenger, 1.10 doesn't without NBT
		if (e instanceof Player) {
			Player p = (Player) e;

			if (!canWriteLog(p)) return;
			insert(event);
		}
	}

	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onVehicleExit(VehicleExitEvent event) {
		Entity e = event.getExited();
		// TODO: 1.11 supports retrieval of passenger, 1.10 doesn't without NBT
		if (e instanceof Player) {
			Player p = (Player) e;

			if (!canWriteLog(p)) return;
			insert(event);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerKick(PlayerKickEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerVelocity(PlayerVelocityEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		insert(event);
	}

	/**
	 * Called by MonitorThread, triggers a periodic sampling process
	 * 
	 * TODO: This might not be safe to call the Bukkit function. Trying it, we can recode if unsafe.
	 * TODO: incomplete
	 */
	protected void doSample() {
		if (!isEnabled()) return; // not enabled, stopping.
		if (isSampling.getAndSet(true)) return; // already sampling.
		int samples = 0;
		UUID start = playersToMonitor.poll();
		UUID now = start;
		while (now != null && samples <= this.config.sampleSize) {
			Player p = Bukkit.getPlayer(now);
			if (p != null) {
				insert(new PlayerMoveEvent(p, new Location(null, 0, 0, 0), new Location(null, 0, 0, 0)));
			}
			samples++;
			checkRemove(now); // put current samplee back on the list.
			now = playersToMonitor.poll();
			if (start.equals(now)) break; // we've sampled everyone
		}
		if (start != null) {
			checkRemove(start); // put first person back on the list.
		}
		isSampling.set(false);
	}
	
	/**
	 * Quickly create a flyweight and pass it along to the active handlers.
	 * @param event
	 */
	private void insert(PlayerEvent event) {
		Flyweight flyweight = FlyweightFactory.create(event);
		
		Devotion.instance().insert(flyweight);
	}
	
	/**
	 * Quickly create a flyweight and pass it along to the active handlers.
	 * @param event
	 */
	private void insert(VehicleEvent event) {
		Flyweight flyweight = FlyweightFactory.create(event);
		
		Devotion.instance().insert(flyweight);
	}
	
	/**
	 * Approx O(1) check to see if we should be skipping this player.
	 * 
	 * @param player
	 */
	private void checkRemove(UUID player) {
		Boolean remStat = playersToRemove.get(player);
		if (remStat != null) {
			playersToRemove.put(player, Boolean.FALSE);
		} else {
			playersToMonitor.add(player);
		}
	}
	
	/**
	 * Use on player login. 
	 * If in remove list, and true, removes from remove list. (removal scheduled but hasn't happened)
	 * If in remove list, and false, removes from remove list and adds to check list. (removal happened)
	 * If not in remove list, adds to check list.
	 * @param player
	 */
	private void checkAdd(UUID player) {
		Boolean remStat = playersToRemove.remove(player);
		if (remStat == null || !remStat.booleanValue()) {
			playersToMonitor.add(player);
		}
	}
	
	/**
	 * Use on player quit/kick/disconnect
	 * Adds player to remove list, with flag TRUE -- scheduled but hasn't happened
	 * @param player
	 */
	private void setRemove(UUID player) {
		playersToRemove.put(player, Boolean.TRUE);
	}
	

	@Override
	public boolean setConfig(String path, Object value) {
		if ("debug".equalsIgnoreCase(path) && value instanceof Boolean) {
			this.setDebug((Boolean) value);
			devotion().getConfig().set("monitors." + getName() + ".debug", (Boolean) value);
			return true;
		} else if ("sampling_period".equals(path) && value instanceof String) {
			try{
				long current = this.config.timeoutBetweenSampling;
				long delay = Long.valueOf((String) value);
				if (delay < 0) return false;
				this.config.timeoutBetweenSampling = delay;
				devotion().getConfig().set("monitors." + getName() + ".sampling_period", delay);
				if (current != delay && !this.onlyAsynch) {
					// retart.
					this.onDisable();
					this.onEnable();
				}
				return true;
			} catch (NumberFormatException nfe) {
				return false;
			}
		} else if ("sampling_size".equals(path) && value instanceof String) {
			try{
				int size = Integer.valueOf((String) value);
				if (size < 1) return false;
				this.config.sampleSize = size;
				devotion().getConfig().set("monitors." + getName() + ".sampling_size", size);
				return true;
			} catch (NumberFormatException nfe) {
				return false;
			}
		} else if ("sampling".equals(path) && value instanceof String) {
			try {
				SamplingMethod current = this.config.technique;
				SamplingMethod method = SamplingMethod.valueOf((String)value);
				if (method.equals(SamplingMethod.roundrobin)) {
					method = SamplingMethod.periodic;
				}
				if (current == method) return true;
				this.onDisable();
				this.config.technique = method;
				devotion().getConfig().set("monitors." + getName() + ".sampling", method.toString());
				this.onEnable();
				return true;
			} catch (IllegalArgumentException iae) {
				return false;
			}
		}
		return false;
	}

	@Override
	public String getConfigs() {
		StringBuffer sb = new StringBuffer();
		sb.append(ChatColor.WHITE).append(" debug")
			.append(ChatColor.GRAY).append("=").append(ChatColor.GOLD).append(this.isDebug())
			.append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Set to on / off to activate or deactivate debug mode, which is just increased output on what's going on.\n");
		sb.append(ChatColor.WHITE).append(" sampling_period")
			.append(ChatColor.GRAY).append("=").append(ChatColor.GOLD).append(config.timeoutBetweenSampling)
			.append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Put a value greater than 0 to introduce a delay between movement records or sampling passes. ")
			.append("Set to 0 to potentially capture all movement. Note this may restart the Monitor with some data loss.\n");
		sb.append(ChatColor.WHITE).append(" sampling_size")
			.append(ChatColor.GRAY).append("=").append(ChatColor.GOLD).append(config.sampleSize)
			.append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Put a value greater than 0, indicates the number of players to sample during a sampling pass.\n");
		sb.append(ChatColor.WHITE).append(" sampling")
			.append(ChatColor.GRAY).append("=").append(ChatColor.GOLD).append(config.technique.toString())
			.append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Use ").append(ChatColor.WHITE).append("onevent")
			.append(ChatColor.DARK_AQUA).append(" to capture movement on an event basis, use ")
			.append(ChatColor.WHITE).append("periodic").append(ChatColor.DARK_AQUA).append(" to capture movement using a tick based clock to interpret sampling period, use ")
			.append(ChatColor.WHITE).append("continuous").append(ChatColor.DARK_AQUA).append(" to capture movement using a self-adjusting clock to hold more strictly to milliseconds in sampling period.")
			.append(" Note this may restart the Monitor with some data loss.\n");
		return sb.toString();
	}
}
