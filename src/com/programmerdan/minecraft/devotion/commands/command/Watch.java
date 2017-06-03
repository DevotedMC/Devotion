package com.programmerdan.minecraft.devotion.commands.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.api.WatchEvent;
import com.programmerdan.minecraft.devotion.api.WatchEventType;
import com.programmerdan.minecraft.devotion.commands.AbstractCommand;

/**
 * C&C for the administrative (non-automatic) watch interface.
 * 
 * @author ProgrammerDan
 *
 */
public final class Watch extends AbstractCommand {

	public Watch(Devotion instance, String commandName) {
		super(instance, commandName);
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		if (args.size() < 1) {
			sender.sendMessage("Listing all active monitors and who they watch: ");
			Devotion.instance().getMonitors().forEach(m -> {
				if (!m.isEnabled()) { return; }
				StringBuffer sb = new StringBuffer(m.getName());
				sb.append(": \n");
				m.listWatch().forEach(p -> sb.append("  ").append(p.toString()).append("\n"));
				sender.sendMessage(sb.toString());
			});
						
			return true;
		}
		String checkWatch = args.get(0);
		UUID player = plugin.getUUIDResolver().getPlayer(checkWatch);
		if (player == null) {
			sender.sendMessage(ChatColor.RED + "Unable to find " + ChatColor.AQUA + checkWatch);
			return true;
		}
		
		WatchEventType type = WatchEventType.TOGGLE;
		if (args.size() > 1 && args.get(1).equalsIgnoreCase("add")) {
			type = WatchEventType.ADD;
		} else if (args.size() > 1 && args.get(1).equalsIgnoreCase("remove")) {
			type = WatchEventType.REMOVE;
		}
		
		WatchEvent event = new WatchEvent(type, player);
		Bukkit.getPluginManager().callEvent(event);
		
		event.getMessages().forEach(m -> sender.sendMessage(m));
		
		return true;
	}

}
