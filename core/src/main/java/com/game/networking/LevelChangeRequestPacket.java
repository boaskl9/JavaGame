package com.game.networking;

/**
 * Client-to-server request to change levels.
 * Sent when client player enters a gateway or wants to transition to a new level.
 *
 * Flow:
 * 1. Client detects gateway collision
 * 2. Client sends LevelChangeRequestPacket to server
 * 3. Server validates request, loads level, calculates spawn point
 * 4. Server sends LevelChangeConfirmPacket back to client
 * 5. Client receives confirmation and loads level locally
 *
 * This replaces the old "piggyback on InputPacket" approach which had timing issues.
 */
public class LevelChangeRequestPacket extends Packet {
    /**
     * The player requesting the level change
     */
    public int playerId;

    /**
     * Target level ID (e.g., "Maps/WestArea.tmx")
     */
    public String targetLevelId;

    /**
     * Spawn point name in target level (can be null for default spawn)
     */
    public String spawnPointName;

    /**
     * Client timestamp when request was made (for latency tracking)
     */
    public long clientTimestamp;

    /**
     * Default constructor for Kryo serialization
     */
    public LevelChangeRequestPacket() {
    }

    /**
     * Create a level change request
     * @param playerId Player making the request
     * @param targetLevelId Level to load
     * @param spawnPointName Spawn point in target level (null = default)
     */
    public LevelChangeRequestPacket(int playerId, String targetLevelId, String spawnPointName) {
        this.playerId = playerId;
        this.targetLevelId = targetLevelId;
        this.spawnPointName = spawnPointName;
        this.clientTimestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.LEVEL_CHANGE_REQUEST;
    }
}
