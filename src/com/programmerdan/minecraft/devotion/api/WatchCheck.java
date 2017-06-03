package com.programmerdan.minecraft.devotion.api;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WatchCheck extends Event {

	// Handler list for spigot events
	private static final HandlerList handlers = new HandlerList();
	private final UUID player;
	
	private List<String> monitors; 
	
	public WatchCheck(UUID player) {
		this.player = player;
		this.monitors = Collections.synchronizedList(new LinkedList<String>());
	}
	
	
	public UUID getPlayer() {
		return player;
	}
	
	public void addMonitor(String monitor) {
		this.monitors.add(monitor);
	}
	
	public List<String> getMonitors() {
		return this.monitors;
	}
	
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
