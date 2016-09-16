package com.programmerdan.minecraft.devotion.monitors.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
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
		super("movement");
		this.config = config;
	}
	
	public static PlayerMovementMonitor generate(ConfigurationSection config) {
		if (config == null) return null;
		PlayerMovementMonitorConfig pmmc = new PlayerMovementMonitorConfig();
		pmmc.technique = SamplingMethod.valueOf(config.getString("sampling", "onevent"));
		pmmc.timeoutBetweenSampling = config.getLong("sampling_period", 1000l);
		pmmc.sampleSize = config.getInt("sampling_size", 50);
		PlayerMovementMonitor pmm = new PlayerMovementMonitor(pmmc);
		pmm.setDebug(config.getBoolean("debug", Devotion.instance().isDebug()));
		
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
		} else{
			asynch = new MonitorSamplingThread(this);
			if (SamplingMethod.continuous.equals(this.config.technique)) {
				asynch.startAdaptive(this.config.timeoutBetweenSampling);
			} else {
				asynch.startPeriodic(this.config.timeoutBetweenSampling);
			}
		}
		
		Devotion.instance().getServer().getPluginManager().registerEvents(this, Devotion.instance());

		super.setEnabled(true);
	}

	@Override
	public void onDisable() {
		if (!super.isEnabled()) {
			return;
		}

		// Note: if asynch is set, it auto-self-terminates when this monitor is disabled.
		//  all the same, calling cancel explicitly here (for now: TODO)
		if (asynch != null) {
			asynch.cancel();
		}

		super.setEnabled(false);
		
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
		checkRemove(start); // put first person back on the list.
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
}
