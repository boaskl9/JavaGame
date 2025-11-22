package com.game.save;

/**
 * Lightweight metadata for displaying save slots without loading full save data.
 * Stored separately in <saveName>.meta.json for fast save list rendering.
 */
public class SaveMetadata {
    public String saveName;
    public long timestamp; // Unix timestamp (milliseconds)
    public int playtimeSeconds;
    public String currentLevelId;
    public int playerHealth;
    public int playerMaxHealth;

    // Required for JSON deserialization
    public SaveMetadata() {
    }

    public SaveMetadata(String saveName, long timestamp, int playtimeSeconds,
                        String currentLevelId, int playerHealth, int playerMaxHealth) {
        this.saveName = saveName;
        this.timestamp = timestamp;
        this.playtimeSeconds = playtimeSeconds;
        this.currentLevelId = currentLevelId;
        this.playerHealth = playerHealth;
        this.playerMaxHealth = playerMaxHealth;
    }

    /**
     * Get formatted playtime string (e.g., "1h 30m").
     */
    public String getFormattedPlaytime() {
        int hours = playtimeSeconds / 3600;
        int minutes = (playtimeSeconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
