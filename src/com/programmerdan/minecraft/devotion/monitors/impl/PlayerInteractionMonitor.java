package com.programmerdan.minecraft.devotion.monitors.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
			captureTimes[pit.getIdx()] = now;
			return res;
		}
	}
	
	private boolean checkDelay = true; 
	
	protected PlayerInteractionMonitorConfig getConfig() {
		return config;
	}
	
	private PlayerInteractionMonitor(PlayerInteractionMonitorConfig config) {
		super("interaction");
		this.config = config;
	}
	
	public static PlayerInteractionMonitor generate(ConfigurationSection config) {
		if (config == null) return null;
		PlayerInteractionMonitorConfig pimc = new PlayerInteractionMonitorConfig();
		pimc.delayBetweenSamples = config.getLong("sampling_delay", 10l);
		PlayerInteractionMonitor pim = new PlayerInteractionMonitor(pimc);
		pim.setDebug(config.getBoolean("debug", Devotion.instance().isDebug()));
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
	public void onPlayerBucketFill(PlayerBucketEmptyEvent event) {
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
}