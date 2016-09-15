package com.programmerdan.minecraft.devotion.siphon;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SiphonWorker implements Callable<Boolean> {

	private Siphon siphon;
	private SiphonDatabase database;
	private int slices;
	private long sliceLength;
	private long fuzz;
	private long minBuffer;
	
	/*
	 * Statistics.
	 */
	private long timeSpentExporting = 0l;
	private long recordsExported = 0l;
	private long timeSpentInvoking = 0l;
	private long timeSpentCleaning = 0l;
	private long recordsCleaned = 0l;
	private long timeTotal = 0l;

	private static final String[] TABLES = new String[] {
		"dev_block_break",
		"dev_block_place",
		"dev_drop_item",
		"dev_player_bed",
		"dev_player_bucket",
		"dev_player_death",
		"dev_player_drop_item",
		"dev_player_edit_book",
		"dev_player_egg_throw",
		"dev_player_exp_change",
		"dev_player_fish",
		"dev_player_game_mode_change",
		"dev_player_interact",
		"dev_player_interact_entity",
		"dev_player_item_break",
		"dev_player_item_consume",
		"dev_player_item_held",
		"dev_player_kick",
		"dev_player_level_change",
		"dev_player_login",
		"dev_player_pickup_item",
		"dev_player_quit",
		"dev_player_resource_pack_status",
		"dev_player_respawn",
		"dev_player_shear_entity",
		"dev_player_statistic_increment",
		"dev_player_teleport",
		"dev_player_toggle",
		"dev_player_velocity",
		"dev_player"
	};

	private static final HashSet<String> SPECIAL_TABLES = new HashSet<String>( Arrays.asList( "dev_player" ) );
	
	/**
	 * SiphonWorker is the root controller for sucking down slices of data in a way that hopefully
	 * doesn't lock up the database.
	 *
	 * @param siphon The Siphon base object / controller.
	 * @param database The database primitive (used to spawn connections)
	 * @param slices The number of slices to split each day up into. This is used to determine the size and
	 *        affinity of slice operations.
	 * @param fuzz The number of milliseconds "off" of precise we can tolerate when slicing up the data.
	 *        Using 0 is strongly discouraged as its meaningless. Values of 500-1000 make the most sense.
	 * @param minBuffer The # of records at a minimum to leave in the database; only capture slices below
	 *        this buffer value. Useful for keeping a day or two of data in the database but
	 *        constrained by number so if you have a hugely busy day your database doesn't explode.
	 */
	public SiphonWorker(Siphon siphon, SiphonDatabase database, int slices, long fuzz, long minBuffer) {
		this.siphon = siphon;
		this.database = database;
		this.slices = slices;
		this.sliceLength = 86400000l / this.slices;
		this.fuzz = fuzz;
		this.minBuffer = minBuffer;
	}
	
	@Override
	public Boolean call() {
		// We can't be sure of uninterrupted transaction.
		// attempt to create a persistent temporary table (CREATE IF NOT EXISTS) for transaction identifiers
		// if it's not empty, assume we are resuming a prior transaction.
		// If it is empty, bound the new transaction. Find oldest data, figure out time of day for it
		//     and "fit" into slices as informed.
		// Select into the temptable the data that fits into the next slice.
		// 
		// Then use an ExecutorService to farm out all the work of spitting out data to file.
		// consider using a fixedsize executor .
		// 
		// temp table
		// index
		// export
		// remove
		/*Statistics*/ timeTotal = System.currentTimeMillis();
		SiphonConnection connect = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss-SSSS");
			connect = this.database.connect();
			long sliceID = -1l;
			long sliceTime = -1l;

			// Our first check is to see if a prior transaction was left incomplete.
			boolean resume = connect.checkTableExists(SiphonConnection.TRANS_TABLE);
			if (resume) {
				System.out.println("Resuming a prior slice export");
				// we're going to resume a presumably aborted process.
				PreparedStatement retrieveStop = connect.prepareStatement(SiphonConnection.TRANS_SELECT);
				ResultSet retrieveRS = retrieveStop.executeQuery();
				if (retrieveRS.next()) {
					sliceID = retrieveRS.getLong(1);
					Timestamp sliceTimestamp = retrieveRS.getTimestamp(2);
					if (sliceTimestamp != null) {
						sliceTime = sliceTimestamp.getTime();
					}
				}
				retrieveRS.close();
				retrieveStop.close();
			}

			// BEGIN TRANSACTION
			
			if (sliceID < 0) { // resume failed or doesn't exist.
				System.out.println("Starting a new slice export transaction");
				PreparedStatement transRemove = connect.prepareStatement(SiphonConnection.TRANS_REMOVE);
				transRemove.execute();
				transRemove.close();

				long bottomID = -1L;
				long upperID = -1L;
				
				PreparedStatement bounds = connect.prepareStatement(SiphonConnection.BOUNDS);
				ResultSet boundsRS = bounds.executeQuery();
				if (boundsRS.next()) {
					bottomID = boundsRS.getLong(1);
					upperID = boundsRS.getLong(2);
				}
				boundsRS.close();
				bounds.close();

				if (bottomID > -1 && upperID > -1) {					
					long maxID = upperID - this.minBuffer; // this is our actual cap.
					if (bottomID > maxID) {// nothing to do
						System.err.println("Nothing to siphon!");
						return Boolean.TRUE;
					}
					long baseID = bottomID;
					long baseTime = getTime(baseID, connect);
					long bottomTime = baseTime;
					
					// now compute target time as the next-closest subdivision based on slices.
					Calendar baseDay = Calendar.getInstance();
					baseDay.setTime(new Date(baseTime));
					baseDay.set(Calendar.HOUR_OF_DAY, 0);
					baseDay.set(Calendar.MINUTE, 0);
					baseDay.set(Calendar.SECOND, 0);
					baseDay.set(Calendar.MILLISECOND, 0);
					// target time is the next slice within the day. So day start milliseconds + adjusted target slice within the day based on time already transpired.
					long targetTime = baseDay.getTimeInMillis() + (((baseTime - baseDay.getTimeInMillis()) / this.sliceLength) + 1) * this.sliceLength; // roundToZero truncation gives low bound + 1 + sliceLength to give target endtime.

					long maxTime = getTime(maxID, connect);
					System.out.println("Starting targetTime w/ starting max time and ID : " + sdf.format(new Date(targetTime)) + 
							" max : " + sdf.format(new Date(maxTime)) + " [" + maxID + "]");
					if (maxTime > targetTime) {
						long currentID = (maxID + baseID) / 2;
						long currentTime = getTime(currentID, connect);
						int failsafe = 0;
						while (Math.abs(currentTime - targetTime) > this.fuzz && currentID < maxID && failsafe < 100) {
							if (currentTime > targetTime) {
								maxID = currentID - 1;
							} else { // currentTime < targetTime) {
								baseID = currentID + 1;
							} // == handled in loop terminator
							currentID = (maxID + baseID) / 2;
							currentTime = getTime(currentID, connect);
							failsafe ++;
						}
						
						if (failsafe < 100) {
							// found! use bottomTime and currentTime as bounds.
							System.out.println("Found time and ID span: " + sdf.format(new Date(bottomTime)) + " [" + bottomID + "] to " + 
									sdf.format(new Date(currentTime)) +  " [" + currentID + "] in " + failsafe + " steps.");
							sliceID = currentID;
							sliceTime = currentTime;
						} else {
							System.err.println("Very bad behavior, failed to find either near event or stable. Are you sure data is properly ordered?");
						}
					} else {
						System.err.println("Nothing to siphon, current max time is too soon.");
					}
				} else {
					System.err.println("Nothing to siphon, no records returned from min-max query");
				}
			} 

			// Ok, we've either got a slice ID or we call it quits.
			if (sliceID > -1) {
				// Try to set up resume data point
				PreparedStatement transBegin = connect.prepareStatement(SiphonConnection.TRANS_CREATE);
				transBegin.setLong(1, sliceID);
				int captured = transBegin.executeUpdate();
				if (captured < 1) {
					System.err.println("WARNING: Resume point not created.");
				} else {
					System.out.println("Resume point created pointing at ID " + sliceID );
				}
				transBegin.close();
				
				// We will use a new connection in the task.
				connect.close();

				// CREATE SLICE REFERENCE ID TABLE
				
				// now spawn some Callables w/ an Executor to do the work.
				final long targetSliceID = sliceID;
				final long targetSliceTime = sliceTime;
				// First, create the temporary ID table.
				ExecutorService doWork = Executors.newFixedThreadPool(siphon.getConcurrency());
				Future<Integer> prep = doWork.submit(new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						SiphonConnection connect = null;
						try {
							connect = database.connect();
							int captured = 0;
							boolean resume = connect.checkTableExists(SiphonConnection.SLICE_TABLE_NAME);
							if (resume) {
								System.out.println("Trying to resume an old slice export");
								// we're probably going to resume a presumably aborted process.
								PreparedStatement checkSize = connect.prepareStatement(SiphonConnection.SLICE_TABLE_SIZE);
								ResultSet checkRS = checkSize.executeQuery();
								if (checkRS.next()) {
									captured = checkRS.getInt(1);
									if (captured < 1) {
										// remove old one!
										PreparedStatement removeIndex = connect.prepareStatement(SiphonConnection.REMOVE_SLICE_INDEX);
										removeIndex.execute();
										removeIndex.close();
			
										PreparedStatement removeSliceTable = connect.prepareStatement(SiphonConnection.REMOVE_SLICE_TABLE);
										removeSliceTable.execute();
										removeSliceTable.close();
										resume = false;
									}
								} else {
									captured = 0;
									resume = false;
								}
								checkRS.close();
								checkSize.close();
							}
			
							if (!resume) {
								PreparedStatement createTable = connect.prepareStatement(SiphonConnection.GET_SLICE_TABLE);
								createTable.setLong(1, targetSliceID);
								captured = createTable.executeUpdate();
								System.out.println("Captured " + captured + " rows in slice table.");
								createTable.close();
							} else {
								System.out.println("Resuming a prior capture of " + captured + " rows");
							}
							
							PreparedStatement addIndex = connect.prepareStatement(SiphonConnection.ADD_SLICE_INDEX);
							addIndex.execute();
							addIndex.close();
							
							return captured;
						} catch (SQLException se) {
							se.printStackTrace();
							throw new Exception("Failure while attempting to set up slice export.", se);
						} finally {
							if (connect != null) connect.close();
						}
					}	
				});
				
				Integer outcome = null;
				long delaySeconds = 0l;
				long delayStart = System.currentTimeMillis();
				while (outcome == null) {
					try {
						outcome = prep.get(1, TimeUnit.SECONDS); //siphon.getCheckDelay(), TimeUnit.SECONDS);
					} catch (TimeoutException te) {
						outcome = null;
					} catch (InterruptedException e) {
						if (siphon.isDebug()) e.printStackTrace();
						outcome = null;
					} catch (ExecutionException e) {
						e.printStackTrace();
						outcome = null;
						break;
					} finally {
						delaySeconds ++; //= siphon.getCheckDelay();
						if (delaySeconds % siphon.getCheckDelay() == 0) {
							System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for table and index creation.");
						}
						if (prep.isDone()) {
							break;
						}
					}
				}
				System.out.println("Took ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) for table and index creation.");
				
				// now done, pile on the other executors...
				if (outcome == null) {
					System.err.println("We failed at the last -- could not create slice table.");
				} else {
					/* Statistics */ timeSpentExporting = System.currentTimeMillis();
					/* Statistics */ recordsExported = 0l;
					System.out.println("Launching tasks to export data");
					// EXPORT SLICE DATA TO FILE
					
					Map<Future<Integer>, Boolean> futures = new LinkedHashMap<Future<Integer>, Boolean>();
					final int maxGrab = outcome;
					final String targetSliceTimeString = sdf.format(new Date(targetSliceTime));
					for (final String table : TABLES) {
						Future<Integer> tableOutcome = doWork.submit(new Callable<Integer>() {
							@Override
							public Integer call() throws Exception {
								SiphonConnection connect = null;
								try {
									System.out.println(table + "] Preparing to collect and export");
									connect = database.connect();
									
									if (connect.checkTableExists(String.format(SiphonConnection.SLICE_DUMP_NAME, table))){
										PreparedStatement cleanSize = connect.prepareStatement(String.format(SiphonConnection.FILE_CLEANUP, table));
										if (siphon.isDebug()) {
											System.out.println(cleanSize);
										} else {
											System.out.println(table + "] Temp table previously existed, cleaning up");
										}
										cleanSize.execute();
									}
									
									PreparedStatement selectSize = connect.prepareStatement(String.format(SiphonConnection.FILE_SELECT, table));
									selectSize.setInt(1, maxGrab);
									if (siphon.isDebug()) {
										System.out.println(selectSize);
									} else {
										System.out.println(table + "] Getting records");
									}
									selectSize.execute();
									int size = selectSize.getUpdateCount();
									if (size > 0) {
										System.out.println(table + "] Staging " + size + " records");
									} else {
										System.out.println(table + "] No records found within the range");
									}
									SQLWarning warn = selectSize.getWarnings();
									while (warn != null) {
										System.out.println(table + "] [WARNING] " + warn.getLocalizedMessage());
										warn = warn.getNextWarning();
									}
									selectSize.close();
									
									if (size <= 0) {
										return size;
									}
									
									PreparedStatement addIndex = connect.prepareStatement(String.format(SiphonConnection.FILE_INDEX, table));
									addIndex.execute();
									addIndex.close();
									
									PreparedStatement shrink = connect.prepareStatement(String.format(SiphonConnection.FILE_SHRINK, table));
									shrink.execute();
									int nsize = shrink.getUpdateCount();
									if (nsize > 0) {
										System.out.println(table + "] Shrinking stage by " + nsize + " records");
									} else {
										System.out.println(table + "] Keeping all staged records");
									}
									warn = shrink.getWarnings();
									while (warn != null) {
										System.out.println(table + "] [WARNING] " + warn.getLocalizedMessage());
										warn = warn.getNextWarning();
									}
									shrink.close();

									PreparedStatement dumpSize = connect.prepareStatement(String.format(SiphonConnection.FILE_DUMP,
											table, targetSliceTimeString, siphon.getDatabaseTmpFolder()));
									if (siphon.isDebug()) {
										System.out.println(dumpSize);
									} else {
										System.out.println(table + "] Exporting records");
									}
									dumpSize.execute();
									size = dumpSize.getUpdateCount();
									if (size > 0) {
										System.out.println(table + "] Saved " + size + " records to tmp");
									} else {
										System.out.println(table + "] No records found within the range while saving to tmp");
									}
									warn = dumpSize.getWarnings();
									while (warn != null) {
										System.out.println(table + "] [WARNING] " + warn.getLocalizedMessage());
										warn = warn.getNextWarning();
									}
									dumpSize.close();

									return size;
								} catch (SQLException sqe) {
									throw new Exception(table + "] Error in query for while saving to tmp", sqe);
								} finally {
									if (connect != null) connect.close();
								}
							}
						});
						futures.put(tableOutcome, false);
					}
					
					// now we wait for completion.
					boolean allDone = false;
					boolean noErrors = false;
					delaySeconds = 0l;
					delayStart = System.currentTimeMillis();
					while (!allDone) {
						allDone = true;
						noErrors = true;
						for (Future<Integer> future : futures.keySet()) {
							if (!future.isDone()) {
								allDone = false;
								break;
							} else {
								if (!futures.get(future)) { // only print out error once.
									futures.put(future, true);
									try {
										/* Statistics */ recordsExported += 
											future.get();
									} catch(ExecutionException | InterruptedException ee) {
										noErrors = false;
										ee.printStackTrace();
									}
								}
							}
						}
						if (!allDone) {
							try {
								Thread.sleep(1000l); //siphon.getCheckDelay() * 1000l);
							} catch (InterruptedException e) {
								if (siphon.isDebug()) e.printStackTrace();
								outcome = null;
							} finally {
								delaySeconds ++; //= siphon.getCheckDelay();
								if (delaySeconds % siphon.getCheckDelay() == 0) {
									System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for table exports.");
								}
							}
						}
					}
					System.out.println("Took ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) to export slice.");
					/* Statistics */ timeSpentExporting = System.currentTimeMillis() - timeSpentExporting;
					
					boolean abort = false;
					if (!noErrors) {
						System.err.println(" Export completed with errors. Aborting.");
						abort = true;
					}
					
					// SYSTEM COMMANDS TO TARBALL AND MOVE TO BACKUP

					/* Statistics */ timeSpentInvoking = System.currentTimeMillis();
					
					if (!abort) {
						// Now run system commands to tarball it all
						try {
							ProcessBuilder accumBuilder = null;
							if (siphon.getWrapAccumulate()) {
								accumBuilder = new ProcessBuilder("su", "-c", 
									String.format("tar --remove-files -czvf %1$sdev_tracks_%2$s.tar.gz %1$sdev_*.dat", siphon.getTmpFolder(), targetSliceTimeString))
									.redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
							} else {
								accumBuilder = new ProcessBuilder("tar", "--remove-files", "-czvf", 
										String.format("%1$sdev_tracks_%2$s.tar.gz", siphon.getTmpFolder(), targetSliceTimeString),
										String.format("%1$sdev_*.dat", siphon.getTmpFolder()))
										.redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);								
							}
							if (siphon.isDebug()) System.out.println("Running command: " + accumBuilder.command());
							
							Process accumulate = accumBuilder.start();

							delaySeconds = 0l;
							delayStart = System.currentTimeMillis();
							boolean notDone = true;
							while (notDone) { // 1.8 !accumulate.waitFor(siphon.getCheckDelay(), TimeUnit.SECONDS)) {
								try {
									accumulate.exitValue();
									break;
								} catch (IllegalThreadStateException ise) {
									notDone = true;
								} catch (Exception e) {
									e.printStackTrace();
									notDone = false;
								}
								try {
									Thread.sleep(1000l); // siphon.getCheckDelay() * 1000l);
								} catch (InterruptedException ie) {
									if (siphon.isDebug()) ie.printStackTrace();
								}
								delaySeconds ++; //= siphon.getCheckDelay();
								if (delaySeconds % siphon.getCheckDelay() == 0) {
									System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for accumulation.");
								}
							}
							System.out.println("Done accumulation after ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) with exit value: " 
										+ accumulate.exitValue());
							if (accumulate.exitValue() > 0) { // by convention, failure.
								throw new ExecutionException("Accumulation failed", null);
							}
	
							ProcessBuilder moveBuilder = new ProcessBuilder("mv", 
									String.format("%1$sdev_tracks_%2$s.tar.gz", siphon.getTmpFolder(), targetSliceTimeString), 
									String.format("%1$s", siphon.getTargetFolder()))
									.redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
							
							if (siphon.isDebug()) System.out.println("Move command: " + moveBuilder.command());
							Process move = moveBuilder.start();
							delaySeconds = 0l;
							delayStart = System.currentTimeMillis();
							notDone = true;
							while (notDone) {//!move.waitFor(siphon.getCheckDelay(), TimeUnit.SECONDS)) {
								try {
									move.exitValue();
									break;
								} catch (IllegalThreadStateException ise) {
									notDone = true;
								} catch (Exception e) {
									e.printStackTrace();
									notDone = false;
								}
								try {
									Thread.sleep(1000l);//siphon.getCheckDelay() * 1000l);
								} catch (InterruptedException ie) {
									if (siphon.isDebug()) ie.printStackTrace();
								}
								
								delaySeconds ++; //= siphon.getCheckDelay();
								if (delaySeconds % siphon.getCheckDelay() == 0) {
									System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for move.");
								}
							}
							System.out.println("Done move after ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) with exit value: "
									+ move.exitValue());
							if (move.exitValue() > 0) { // by convention, failure.
								throw new ExecutionException("Move failed", null);
							}
						} catch (IOException | SecurityException | ExecutionException e) {
							e.printStackTrace();
							abort = true;
						}
						
						if (!abort) {
							try {
								ProcessBuilder chownBuilder = new ProcessBuilder("chown", 
										String.format("%1$s", siphon.getTargetOwner()),
										String.format("%1$sdev_tracks_%2$s.tar.gz", siphon.getTargetFolder(), targetSliceTimeString))
										.redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT);
								
								if (siphon.isDebug()) System.out.println("Chown command: " + chownBuilder.command());
								Process chown = chownBuilder.start();
								delaySeconds = 0l;
								delayStart = System.currentTimeMillis();
								boolean notDone = true;
								while (notDone) {//!chown.waitFor(siphon.getCheckDelay(), TimeUnit.SECONDS)) {
									try {
										chown.exitValue();
										break;
									} catch (IllegalThreadStateException ise) {
										notDone = true;
									} catch (Exception e) {
										e.printStackTrace();
										notDone = false;
									}
									try {
										Thread.sleep(1000l);//siphon.getCheckDelay() * 1000l);
									} catch (InterruptedException ie) {
										if (siphon.isDebug()) ie.printStackTrace();
									}
									delaySeconds ++;//= siphon.getCheckDelay();
									if (delaySeconds % siphon.getCheckDelay() == 0) {
										System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for chown.");
									}
								}
								System.out.println("Done chown after ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) with exit value: "
										+ chown.exitValue());
								if (chown.exitValue() > 0) { // by convention, failure.
									throw new ExecutionException("Chown failed", null);
								}
							} catch (IOException | SecurityException | ExecutionException e) {
								System.err.println("[WARNING] Chown Failed.");
								e.printStackTrace();
							}
						}
					} else {
						System.err.println("Aborting backup file create, move and chown.");
					}

					/* Statistics */ timeSpentInvoking = System.currentTimeMillis() - timeSpentInvoking;
					
					// REMOVE BACKED UP RECORDS FROM SOURCE TABLES

					/* Statistics */ timeSpentCleaning = System.currentTimeMillis();
					/* Statistics */ recordsCleaned = 0l;
					
					if (!abort) {
						// We haven't aborted, so clean up.
						// now done, pile on the other executors...
						futures = new LinkedHashMap<Future<Integer>, Boolean>();
						for (final String table : TABLES) {
							Future<Integer> tableOutcome = doWork.submit(new Callable<Integer>() {
								@Override
								public Integer call() throws Exception {
									SiphonConnection connect = null;
									try {
										System.out.println(table + "] Cleaning up table and associated support structures");
										connect = database.connect();
									
										String queryRemove = SiphonConnection.GENERAL_DELETE;
										if (SPECIAL_TABLES.contains(table)) {
											System.out.println(table + "] Using special no-gaps cleanup query");
											queryRemove = SiphonConnection.SPECIAL_DELETE;
										}
										PreparedStatement checkSize = connect.prepareStatement(
												String.format(queryRemove, table));
										checkSize.setInt(1, maxGrab);
										if (siphon.isDebug()) System.out.println(checkSize);
										int size = checkSize.executeUpdate();
										if (size > 0) {
											System.out.println(table + "] Deleted " + size + " records");
										} else {
											System.out.println(table + "] No records deleted within the range");
										}
										SQLWarning warn = checkSize.getWarnings();
										while (warn != null) {
											System.out.println(table + "] [WARNING] " + warn.getLocalizedMessage());
											warn = warn.getNextWarning();
										}
										checkSize.close();

										PreparedStatement removSize = connect.prepareStatement(String.format(SiphonConnection.FILE_REMOVE_INDEX, table));
										if (siphon.isDebug()) {
											System.out.println(removSize);
										} else {
											System.out.println(table + "] Cleaning up index");
										}
										removSize.execute();
										removSize.close();
										
										PreparedStatement cleanSize = connect.prepareStatement(String.format(SiphonConnection.FILE_CLEANUP, table));
										if (siphon.isDebug()) {
											System.out.println(cleanSize);
										} else {
											System.out.println(table + "] Cleaning up temp records");
										}
										cleanSize.execute();
										warn = cleanSize.getWarnings();
										while (warn != null) {
											System.out.println(table + "] [WARNING] " + warn.getLocalizedMessage());
											warn = warn.getNextWarning();
										}
										cleanSize.close();
										
										return size;
									} catch (SQLException sqe) {
										throw new Exception(table + "] Query failure in deletion request while cleaning up", sqe);
									} finally {
										if (connect != null) connect.close();
									}
								}
							});
							futures.put(tableOutcome, false);
						}
						// now we wait for completion.
						allDone = false;
						delaySeconds = 0l;
						delayStart = System.currentTimeMillis();
						while (!allDone) {
							allDone = true;
							noErrors = true;
							for (Future<Integer> future : futures.keySet()) {
								if (!future.isDone()) {
									allDone = false;
									break;
								} else {
									if (!futures.get(future)) { // only print out error once.
										futures.put(future, true);
										try {
											/* Statistics */ recordsCleaned += 
												future.get();
										} catch(ExecutionException | InterruptedException ee) {
											noErrors = false;
											ee.printStackTrace();
										}
									}
								}
							}
							if (!allDone) {
								try {
									Thread.sleep(1000l); //siphon.getCheckDelay()*1000l);
								} catch (InterruptedException e) {
									e.printStackTrace();
									outcome = null;
								} finally {
									delaySeconds ++; //= siphon.getCheckDelay();
									if (delaySeconds % siphon.getCheckDelay() == 0) {
										System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for table cleanup.");
									}
								}
							}
						}
						System.out.println("Took ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) to cleanup slice.");
					} else {
						System.err.println("Aborting, slice cleanup skipped.");
					}

					/* Statistics */ timeSpentCleaning = System.currentTimeMillis() - timeSpentCleaning;
				
					// REMOVE SLICE ID TABLE
					if (!abort) {
						System.out.println("Cleaning up slice table and index");
						prep = doWork.submit(new Callable<Integer>() {
							@Override
							public Integer call() throws Exception {
								SiphonConnection connect = null;
								try {
									connect = database.connect();
								
									PreparedStatement removeIndex = connect.prepareStatement(SiphonConnection.REMOVE_SLICE_INDEX);
									removeIndex.execute();
									removeIndex.close();
			
									PreparedStatement removeSliceTable = connect.prepareStatement(SiphonConnection.REMOVE_SLICE_TABLE);
									removeSliceTable.execute();
									removeSliceTable.close();
			
									return 1;
								} catch (SQLException se) {
									throw new Exception("Failure cleaning up slice table and index", se);
								} finally {
									if (connect != null) connect.close();
								}
		
							}	
						});
						
						outcome = null;
						delaySeconds = 0l;
						delayStart = System.currentTimeMillis();
						while (outcome == null) {
							try {
								outcome = prep.get(1, TimeUnit.SECONDS); //siphon.getCheckDelay(), TimeUnit.SECONDS);
							} catch (TimeoutException te) {
								outcome = null;
							} catch (InterruptedException e) {
								if (siphon.isDebug()) e.printStackTrace();
								outcome = null;
							} catch (ExecutionException e) {
								e.printStackTrace();
								outcome = null;
								break;
							} finally {
								delaySeconds ++; //= siphon.getCheckDelay();
								if (delaySeconds % siphon.getCheckDelay() == 0) {
									System.out.println("Waited ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) so far for removal of slice ID table and index.");
								}
								if (prep.isDone()) {
									break;
								}
							}
						}
						System.out.println("Took ~" + delaySeconds + "sec (" + (System.currentTimeMillis() - delayStart) + "ms system clock) to remove slice ID table and index.");
					}
				}
			
				// CLEAN TRANSACTION
				try {
					System.out.println("Preparing to remove resume point.");
					connect = database.connect();
					
					PreparedStatement transEnd = connect.prepareStatement(SiphonConnection.TRANS_REMOVE);
					transEnd.execute();
					System.out.println("Resume point removed." );
					transEnd.close();
					connect.close();
				} catch (SQLException sqe) {
					System.err.println("WARNING: Resume point not cleared.");
					sqe.printStackTrace();
				}
				
				doWork.shutdown();
				
				System.out.println("Siphon Worker Process Complete." );
			} else {
				connect.close();
				System.err.println("Unable to find any data to siphon.");
			}
		} catch(SiphonFailure sf) {
			System.err.println("Failed to connect to the database");
			sf.printStackTrace();
			return Boolean.FALSE;
		} catch(SQLException se) {
			System.err.println("Failed to retrieve information from the database");
			se.printStackTrace();
			return Boolean.FALSE;
		} finally {
			if (connect != null) {
				connect.close();
			}
			/* Statistics */ timeTotal = System.currentTimeMillis() - timeTotal;
			/* Statistics */ System.out.println(String.format("Spent %dms exporting %d records.\nSpent %dms invoking system commands.\nSpent %dms cleaning %d records.\nSpent %dms total.", timeSpentExporting, recordsExported, timeSpentInvoking, timeSpentCleaning, recordsCleaned, timeTotal));
		}
		return Boolean.TRUE;
	}
	
	/**
	 * Gets a timestamp associated with a specific sequence within dev_player
	 **/
	public long getTime(long ID, SiphonConnection connect) throws SQLException {
		PreparedStatement eventtime = connect.prepareStatement(SiphonConnection.SAMPLE_DATE);
		eventtime.setLong(1, ID);
		ResultSet eventtimeRS = eventtime.executeQuery();
		if (eventtimeRS.next()) {
			Timestamp time = eventtimeRS.getTimestamp(1);
			if (time != null) {
				return time.getTime();
			}
		}
		throw new SiphonFailure("Unable to get event time based on ID");
	}

}
