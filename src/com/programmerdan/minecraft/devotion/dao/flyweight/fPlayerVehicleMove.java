package com.programmerdan.minecraft.devotion.dao.flyweight;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.entity.Player;

import com.programmerdan.minecraft.devotion.dao.FlyweightType;
import com.programmerdan.minecraft.devotion.dao.database.SqlDatabase;
import com.programmerdan.minecraft.devotion.dao.info.PlayerVehicleInfo;

/**
 * 
 * @author ProgrammerDan
 *
 */
public class fPlayerVehicleMove extends fPlayer {
	private PlayerVehicleInfo vehicleInfo;
	
	public fPlayerVehicleMove(VehicleMoveEvent event) {
		super((Player)event.getVehicle().getPassenger(), FlyweightType.VehicleMove);
		
		if(event != null) {
			this.vehicleInfo = new PlayerVehicleInfo();
			this.vehicleInfo.trace_id = this.eventInfo.trace_id;
			
			this.vehicleInfo.vehicle_type = event.getVehicle() != null ? event.getVehicle().getType().name(): null;
			this.vehicleInfo.vehicle_name = event.getVehicle() != null ? event.getVehicle().getCustomName(): null;
			this.vehicleInfo.eventCancelled = false; // I don't really feel like having a unique type just for this smh
		}
	}
	
	@Override
	protected void marshallToStream(DataOutputStream os) throws IOException {
		super.marshallToStream(os);
		
		os.writeUTF(this.vehicleInfo.vehicle_type != null ? this.vehicleInfo.vehicle_type: "");
		os.writeUTF(this.vehicleInfo.vehicle_name != null ? this.vehicleInfo.vehicle_name: "");
		os.writeBoolean(this.vehicleInfo.eventCancelled);
	}
	
	@Override
	protected void unmarshallFromStream(DataInputStream is) throws IOException {
		super.unmarshallFromStream(is);
		
		this.vehicleInfo = new PlayerVehicleInfo();
		this.vehicleInfo.trace_id = this.eventInfo.trace_id;

		this.vehicleInfo.vehicle_type = is.readUTF();
		if(this.vehicleInfo.vehicle_type == "") this.vehicleInfo.vehicle_type = null;

		this.vehicleInfo.vehicle_name = is.readUTF();
		if(this.vehicleInfo.vehicle_name == "") this.vehicleInfo.vehicle_name = null;
		
		this.vehicleInfo.eventCancelled = is.readBoolean();
	}
	
	@Override
	protected void marshallToDatabase(SqlDatabase db) throws SQLException {
		super.marshallToDatabase(db);
		
		db.getPlayerVehicleSource().insert(this.vehicleInfo);
	}
}
