package com.programmerdan.minecraft.devotion.dao.flyweight;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.event.player.PlayerKickEvent;

import com.programmerdan.minecraft.devotion.dao.FlyweightType;
import com.programmerdan.minecraft.devotion.dao.database.SqlDatabase;
import com.programmerdan.minecraft.devotion.dao.info.DevotionEventKickInfo;

/**
 * Soft wrapper for the abstract underlying class.
 * @author Aleksey Terzi
 *
 */

public class fPlayerKick extends fPlayer {
	private DevotionEventKickInfo kickInfo;
	
	public fPlayerKick(PlayerKickEvent event) {
		super(event, FlyweightType.Kick);
		
		if(event != null) {
			this.kickInfo = new DevotionEventKickInfo();
			this.kickInfo.eventTime = this.eventInfo.eventTime;
			this.kickInfo.playerUUID = this.eventInfo.playerUUID;
			this.kickInfo.leaveMessage = event.getLeaveMessage();
			this.kickInfo.kickReason = event.getReason();
		}
	}
	
	@Override
	protected void marshallToStream(DataOutputStream os) throws IOException {
		super.marshallToStream(os);
		
		os.writeUTF(this.kickInfo.leaveMessage != null ? this.kickInfo.leaveMessage: "");
		os.writeUTF(this.kickInfo.kickReason != null ? this.kickInfo.kickReason: "");
	}
	
	@Override
	protected void unmarshallFromStream(DataInputStream is) throws IOException {
		super.unmarshallFromStream(is);
		
		this.kickInfo = new DevotionEventKickInfo();
		this.kickInfo.eventTime = this.eventInfo.eventTime;
		this.kickInfo.playerUUID = this.eventInfo.playerUUID;

		this.kickInfo.leaveMessage = is.readUTF();
		if(this.kickInfo.leaveMessage == "") this.kickInfo.leaveMessage = null;
		
		this.kickInfo.kickReason = is.readUTF();
		if(this.kickInfo.kickReason == "") this.kickInfo.kickReason = null;
	}
	
	@Override
	protected void marshallToDatabase(SqlDatabase db) throws SQLException {
		super.marshallToDatabase(db);
		
		db.getDevotionEventKickSource().insert(this.kickInfo);
	}
}