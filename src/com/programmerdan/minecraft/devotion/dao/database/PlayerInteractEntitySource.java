package com.programmerdan.minecraft.devotion.dao.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.programmerdan.minecraft.devotion.dao.info.PlayerInteractEntityInfo;

public class PlayerInteractEntitySource extends Source {
	private static final String insertScript = "INSERT dev_player_interact_entity (trace_id, clicked_entity, clicked_entity_uuid, event_cancelled) VALUES (?, ?, ?, ?)";
	
	public PlayerInteractEntitySource(SqlDatabase db) {
		super(db);
	}
		
	public void insert(PlayerInteractEntityInfo info) throws SQLException {
		PreparedStatement sql = getSql(insertScript);

		sql.setString(1, info.trace_id);
		
		if(info.clickedEntity != null) {
			sql.setString(2, info.clickedEntity);
		} else {
			sql.setNull(2, Types.VARCHAR);
		}
		
		if(info.clickedEntityUUID != null) {
			sql.setString(3, info.clickedEntityUUID);
		} else {
			sql.setNull(3, Types.VARCHAR);
		}
		
		sql.setBoolean(4, info.eventCancelled);
		
		sql.addBatch();
	}
}
