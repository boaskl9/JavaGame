package com.game.save;

/**
 * Serializable representation of player state.
 */
public class PlayerData {
    public float x;
    public float y;
    public int currentHealth;
    public int maxHealth;
    public InventoryData inventory;
    public String levelId;     // Level the player was in (used for multiplayer guests; null for older saves)
    public String displayName; // Last known name (multiplayer guests; for humans reading the save)

    // Required for JSON deserialization
    public PlayerData() {
    }

    public PlayerData(float x, float y, int currentHealth, int maxHealth, InventoryData inventory) {
        this.x = x;
        this.y = y;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.inventory = inventory;
    }
}
