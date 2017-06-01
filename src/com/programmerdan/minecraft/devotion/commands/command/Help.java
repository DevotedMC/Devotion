package com.programmerdan.minecraft.devotion.commands.command;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.commands.AbstractCommand;

public class Help extends AbstractCommand {

	public Help(Devotion instance, String commandName) {
		super(instance, commandName);
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		sender.sendMessage(ChatColor.WHITE + "Commands for Devotion:");
		sender.sendMessage(ChatColor.GRAY + "-Statistics and Config");
		sender.sendMessage(ChatColor.AQUA + " /devotion stats" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Shows active monitors and data handlers");
		sender.sendMessage(ChatColor.AQUA + " /devotion stats " + ChatColor.BLUE + "<monitor>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Shows stats or configuration for the named monitor");
		sender.sendMessage(ChatColor.AQUA + " /devotion stats " + ChatColor.BLUE + "<handler>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Shows stats or configuration for the named handler");
		sender.sendMessage(ChatColor.AQUA + " /devotion control " + ChatColor.BLUE + "<monitor>" + 
				ChatColor.AQUA + "[" + ChatColor.GREEN + "on" + ChatColor.AQUA + "|" + ChatColor.RED + "off" + ChatColor.AQUA + "]" + 
				ChatColor.GRAY + " - " + ChatColor.WHITE + "Turns on or off the named monitor");
		sender.sendMessage(ChatColor.AQUA + " /devotion control " + ChatColor.BLUE + "<handler>" + 
				ChatColor.AQUA + "[" + ChatColor.GREEN + "on" + ChatColor.AQUA + "|" + ChatColor.RED + "off" + ChatColor.AQUA + "]" + 
				ChatColor.GRAY + " - " + ChatColor.WHITE + "Turns on or off the named handler");
		sender.sendMessage(ChatColor.GRAY + "-Data Collection");
		sender.sendMessage(ChatColor.AQUA + " /devotion watch " + ChatColor.GRAY + " - " + ChatColor.WHITE + "View watching status across all monitors.");
		sender.sendMessage(ChatColor.AQUA + " /devotion watch " + ChatColor.BLUE + "<uuid/name>" + ChatColor.GRAY + " - " + ChatColor.WHITE + "Toggle watching status for named player across all monitors.");
		
		return true;
	}

}
