package com.programmerdan.minecraft.devotion.monitors.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import com.google.common.collect.Sets;
import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.config.PlayerInteractionMonitorConfig;
import com.programmerdan.minecraft.devotion.dao.Flyweight;
import com.programmerdan.minecraft.devotion.dao.flyweight.FlyweightFactory;
import com.programmerdan.minecraft.devotion.monitors.Monitor;
import com.programmerdan.minecraft.devotion.monitors.PlayerInteractionType;

/**
 * Player Interaction Monitor -- tracks interactions with the MC world.
 * 
 * Current:
 * <ul>
 *     <li>PlayerInteractEvent</li>
 *     <li>PlayerBedEnterEvent</li>
 *     <li>PlayerBedLeaveEvent</li>
 *     <li>PlayerBucketEvent</li>
 *     <li>PlayerDropItemEvent</li>
 *     <li>PlayerEditBookEvent</li>
 *     <li>PlayerEggThrowEvent</li>
 *     <li>PlayerExpChangeEvent</li>
 *     <li>PlayerFishEvent</li>
 *     <li>PlayerGameModeChangeEvent</li>
 *     <li>PlayerInteractEntityEvent</li>
 *     <li>PlayerItemBreakEvent</li>
 *     <li>PlayerItemConsumeEvent</li>
 *     <li>PlayerItemHeldEvent</li>
 *     <li>PlayerLevelChangeEvent</li>
 *     <li>PlayerPickupItemEvent</li>
 *     <li>PlayerResourcePackStatusEvent</li>
 *     <li>PlayerShearEntityEvent</li>
 *     <li>PlayerStatisticIncrementEvent</li>
 * 	   <li>PlayerDeathEvent</li>
 *	   <li>BlockPlaceEvent</li>
 *     <li>BlockBreakEvent</li>
 * </ul>
 * 
 * TODO:
 * <ul>
 *     <li>PlayerInventoryEvent</li>
 * </ul>
 * 
 * TODO: extract inventory events into PlayerInventoryMonitor
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 * @author Aleksey Terzi
 */
public class PlayerInteractionMonitor extends Monitor implements Listener {

	private PlayerInteractionMonitorConfig config;
	
	/**
	 * Records the last time a capture occurred, across all interactions.
	 */
	private ConcurrentHashMap<UUID, long[]> lastCapture;
	
	private boolean checkInsert(UUID player, PlayerInteractionType pit) {
		if (config.active.size() > 0 && !config.active.contains(pit)) {// if 0, all are OK; if > 0, only matches tracked
			return false;
		}
		if (!checkDelay) {
			return true;
		} else {
			long[] captureTimes = lastCapture.get(player);
			if (captureTimes == null) {
				captureTimes = new long[PlayerInteractionType.SIZE];
				lastCapture.put(player, captureTimes);
			}
			long now = System.currentTimeMillis();
			boolean res = (now - captureTimes[pit.getIdx()]) > config.delayBetweenSamples;
			if (res) { // if we are clear to capture, update time, otherwise hold it at old value.
				captureTimes[pit.getIdx()] = now;
			}
			return res;
		}
	}
	
	private boolean checkDelay = true; 
	
	protected PlayerInteractionMonitorConfig getConfig() {
		return config;
	}
	
	private PlayerInteractionMonitor(PlayerInteractionMonitorConfig config) {
		super("PlayerInteractionMonitor");
		this.config = config;
	}
	
	public static PlayerInteractionMonitor generate(ConfigurationSection config) {
		if (config == null) return null;
		PlayerInteractionMonitorConfig pimc = new PlayerInteractionMonitorConfig();
		pimc.delayBetweenSamples = config.getLong("sampling_delay", 10l);
		pimc.active = Sets.newConcurrentHashSet();
		if (config.isList("active")) {
			List<String> actives = config.getStringList("active");
			if (actives != null && actives.size() > 0) {
				actives.forEach( s -> {
					try {
						PlayerInteractionType pit = PlayerInteractionType.valueOf(s);
						pimc.active.add(pit);
					} catch (IllegalArgumentException e) {
						// not a match
					}
				});
			}
		}
		PlayerInteractionMonitor pim = new PlayerInteractionMonitor(pimc);
		pim.setDebug(config.getBoolean("debug", Devotion.instance().isDebug()));
		pim.initWatch(config);
		return pim;
	}
	
	@Override
	public void onEnable() {
		if (super.isEnabled()) {
			return;
		}
		
		if (config.delayBetweenSamples > 0l) {
			checkDelay = true;
			lastCapture = new ConcurrentHashMap<UUID, long[]>(5000, .75f, 5); // large pre-claimed space, default load factor, est. 5 concurrent threads
		}else {
			checkDelay = false;
		}
		
		Devotion.instance().getServer().getPluginManager().registerEvents(this, Devotion.instance());

		super.setEnabled(true);
	}

