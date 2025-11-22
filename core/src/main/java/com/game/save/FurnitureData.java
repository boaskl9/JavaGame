package com.game.save;

import java.util.List;

/**
 * Serializable representation of placed furniture.
 */
public class FurnitureData {
    public String itemId; // Furniture item definition ID
    public float x;
    public float y;
    public List<ItemStackData> inventoryContents; // For chests, null for non-container furniture

    // Required for JSON deserialization
    public FurnitureData() {
    }

    public FurnitureData(String itemId, float x, float y, List<ItemStackData> inventoryContents) {
        this.itemId = itemId;
        this.x = x;
        this.y = y;
        this.inventoryContents = inventoryContents;
    }
}
