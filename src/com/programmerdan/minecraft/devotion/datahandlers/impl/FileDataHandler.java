package com.programmerdan.minecraft.devotion.datahandlers.impl;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import com.programmerdan.minecraft.devotion.Devotion;
import com.programmerdan.minecraft.devotion.dao.Flyweight;
import com.programmerdan.minecraft.devotion.datahandlers.DataHandler;
import com.programmerdan.minecraft.devotion.util.FlowHelper;

/**
 * Generic handler for all Flyweight types that properly extend {@link Flyweight}.
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 */
public class FileDataHandler extends DataHandler {

	private FlowHelper statistics;
	private File baseFolder;
	private long maxFileSize;
	private int maxIORate;
	private int ioChunkSize;

	private long lastSampleTime;
	private String lastFileName;
	private long lastFileSize;
	private DataOutputStream activeStream;

	public FlowHelper getStatistics() {
		return statistics;
	}
	
	private FileDataHandler() {
		super("file");
	}

	private String generateFileName() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss_SSSS");
		return sdf.format(Calendar.getInstance().getTime()) + ".dat";
	}
	
	/**
	 * Handles stream setup. There is a bit of cost involved to tear down/bring up a new file, so
	 * be careful in balancing your IO ops.
	 *
	 * TODO: Evaluate using explicit NIO channels instead of DOS. could be huge performance gains...
	 * 
	 * @return a valid DataOutputStream of null.
	 */
	private DataOutputStream getStream() {
		if (activeStream != null && lastFileSize > maxFileSize) {
			try {
				if (activeStream != null)
					activeStream.close();
				activeStream = null;
			} catch (IOException ioe) {
				Devotion.logger().warning("Failed to close prior file: " + this.lastFileName);
			}
		}
		if (lastFileSize > maxFileSize || activeStream == null) {
			this.lastFileName = generateFileName();
						
			File target = new File(this.baseFolder.getAbsoluteFile(), this.lastFileName);
			
			Devotion.logger().info("Generating new streamfile: " + target.getAbsolutePath());
			
			try {
				target.createNewFile();
				lastFileSize = 0l;

				activeStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target), ioChunkSize));
			} catch(FileNotFoundException fnfe) {
				Devotion.logger().log(Level.SEVERE, "File not found to open: " + target.toString(), fnfe);
			} catch(IOException ioe) {
				Devotion.logger().log(Level.SEVERE, "Failed to open stream: " + target.toString(), ioe);
			}
		}
		return activeStream;
	}
	
	private void releaseStream() {
		try {
			if (activeStream != null) {
				activeStream.close();
			}
		} catch (IOException ioe) {
			Devotion.logger().warning("Failed to close prior file on shutdown: " + lastFileName);
		}
	}

	/**
	 * Generates a new FileDataHandler from a config section.
	 * 
	 * @param config the config to use
	 * @return a new FileDataHandler, or null if setup failed.
	 */
	public static FileDataHandler generate(ConfigurationSection config) {
		if (config == null) {
			Devotion.logger().log(Level.SEVERE,
					"Null configuration passed; FileDataHandler not created");
			return null;
		}

		FileDataHandler fdh = new FileDataHandler();
		String baseFolder = config.getString("base");
		if (baseFolder == null) {
			Devotion.logger().log(Level.INFO,
					"Using plugin default data folder");
			fdh.baseFolder = Devotion.instance().getDataFolder();
		} else {
			fdh.baseFolder = new File(baseFolder);
		}
		
		fdh.maxFileSize = config.getLong("max_file_size", 8388608l);
		fdh.maxIORate = config.getInt("max_io_rate", Integer.MAX_VALUE);
		fdh.ioChunkSize = config.getInt("io_chunk_size", 2048);
		fdh.lastFileSize = 0l;
		fdh.lastFileName = null;

		if (fdh.maxFileSize <= 0 || fdh.maxIORate <= 0 || fdh.ioChunkSize <= 0) {
			Devotion.logger().log(Level.SEVERE,
					"Improper settings for FileDataHandler");
			return null;
		}

		try {
			if (!fdh.baseFolder.isDirectory() && !fdh.baseFolder.mkdirs()) {
				Devotion.logger().log( Level.SEVERE, "FileDataHandler base folder can't be created: "
								+ fdh.baseFolder.getPath());
				return null;
			} else {
				Devotion.logger().log( Level.INFO, "FileDataHandler base folder was found or created: "
						+ fdh.baseFolder.getAbsolutePath());
			}
		} catch (SecurityException se) {
			Devotion.logger().log(Level.SEVERE,
					"Failed to set up FileDataHandler", se);
			return null;
		}

		fdh.setup(config.getLong("delay", 10l), config.getLong("max_run", 250l), false, config.getBoolean("debug"));
		
		// Target is sufficient samples for min 60 seconds of windowed average.
		int samples = Math.max(10, (fdh.getDelay() < 1 ? 10 : 1200 / (int) fdh.getDelay()) );
		fdh.statistics = new FlowHelper(samples);
		
		// prepare stream
		fdh.getStream();

		return fdh;
	}

	@Override
	public void teardown() {
		this.releaseStream();
	}

	/**
	 * This override just forces the initialization of the file resource, ensuring it is ready for
	 * writes as they accumulate.
	 */
	@Override
	public void buildUp() {
		this.lastSampleTime = System.currentTimeMillis();
		this.getStream();
	}

	/**
	 * Simple process, generalized for all serializable Flyweights.
	 * 
	 *  - Gets the datastream {@link getStream()}
	 *  - Gets a Flyweight from the asynch FIFO queue {@link insertQueue}
	 *  - Writes the flyweight {@link Flyweight.serialize(DataOutputStream)}
	 *  - Continues until:
	 *    - Nothing left in the queue
	 *    - Time exceeded for write operations
	 *    - Bytes exceed limit per cycle
	 *  
	 *  Using atomic statistics handler. TODO: self-configure based on results.
	 */
	@Override
	protected void process() {
		debug(Level.INFO, "FileDataHandler: Starting commit...");
		long in = System.currentTimeMillis();
		long records = 0l;
		
		int writeSoFar = 0;

		if (!isQueueEmpty()) {
			DataOutputStream bos = getStream();
			
			if (bos != null) {
	
				while (System.currentTimeMillis() < in + this.getMaxRun()
						&& !isQueueEmpty() && writeSoFar <= this.maxIORate) {
					
					Flyweight toWrite = pollFromQueue();
		
					toWrite.serialize(bos);
					
					writeSoFar += toWrite.getLastWriteSize();
					
					this.lastFileSize += toWrite.getLastWriteSize();
					
					records++;
				}
				
				try {
					bos.flush();
				} catch (IOException e) {
					debug(Level.SEVERE, "FileDataHandler: Unable to flush output stream.", e);
				}
	
			} else {
				debug(Level.SEVERE, "FileDataHandler: Data stream is null, cannot commit. Skipping for now.");
			}
		} else {
			debug(Level.INFO, "FileDataHandler: Event queue is empty, nothing to commit.");
		}
		
		long sTime = System.currentTimeMillis();
		statistics.sample(sTime - this.lastSampleTime, super.getAndClearInsertCount(), records);
		
		in = sTime - in;
		this.lastSampleTime = sTime;
		debug(Level.INFO, "FileDataHandler: Done commit {0} records ({1} bytes) in {2} milliseconds",
				records, writeSoFar, in);
		
		debug(Level.INFO, "FileDataHandler: Inflow {0} -- Outflow {1} over {2} ms", statistics.totalInflow(),
				statistics.totalOutflow(), statistics.totalSampleTime());
	}
}
