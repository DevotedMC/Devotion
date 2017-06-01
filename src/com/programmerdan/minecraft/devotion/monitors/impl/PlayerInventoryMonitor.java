package com.programmerdan.minecraft.devotion.monitors.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.config.PlayerInventoryMonitorConfig;
import com.programmerdan.minecraft.devotion.monitors.Monitor;

public class PlayerInventoryMonitor extends Monitor implements Listener {

	private PlayerInventoryMonitorConfig config;
	
	/**
	 * Records the last time a datapoint was captured
	 */
	private ConcurrentHashMap<UUID, long[]> lastCapture;
	
	private boolean checkInsert(UUID player, PlayerInventoryType pit) {
		if (!checkDelay) {
			return true;
		} else {
			long[] captureTimes = lastCapture.get(player);
			if (captureTimes == null) {
				captureTimes = new long[PlayerInventoryType.SIZE];
				lastCapture.put(player, captureTimes);
			}
			long now = System.currentTimeMillis();
			boolean res = (now - captureTimes[pit.getIdx()]) > config.samplingDelay;
			captureTimes[pit.getIdx()] = now;
			return res;
		}
	}
	
	private boolean checkDelay = true; 
	
	protected PlayerInventoryMonitorConfig getConfig() {
		return config;
	}
	
	private PlayerInventoryMonitor(PlayerInventoryMonitorConfig config) {
		super("PlayerInventoryMonitor");
		this.config = config;
	}
	
	public static PlayerInventoryMonitor generate(ConfigurationSection config) {
		if (config == null) return null;
		PlayerInventoryMonitorConfig pimc = new PlayerInventoryMonitorConfig();
		pimc.samplingDelay = config.getLong("sampling_delay", 10l);
		pimc.samplingEnabled = config.getBoolean("sampling_enabled", false);
		PlayerInventoryMonitor pim = new PlayerInventoryMonitor(pimc);
		pim.setDebug(config.getBoolean("debug", Devotion.instance().isDebug()));
		pim.initWatch(config);
		return pim;
	}
	
	@Override
	public void onEnable() {
		if (super.isEnabled()) {
			return;
		}
		super.setEnabled(false);
	}
	
	public static enum PlayerInventoryType {
		Player(0),
		Container(1),
		Vehicle(2);
		
		public static final int MAX_IDX = 2; // UPDATE THIS if you alter the above
		public static final int SIZE = 3; // UPDATE THIS if you alter the above.
		
		private int idx;
		
		PlayerInventoryType(int idx) {
			this.idx = idx;
		}
		
		public int getIdx() {
			return idx;
		}
	}

	@Override
	public void onDisable() {
		if (!super.isEnabled()) {
			return;
		}

		super.setEnabled(false);
		
		super.commitWatch();
		
	}

	@Override
	protected void doSample() {
		// TODO Auto-generated method stub
		
	}
}
