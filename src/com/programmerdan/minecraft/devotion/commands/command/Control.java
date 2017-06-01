package com.programmerdan.minecraft.devotion.commands.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.commands.AbstractCommand;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.monitors.Monitor;

public final class Control extends AbstractCommand {

	public Control(Devotion instance, String commandName) {
		super(instance, commandName);
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		StringBuffer sb = new StringBuffer();
		if (args.size() >= 2) {
			String modes = args.get(1);
			Boolean mode = "on".equals(modes) ? Boolean.TRUE : ("off".equals(modes) ? Boolean.FALSE : null);
			if (mode == null) {
				sender.sendMessage("Mode must be on or off, use /dev-s to see current mode");
			} else {
				for (Monitor m : this.plugin.getMonitors()) {
					if (m.getName().equals(args.get(0))) {
						if (!mode) {
							m.onDisable();
							sb.append("Disabling Monitor " + m.getName());
						} else {
							m.onEnable();
							sb.append("Enabling Monitor " + m.getName());
						}
					}
				}
				
				for (DataHandler d : this.plugin.getHandlers()) {
					if (d.getName().equals(args.get(0))) {
						if (!mode) {
							d.teardown();
							sb.append("Disabling Handler " + d.getName());
						} else {
							d.begin();
							sb.append("Enabling Handler " + d.getName());
						}
					}
				}
				if (sb.length() == 0) {
					sender.sendMessage("Named monitor or handler not found.");
				} else {
					sb.append("\nDisclaimer: things might break.");
				}
			}
		}
		if (sb.length() > 0) {
			sender.sendMessage(sb.toString());
			return true;
		}
		return false;
	}

}
