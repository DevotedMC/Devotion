package com.programmerdan.minecraft.devotion.config;

import com.programmerdan.minecraft.devotion.monitors.SamplingMethod;

/**
 * Wrapper class for Inventory monitoring.
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 *
 */
public class PlayerInventoryMonitorConfig {
	public boolean samplingEnabled;
	public long samplingDelay;
	public SamplingMethod samplingTechnique;
	public boolean sampleVehicleInventory;

	public boolean onPlayerOpenEnabled;
	public long onPlayerOpenTimeout;
	
	public boolean onContainerOpenEnabled;
	public long onContainerOpenTimeout; // recommend off

	public boolean onVehicleOpenEnabled;
	public long onVehicleOpenTimeout; // recommend off
}


