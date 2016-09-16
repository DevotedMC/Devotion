package com.programmerdan.minecraft.devotion.commands.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.commands.AbstractCommand;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.datahandlers.impl.DatabaseDataHandler;
import com.programmerdan.minecraft.devotion.datahandlers.impl.FileDataHandler;
import com.programmerdan.minecraft.devotion.monitors.Monitor;
import com.programmerdan.minecraft.devotion.monitors.impl.PlayerInteractionMonitor;
import com.programmerdan.minecraft.devotion.monitors.impl.PlayerMovementMonitor;

/**
 * Ultimately will allow deeper inspection of Devotion while running. For now just lists monitors and
 * active status.
 * 
 * @author ProgrammerDan
 *
 */
public final class Stats extends AbstractCommand {

	public Stats(Devotion instance, String commandName) {
		super(instance, commandName);
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		StringBuffer sb = new StringBuffer();
		if (args.size() >= 1) {
			for (Monitor m : this.plugin.getMonitors()) {
				if (m.getName().equals(args.get(0))) {
					if (m instanceof PlayerInteractionMonitor) {
						PlayerInteractionMonitor pim = (PlayerInteractionMonitor) m;
						sb.append("PlayerInteractionMonitor:\n")
							.append("\tDebug: ").append(pim.isDebug()).append("\n")
							.append("\tActive: ").append(pim.isEnabled()).append("\n");
						// TODO: stats from monitor
						break;
					}
					if (m instanceof PlayerMovementMonitor) {
						PlayerMovementMonitor pmm = (PlayerMovementMonitor) m;
						sb.append("PlayerMovementMonitor:\n")
							.append("\tDebug: ").append(pmm.isDebug()).append("\n")
							.append("\tActive: ").append(pmm.isEnabled()).append("\n");
						// TODO: stats from monitor
						break;
					}
				}
			}
			
			for (DataHandler d : this.plugin.getHandlers()) {
				if (d.getName().equals(args.get(0))) {
					if (d instanceof DatabaseDataHandler) {
						DatabaseDataHandler ddh = (DatabaseDataHandler) d;
						sb.append("DatabaseDataHandler:\n")
							.append("\tDebug: ").append(ddh.isDebug()).append("\n")
							.append("\tActive: ").append(ddh.isActive()).append("\n");
						// TODO: stats from data handler
						break;
					}
					if (d instanceof FileDataHandler) {
						FileDataHandler fdh = (FileDataHandler) d;
						sb.append("FileDataHandler:\n")
							.append("\tDebug: ").append(fdh.isDebug()).append("\n")
							.append("\tActive: ").append(fdh.isActive()).append("\n");
						// TODO: stats from monitor
						break;
					}
				}
			}
			if (sb.length() == 0) {
				sender.sendMessage("Named monitor or handler not found.");
			}
		} else {
			sb.append("Current Monitors:\n");
			for (Monitor m : this.plugin.getMonitors()) {
				sb.append("\t").append(m.getName()).append(" - ")
						.append(m.isEnabled() ? "on" : "off").append("\n");
			}

			sb.append("Current Handlers:\n");
			for (DataHandler m : this.plugin.getHandlers()) {
				sb.append("\t").append(m.getName()).append(" - ")
						.append(m.isActive() ? "on" : "off").append("\n");
			}
		}
		if (sb.length() > 0) {
			sender.sendMessage(sb.toString());
			return true;
		}
		return false;
	}

}
