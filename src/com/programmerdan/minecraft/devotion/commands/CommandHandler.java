package com.programmerdan.minecraft.devotion.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.commands.command.Config;
import com.programmerdan.minecraft.devotion.commands.command.Control;
import com.programmerdan.minecraft.devotion.commands.command.Help;
import com.programmerdan.minecraft.devotion.commands.command.Stats;
import com.programmerdan.minecraft.devotion.commands.command.Watch;

public class CommandHandler implements CommandExecutor {

	final Devotion plugin;

	HashMap<String, AbstractCommand> commands = new HashMap<String, AbstractCommand>();

	public CommandHandler(Devotion instance) {
		plugin = instance;

		plugin.getCommand("devotion").setExecutor(this);

		registerCommands(new AbstractCommand[] {
					new Stats(instance, "stats"),
					new Control(instance, "control"),
					new Watch(instance, "watch"),
					new Help(instance, "help"),
					new Config(instance, "config")
				});
	}

	private void registerCommands(AbstractCommand[] abstractCommands) {

		for (AbstractCommand abstractCommand : abstractCommands) {
			commands.put(abstractCommand.getName(), abstractCommand);
			List<String> aliases = abstractCommand.getAliases();
			if (abstractCommand.getAliases() != null) {
				for (String alias : aliases) {
					commands.put(alias, abstractCommand);
				}
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		if (args.length == 0 || !commands.containsKey(args[0].toLowerCase()))
			return false;

		AbstractCommand abstractCommand = commands.get(args[0].toLowerCase());

		if (abstractCommand.getPermission() != null
				&& !sender.hasPermission(abstractCommand.getPermission())) {
			sender.sendMessage("You don't have the permission to use this command!");
			return true;
		}

		if (abstractCommand.onCommand(sender,
				Arrays.asList(args).subList(1, args.length)) == false
				&& abstractCommand.getUsage() != null) {
			sender.sendMessage("The correct usage is"
					+ abstractCommand.getUsage());
		}

		return true;
	}

	public static String readableTimeSpan(int t) {
		String b = "";
		int s = t % 60;
		t /= 60;
		int m = t % 60;
		t /= 60;
		int h = t % 24;
		t /= 24;
		int d = t;
		if (d != 0)
			b += d + "d ";
		if (d != 0 || h != 0)
			b += h + "h ";
		if (d != 0 || h != 0 || m != 0)
			b += m + "m ";
		b += s + "s ";
		return b;
	}
}
