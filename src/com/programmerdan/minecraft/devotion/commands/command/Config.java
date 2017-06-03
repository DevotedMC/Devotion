package com.programmerdan.minecraft.devotion.commands.command;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.commands.AbstractCommand;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.monitors.Monitor;

public class Config extends AbstractCommand {

	public Config(Devotion instance, String commandName) {
		super(instance, commandName);
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		StringBuffer sb = new StringBuffer();
		if (args.size() >= 2) {
			String modes = args.size() > 2 ? args.get(2) : null;
			Boolean mode = ("on".equalsIgnoreCase(modes) || "true".equalsIgnoreCase(modes)) ? Boolean.TRUE :
						(("off".equalsIgnoreCase(modes) || "false".equalsIgnoreCase(modes)) ? Boolean.FALSE : null);
			
			Devotion.instance().reloadConfig();
			for (Monitor m : this.plugin.getMonitors()) {
				if (m.getName().equals(args.get(0))) {
					if (m.setConfig(args.get(1), mode == null ? modes : mode)) {
						sb.append("Succeeded in setting Monitor " + m.getName() + " config " + args.get(1) + " to ").append(modes);
					} else {
						sb.append("Failed to set Monitor " + m.getName() + " config " + args.get(1) + " to ").append(modes);
					}
				}
			}
			
			for (DataHandler d : this.plugin.getHandlers()) {
				if (d.getName().equals(args.get(0))) {
					if ("debug".equalsIgnoreCase(args.get(1))) {
						Devotion.instance().reloadConfig(); // get changes first
						Devotion.instance().getConfig().set("dao." + d.getName() + ".debug", mode == null ? false : mode.booleanValue());
						d.setDebug(mode == false ? false : mode.booleanValue());
						Devotion.instance().saveConfig();
						sb.append("Succeeded in setting Datasource " + d.getName() + " config " + args.get(1) + " to ").append(mode == null ? false : mode.booleanValue());
					} else {
						sb.append("Failed to set Datasource " + d.getName() + " config " + args.get(1) + " to ").append(mode == null ? false : mode.booleanValue());
					}
				}
			}

			if (sb.length() == 0) {
				sender.sendMessage("Named monitor or handler not found.");
			} else {
				sb.append("\nDisclaimer: things might break.");
				Devotion.instance().saveConfig();
			}
		} else if (args.size() == 1) {
			for (Monitor m : this.plugin.getMonitors()) {
				if (m.getName().toLowerCase().contains(args.get(0).toLowerCase())) {
					sb.append("Found potential matching Monitor: " + m.getName() + "\n Has configurable: \n");
					sb.append(m.getConfigs());
				}
			}
			
			for (DataHandler d : this.plugin.getHandlers()) {
				if (d.getName().toLowerCase().contains(args.get(0).toLowerCase())) {
					sb.append("Found potential matching Datasource: " + d.getName() + "\n Has configurable: \n");
					sb.append(" debug - on / off \n");
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
