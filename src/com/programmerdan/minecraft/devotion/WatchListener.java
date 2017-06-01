package com.programmerdan.minecraft.devotion;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.programmerdan.minecraft.devotion.api.WatchEvent;

/**
 * This allows third party plugins to control Devotion's monitoring of specific players.
 * You could for instance configure your anticheat to activate Devotion based on specific criteria, later removing them.
 * 
 * Internally this is leveraged from the Watch command which fires this event.
 * 
 * @author ProgrammerDan
 *
 */
public class WatchListener implements Listener {

	@EventHandler
	public void onWatchEvent(WatchEvent event) {
		Devotion.logger().log(Level.INFO, "{0} watching for {1}", new Object[]{event.getType(), event.getPlayer()});
		UUID player = event.getPlayer();
		switch(event.getType()) { 
		case TOGGLE:
			Devotion.instance().getMonitors().forEach( (m) -> {
				if (m.isWatched(player)) { 
					m.removeWatch(player);
					event.addMessage(ChatColor.GOLD + " Removed " + ChatColor.AQUA + "from " + m.getName());
				} else { 
					m.addWatch(player);
					event.addMessage(ChatColor.GREEN + " Added " + ChatColor.AQUA + "to " + m.getName());
				}
			} );
			break;
		case REMOVE:
			Devotion.instance().getMonitors().forEach( (m) -> {
				if (m.isWatched(player)) { 
					m.removeWatch(player);
					event.addMessage(ChatColor.GOLD + " Removed " + ChatColor.AQUA + "from " + m.getName());
				} else { 
					event.addMessage(ChatColor.RED + " No change " + ChatColor.AQUA + "in " + m.getName());
				}
			} );
			break;
		case ADD:
			Devotion.instance().getMonitors().forEach( (m) -> {
				if (!m.isWatched(player)) { 
					m.addWatch(player);
					event.addMessage(ChatColor.GREEN + " Added " + ChatColor.AQUA + "to " + m.getName());
				} else { 
					event.addMessage(ChatColor.RED + " No change " + ChatColor.AQUA + "in " + m.getName());
				}
			} );
			break;
		}
	}
}
