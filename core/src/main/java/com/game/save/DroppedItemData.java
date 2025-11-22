package com.game.save;

/**
 * Serializable representation of a dropped item in the world.
 */
public class DroppedItemData {
    public String itemId;
    public int quantity;
    public float x;
    public float y;

    // Required for JSON deserialization
    public DroppedItemData() {
    }

    public DroppedItemData(String itemId, int quantity, float x, float y) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.x = x;
        this.y = y;
    }
}
