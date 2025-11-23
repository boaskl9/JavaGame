package com.game.save;

/**
 * Root container for all save data.
 * This is what gets serialized to JSON and saved to disk.
 */
public class SaveData {
    public static final String CURRENT_VERSION = "1.0.0";

    public String version; // Save format version for migration support
    public String saveName; // Player-chosen save name
    public long timestamp; // When this save was created (Unix timestamp in milliseconds)
    public int playtimeSeconds; // Total playtime in seconds
    public PlayerData player;
    public WorldData world;

    // Required for JSON deserialization
    public SaveData() {
    }

    public SaveData(String saveName, int playtimeSeconds, PlayerData player, WorldData world) {
        this.version = CURRENT_VERSION;
        this.saveName = saveName;
        this.timestamp = System.currentTimeMillis();
        this.playtimeSeconds = playtimeSeconds;
        this.player = player;
        this.world = world;
    }

    /**
     * Extract metadata from this save data.
     */
    public SaveMetadata extractMetadata() {
        return new SaveMetadata(
            saveName,
            timestamp,
            playtimeSeconds,
            world.currentLevelId,
            player.currentHealth,
            player.maxHealth
        );
    }
}
