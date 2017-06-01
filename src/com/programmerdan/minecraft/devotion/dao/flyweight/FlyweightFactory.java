package com.programmerdan.minecraft.devotion.dao.flyweight;

import java.util.ArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.programmerdan.minecraft.devotion.dao.DAOException;
import com.programmerdan.minecraft.devotion.dao.FlyweightType;

/**
 * 
 * @author Aleksey Terzi
 */
public class FlyweightFactory {
	private static final ArrayList<EventDefinition> Definitions = new ArrayList<EventDefinition>();
	
	public static void init() {
		Definitions.add(new EventDefinition(FlyweightType.Login.getId(), PlayerLoginEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Join.getId(), PlayerJoinEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Quit.getId(), PlayerQuitEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Move.getId(), PlayerMoveEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Interact.getId(), PlayerInteractEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Kick.getId(), PlayerKickEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Teleport.getId(), PlayerTeleportEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ChangedWorld.getId(), PlayerChangedWorldEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Respawn.getId(), PlayerRespawnEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ToggleFlight.getId(), PlayerToggleFlightEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ToggleSneak.getId(), PlayerToggleSneakEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ToggleSprint.getId(), PlayerToggleSprintEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Velocity.getId(), PlayerVelocityEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.BedEnter.getId(), PlayerBedEnterEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.BedLeave.getId(), PlayerBedLeaveEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Bucket.getId(), PlayerBucketEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Bucket.getId(), PlayerBucketFillEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Bucket.getId(), PlayerBucketEmptyEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.DropItem.getId(), PlayerDropItemEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.EditBook.getId(), PlayerEditBookEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.EggThrow.getId(), PlayerEggThrowEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ExpChange.getId(), PlayerExpChangeEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.Fish.getId(), PlayerFishEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.GameModeChange.getId(), PlayerGameModeChangeEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.InteractEntity.getId(), PlayerInteractEntityEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ItemBreak.getId(), PlayerItemBreakEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ItemConsume.getId(), PlayerItemConsumeEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ItemHeld.getId(), PlayerItemHeldEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.LevelChange.getId(), PlayerLevelChangeEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.PickupItem.getId(), PlayerPickupItemEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.PickupItem.getId(), PlayerPickupArrowEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ResourcePackStatus.getId(), PlayerResourcePackStatusEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.ShearEntity.getId(), PlayerShearEntityEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.StatisticIncrement.getId(), PlayerStatisticIncrementEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.PlayerDeath.getId(), PlayerDeathEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.BlockPlace.getId(), BlockPlaceEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.BlockPlace.getId(), BlockMultiPlaceEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.BlockBreak.getId(), BlockBreakEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.VehicleMove.getId(), VehicleMoveEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.VehicleEnter.getId(), VehicleEnterEvent.class));
		Definitions.add(new EventDefinition(FlyweightType.VehicleExit.getId(), VehicleExitEvent.class));
	}
	
	public static fPlayer create(Event event) {
		byte id = getId(event);
		
		return create(id, event);
	}
	
	public static fPlayer create(byte id) {
		return create(id, null);
	}

