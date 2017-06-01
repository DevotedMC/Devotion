package com.programmerdan.minecraft.devotion.api;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Watch events are how to trigger watch status changes for a player.
 * <br/>
 * 
 * For example, a monitoring plugin like NCP could check for unnature movement rates, and trigger watching to begin
 * for a specific player.
 * 
 * @author ProgrammerDan
 *
 */
public class WatchEvent extends Event {

	// Handler list for spigot events
	private static final HandlerList handlers = new HandlerList();
	
	private final UUID player;
	private final WatchEventType type;
	private List<String> messages; 
	
	public WatchEvent(WatchEventType type, UUID player) {
		this.type = type;
		this.player = player;
		this.messages = Collections.synchronizedList(new LinkedList<String>());
	}
	
	public UUID getPlayer() {
		return player;
	}
	
	public WatchEventType getType() {
		return type;
	}
	
	public void addMessage(String message) {
		messages.add(message);
	}
	
	public List<String> getMessages() {
		return messages;
	}
	
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}

}
