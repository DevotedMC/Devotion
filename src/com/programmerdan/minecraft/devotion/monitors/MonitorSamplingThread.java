package com.programmerdan.minecraft.devotion.monitors;

import net.minecraft.server.v1_12_R1.MinecraftServer;

import org.bukkit.scheduler.BukkitRunnable;

import com.programmerdan.minecraft.devotion.Devotion;

/**
 * Separate Runnable that simply pings the host monitor to wait up and take some samples from
 * time to time.
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 *
 */
public class MonitorSamplingThread extends BukkitRunnable {

	private Monitor monitor;
	
	private long targetDelay;
	private boolean adaptive;
	
	/**
	 * Create a new Sampling thread with the target monitor.
	 * @param monitor
	 */
	public MonitorSamplingThread(Monitor monitor) {
		this.monitor = monitor;
		targetDelay = 100l;
		adaptive = false;
	}
	
	/**
	 * Allows to set or reset the current running mode of this sampling trigger thread to periodic.
	 * @param targetDelay server Ticks between invocations.
	 */
	public void startPeriodic(long targetDelay) {
		this.targetDelay = targetDelay;
		if (adaptive) {
			this.cancel();
		}
		adaptive = false;
		if (monitor.isEnabled()) {
			activate();
		}
	}
	
	/**
	 * Allows to set or reset the current running mode of this sampling trigger thread to adaptive.
	 * @param targetDelay Milliseconds between invocations.
	 */
	public void startAdaptive(long targetDelay) {
		this.targetDelay = targetDelay;
		
		if (!adaptive) {
			this.cancel();
		}
		adaptive = true;
		if (monitor.isEnabled()) {
			activate();
		}
	}
	
	/**
	 * Used internally to actually kick ass the task.
	 */
	private void activate() {
		if (adaptive) {
			this.runTaskLaterAsynchronously(Devotion.instance(), convertToTicks(this.targetDelay) ); // convert milliseconds into ticks
		} else{
			this.runTaskTimerAsynchronously(Devotion.instance(), targetDelay, targetDelay);
		}
	}
	
	@Override
	public void run() {
		monitor.doSample();
		
		if (!monitor.isEnabled()) {
			this.cancel();
			return;
		}
		if (adaptive) {
			this.runTaskLaterAsynchronously(Devotion.instance(), convertToTicks(this.targetDelay) ); // convert milliseconds into ticks
		}
	}

	/**
	 * Helper that uses the minecraft internal tps handler to convert an expected delay in milliseconds into 
	 * an actual delay in ticks.
	 * 
	 * TODO make a self-adapting superclass ontop of BukkitRunnable?
	 */
	protected final long convertToTicks(long milliseconds) {
		double curtick = 20.0;
		try {
			curtick = MinecraftServer.getServer().recentTps[0];
		} catch (NullPointerException npe) {}
		return (long) ( (double) milliseconds / (1000.0 / curtick));
	}
}
