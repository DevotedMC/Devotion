package com.programmerdan.minecraft.devotion.util;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.programmerdan.minecraft.devotion.Devotion;

import vg.civcraft.mc.namelayer.NameAPI;

public class NameAPIUUIDResolver extends UUIDResolver {

	@Override
	public UUID getPlayer(String player) {
		UUID playerId = null;
		if (player.length() <= 16) {
			try {
				playerId = NameAPI.getUUID(player);
				
				if (playerId == null) {
					Player match = Bukkit.getPlayer(player);
					if (match != null) {
						playerId = match.getUniqueId();
					}
				}
			} catch (Exception ee) {
				Devotion.logger().log(Level.WARNING, ChatColor.RED + "Unable to find player " + ChatColor.DARK_RED + player);
			}
		} else if (player.length() == 36) {
			try {
				playerId = UUID.fromString(player);
			} catch (IllegalArgumentException iae) {
				Devotion.logger().log(Level.WARNING, ChatColor.RED + "Unable to process uuid " + ChatColor.DARK_RED + player);
			}
		} else {
			Devotion.logger().log(Level.WARNING, ChatColor.RED + "Unable to interpret " + ChatColor.DARK_RED + player);
		}
		return playerId;
	}
}
