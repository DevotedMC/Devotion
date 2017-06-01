package com.programmerdan.minecraft.devotion.dao.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.programmerdan.minecraft.devotion.dao.info.PlayerVehicleInfo;

/**
 * @author ProgrammerDan
 *
 */

public class PlayerVehicleSource extends Source {
	private static final String insertScript = "INSERT dev_player_vehicle (trace_id, vehicle_type, vehicle_name, event_cancelled) VALUES (?, ?, ?, ?)";
	
	public PlayerVehicleSource(SqlDatabase db) {
		super(db);
	}
		
	public void insert(PlayerVehicleInfo info) throws SQLException {
		PreparedStatement sql = getSql(insertScript);

		sql.setString(1, info.trace_id);
		
		if(info.vehicle_type != null) {
			sql.setString(2, info.vehicle_type); 
		} else {
			sql.setNull(2, Types.VARCHAR);
		}

		if(info.vehicle_name != null) {
			sql.setString(2, info.vehicle_name); 
		} else {
			sql.setNull(2, Types.VARCHAR);
		}
		
		sql.setBoolean(3, info.eventCancelled);
		
		sql.addBatch();
	}
}