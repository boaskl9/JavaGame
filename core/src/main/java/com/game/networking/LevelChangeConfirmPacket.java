package com.game.networking;

/**
 * Server-to-client confirmation of level change request.
 * Contains the validated spawn position and success status.
 *
 * Flow:
 * 1. Server receives LevelChangeRequestPacket
 * 2. Server validates request (level exists, player can access it, etc.)
 * 3. Server loads/activates target world
 * 4. Server calculates spawn point coordinates
 * 5. Server sends this confirmation packet to client
 * 6. Client teleports to confirmed position and loads level locally
 *
 * This eliminates spawn coordinate timing issues by providing exact server-validated position.
 */
public class LevelChangeConfirmPacket extends Packet {
    /**
     * The player this confirmation is for
     */
    public int playerId;

    /**
     * Level ID that was loaded (should match request)
     */
    public String levelId;

    /**
     * Exact X coordinate to spawn at (server-authoritative)
     */
    public float spawnX;

    /**
     * Exact Y coordinate to spawn at (server-authoritative)
     */
    public float spawnY;

    /**
     * Whether the level change succeeded
     */
    public boolean success;

    /**
     * Error message if failed (e.g., "Level not found", "Access denied")
     */
    public String errorMessage;

    /**
     * Server timestamp when confirmation was sent (for latency tracking)
     */
    public long serverTimestamp;

    /**
     * Default constructor for Kryo serialization
     */
    public LevelChangeConfirmPacket() {
    }

    /**
     * Create a successful level change confirmation
     * @param playerId Player being confirmed
     * @param levelId Level that was loaded
     * @param spawnX X coordinate to spawn at
     * @param spawnY Y coordinate to spawn at
     */
    public static LevelChangeConfirmPacket success(int playerId, String levelId, float spawnX, float spawnY) {
        LevelChangeConfirmPacket packet = new LevelChangeConfirmPacket();
        packet.playerId = playerId;
        packet.levelId = levelId;
        packet.spawnX = spawnX;
        packet.spawnY = spawnY;
        packet.success = true;
        packet.serverTimestamp = System.currentTimeMillis();
        return packet;
    }

    /**
     * Create a failed level change confirmation
     * @param playerId Player being confirmed
     * @param errorMessage Error message explaining failure
     */
    public static LevelChangeConfirmPacket failure(int playerId, String errorMessage) {
        LevelChangeConfirmPacket packet = new LevelChangeConfirmPacket();
        packet.playerId = playerId;
        packet.success = false;
        packet.errorMessage = errorMessage;
        packet.serverTimestamp = System.currentTimeMillis();
        return packet;
    }

    @Override
    public PacketType getType() {
        return PacketType.LEVEL_CHANGE_CONFIRM;
    }
}