	private static fPlayer create(byte id, Event event) {
		if(id == FlyweightType.Login.getId()) return new fPlayerLogin((PlayerLoginEvent)event);
		if(id == FlyweightType.Join.getId()) return new fPlayerJoin((PlayerJoinEvent)event);
		if(id == FlyweightType.Quit.getId()) return new fPlayerQuit((PlayerQuitEvent)event);
		if(id == FlyweightType.Move.getId()) return new fPlayerMove((PlayerMoveEvent)event);
		if(id == FlyweightType.Interact.getId()) return new fPlayerInteract((PlayerInteractEvent)event);
		if(id == FlyweightType.Kick.getId()) return new fPlayerKick((PlayerKickEvent)event);
		if(id == FlyweightType.Teleport.getId()) return new fPlayerTeleport((PlayerTeleportEvent)event);
		if(id == FlyweightType.ChangedWorld.getId()) return new fPlayerChangedWorld((PlayerChangedWorldEvent)event);
		if(id == FlyweightType.Respawn.getId()) return new fPlayerRespawn((PlayerRespawnEvent)event);
		if(id == FlyweightType.ToggleFlight.getId()) return new fPlayerToggleFlight((PlayerToggleFlightEvent)event);
		if(id == FlyweightType.ToggleSneak.getId()) return new fPlayerToggleSneak((PlayerToggleSneakEvent)event);
		if(id == FlyweightType.ToggleSprint.getId()) return new fPlayerToggleSprint((PlayerToggleSprintEvent)event);
		if(id == FlyweightType.Velocity.getId()) return new fPlayerVelocity((PlayerVelocityEvent)event);
		if(id == FlyweightType.BedEnter.getId()) return new fPlayerBedEnter((PlayerBedEnterEvent)event);
		if(id == FlyweightType.BedLeave.getId()) return new fPlayerBedLeave((PlayerBedLeaveEvent)event);
		if(id == FlyweightType.Bucket.getId()) return new fPlayerBucket((PlayerBucketEvent)event);
		if(id == FlyweightType.DropItem.getId()) return new fPlayerDropItem((PlayerDropItemEvent)event);
		if(id == FlyweightType.EditBook.getId()) return new fPlayerEditBook((PlayerEditBookEvent)event);
		if(id == FlyweightType.EggThrow.getId()) return new fPlayerEggThrow((PlayerEggThrowEvent)event);
		if(id == FlyweightType.ExpChange.getId()) return new fPlayerExpChange((PlayerExpChangeEvent)event);
		if(id == FlyweightType.Fish.getId()) return new fPlayerFish((PlayerFishEvent)event);
		if(id == FlyweightType.GameModeChange.getId()) return new fPlayerGameModeChange((PlayerGameModeChangeEvent)event);
		if(id == FlyweightType.InteractEntity.getId()) return new fPlayerInteractEntity((PlayerInteractEntityEvent)event);
		if(id == FlyweightType.ItemBreak.getId()) return new fPlayerItemBreak((PlayerItemBreakEvent)event);
		if(id == FlyweightType.ItemConsume.getId()) return new fPlayerItemConsume((PlayerItemConsumeEvent)event);
		if(id == FlyweightType.ItemHeld.getId()) return new fPlayerItemHeld((PlayerItemHeldEvent)event);
		if(id == FlyweightType.LevelChange.getId()) return new fPlayerLevelChange((PlayerLevelChangeEvent)event);
		if(id == FlyweightType.PickupItem.getId()) return new fPlayerPickupItem((PlayerPickupItemEvent)event);
		if(id == FlyweightType.ResourcePackStatus.getId()) return new fPlayerResourcePackStatus((PlayerResourcePackStatusEvent)event);
		if(id == FlyweightType.ShearEntity.getId()) return new fPlayerShearEntity((PlayerShearEntityEvent)event);
		if(id == FlyweightType.StatisticIncrement.getId()) return new fPlayerStatisticIncrement((PlayerStatisticIncrementEvent)event);
		if(id == FlyweightType.PlayerDeath.getId()) return new fPlayerDeath((PlayerDeathEvent)event);
		if(id == FlyweightType.BlockPlace.getId()) return new fBlockPlace((BlockPlaceEvent)event);
		if(id == FlyweightType.BlockBreak.getId()) return new fBlockBreak((BlockBreakEvent)event);
		if(id == FlyweightType.VehicleMove.getId()) return new fPlayerVehicleMove((VehicleMoveEvent)event);
		if(id == FlyweightType.VehicleEnter.getId()) return new fPlayerVehicleEnter((VehicleEnterEvent)event);
		if(id == FlyweightType.VehicleExit.getId()) return new fPlayerVehicleExit((VehicleExitEvent)event);
		
		
		throw new DAOException("Event with ID = " + id + " is not registered.");
	}
	
	private static byte getId(Event event) {
		Class<?> cls = event.getClass();
		
		for(EventDefinition def : Definitions) {
			if(def.cls == cls) return def.id;
		}
		
		throw new DAOException("Event " + cls.toString() + " is not registered.");
	}
}