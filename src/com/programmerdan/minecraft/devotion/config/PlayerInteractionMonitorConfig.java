package com.programmerdan.minecraft.devotion.config;

import java.util.Set;

import com.programmerdan.minecraft.devotion.monitors.PlayerInteractionType;

/**
 * Wrapper class for Interaction monitoring.
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 *
 */
public class PlayerInteractionMonitorConfig {
	public long delayBetweenSamples;
	
	public Set<PlayerInteractionType> active;
}
