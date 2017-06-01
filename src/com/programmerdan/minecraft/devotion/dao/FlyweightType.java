package com.programmerdan.minecraft.devotion.dao;

public class FlyweightType {
	private byte id;
	public byte getId() {
		return this.id;
	}
	
	private String name;
	public String getName() {
		return this.name;
	}
	
	private FlyweightType(byte id, String name) {
		this.id = id;
		this.name = name;
	}

	public static final FlyweightType Login = new FlyweightType((byte) 0x01, "Login");
	public static final FlyweightType Join = new FlyweightType((byte) 0x02, "Join");
	public static final FlyweightType Quit = new FlyweightType((byte) 0x03, "Quit");
	public static final FlyweightType Move = new FlyweightType((byte) 0x04, "Move");
	public static final FlyweightType Interact = new FlyweightType((byte) 0x05, "Interact");
	public static final FlyweightType Kick = new FlyweightType((byte) 0x06, "Kick");
	public static final FlyweightType Teleport = new FlyweightType((byte) 0x07, "Teleport");
	public static final FlyweightType ChangedWorld = new FlyweightType((byte) 0x08, "ChangedWorld");
	public static final FlyweightType Respawn = new FlyweightType((byte) 0x09, "Respawn");
	public static final FlyweightType ToggleFlight = new FlyweightType((byte) 0x0A, "ToggleFlight");
	public static final FlyweightType ToggleSneak = new FlyweightType((byte) 0x0B, "ToggleSneak");
	public static final FlyweightType ToggleSprint = new FlyweightType((byte) 0x0C, "ToggleSprint");
	public static final FlyweightType Velocity = new FlyweightType((byte) 0x0D, "Velocity");
	public static final FlyweightType BedEnter = new FlyweightType((byte) 0x0E, "BedEnter");
	public static final FlyweightType BedLeave = new FlyweightType((byte) 0x0F, "BedLeave");
	public static final FlyweightType Bucket = new FlyweightType((byte) 0x10, "Bucket");
	public static final FlyweightType DropItem = new FlyweightType((byte) 0x11, "DropItem");
	public static final FlyweightType EditBook = new FlyweightType((byte) 0x12, "EditBook");
	public static final FlyweightType EggThrow = new FlyweightType((byte) 0x13, "EggThrow");
	public static final FlyweightType ExpChange = new FlyweightType((byte) 0x14, "ExpChange");
	public static final FlyweightType Fish = new FlyweightType((byte) 0x15, "Fish");
	public static final FlyweightType GameModeChange = new FlyweightType((byte) 0x16, "GameModeChange");
	public static final FlyweightType InteractEntity = new FlyweightType((byte) 0x17, "InteractEntity");
	public static final FlyweightType ItemBreak = new FlyweightType((byte) 0x18, "ItemBreak");
	public static final FlyweightType ItemConsume = new FlyweightType((byte) 0x19, "ItemConsume");
	public static final FlyweightType ItemHeld = new FlyweightType((byte) 0x1A, "ItemHeld");
	public static final FlyweightType LevelChange = new FlyweightType((byte) 0x1B, "LevelChange");
	public static final FlyweightType PickupItem = new FlyweightType((byte) 0x1C, "PickupItem");
	public static final FlyweightType ResourcePackStatus = new FlyweightType((byte) 0x1D, "ResourcePackStatus");
	public static final FlyweightType ShearEntity = new FlyweightType((byte) 0x1E, "ShearEntity");
	public static final FlyweightType StatisticIncrement = new FlyweightType((byte) 0x1F, "StatisticIncrement");
	public static final FlyweightType PlayerDeath = new FlyweightType((byte) 0x20, "PlayerDeath");
	public static final FlyweightType BlockPlace = new FlyweightType((byte) 0x21, "BlockPlace");
	public static final FlyweightType BlockBreak = new FlyweightType((byte) 0x22, "BlockBreak");
	public static final FlyweightType VehicleMove = new FlyweightType((byte) 0x23, "VehicleMove");
	public static final FlyweightType VehicleEnter = new FlyweightType((byte) 0x24, "VehicleEnter");
	public static final FlyweightType VehicleExit = new FlyweightType((byte) 0x25, "VehicleExit");
}