	@Override
	public void onDisable() {
		if (!super.isEnabled()) {
			return;
		}

		super.setEnabled(false);
		
		super.commitWatch();
				
		if (lastCapture != null) lastCapture.clear();
	}

	@Override
	protected void doSample() {
		// Currently unused for this monitor.
	}

	/**
	 * Quickly create a flyweight and pass it along to the active handlers.
	 * @param event
	 */
	private void insert(Event event) {
		Flyweight flyweight = FlyweightFactory.create(event);
		
		Devotion.instance().insert(flyweight);
	}

	/**
	 * Follow this pattern. Each new monitor uses checkInsert which sees if it's time to update.
	 * If no delay is set, it retuns fast with "true", else checks the last time a record was made ...
	 * see {@link #checkInsert(UUID, PlayerInteractionType)}
	 *  
	 * @param event the Interaction Event.
	 */
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerInteractEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerBedEnterEvent)) {
			insert(event);
		} // else skip.
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerBedLeaveEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerBucketFillEvent)) {
			insert(event);
		} // else skip.
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerBucketEmptyEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerDropItemEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerEditBook(PlayerEditBookEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerEditBookEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerEggThrowBook(PlayerEggThrowEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerEggThrowEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerExpChangeEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerFish(PlayerFishEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerFishEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerGameModeChangeEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerInteractEntityEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerItemBreak(PlayerItemBreakEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerItemBreakEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerItemConsumeEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerItemHeldEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerLevelChangeEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerPickupItemEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerResourcePackStatusEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerShearEntity(PlayerShearEntityEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerShearEntityEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.PlayerStatisticIncrementEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!canWriteLog(event.getEntity())) return;
		if (checkInsert(event.getEntity().getUniqueId(), PlayerInteractionType.PlayerDeathEvent)) {
			insert(event);
		} // else skip.
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onBlockPlace(BlockPlaceEvent event) {		
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.BlockPlaceEvent)) {
			insert(event);
		} // else skip.
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!canWriteLog(event.getPlayer())) return;
		if (checkInsert(event.getPlayer().getUniqueId(), PlayerInteractionType.BlockBreakEvent)) {
			insert(event);
		} // else skip.
	}

	@Override
	public boolean setConfig(String path, Object value) {
		if ("debug".equalsIgnoreCase(path) && value instanceof Boolean) {
			this.setDebug((Boolean) value);
			devotion().getConfig().set("monitors." + getName() + ".debug", (Boolean) value);
			return true;
		} else if ("sampling_delay".equals(path) && value instanceof String) {
			try{
				long delay = Long.valueOf((String) value);
				if (delay < 0) return false;
				this.config.delayBetweenSamples = delay;
				devotion().getConfig().set("monitors." + getName() + ".sampling_delay", delay);
				return true;
			} catch (NumberFormatException nfe) {
				return false;
			}
		} else if ("active".equals(path) && value instanceof String) {
			try {
				PlayerInteractionType pit = PlayerInteractionType.valueOf( (String) value);
				if (this.config.active.contains(pit)) {
					this.config.active.remove(pit);
					List<String> list = devotion().getConfig().getStringList("monitors." + getName() + ".active");
					list.remove(pit.toString());
					devotion().getConfig().set("monitors." + getName() + ".active", list);
					return true;
				} else {
					this.config.active.add(pit);
					List<String> list = devotion().getConfig().getStringList("monitors." + getName() + ".active");
					list.add(pit.toString());
					devotion().getConfig().set("monitors." + getName() + ".active", list);
					return true;					
				}
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
		sb.append(ChatColor.WHITE).append(" sampling_delay")
			.append(ChatColor.GRAY).append("=").append(ChatColor.GOLD).append(config.delayBetweenSamples)
			.append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Put a value greater than 0 to introduce a delay between sampling. Set to 0 to capture all interactions.\n");
		sb.append(ChatColor.WHITE).append(" active").append(ChatColor.GRAY).append(" - ")
			.append(ChatColor.DARK_AQUA).append("Name a specific type of interaction to toggle its capture. Toggle all off to enable all as a shortcut.\n");
		sb.append("  Valid interactions:\n");
		for (PlayerInteractionType pit : PlayerInteractionType.values()) {
			sb.append("   ").append(ChatColor.WHITE).append(pit.toString());
			if (this.config.active.contains(pit)) {
				sb.append(ChatColor.GRAY).append(" - ").append(ChatColor.GREEN).append("Active");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}