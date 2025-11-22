package com.game.save;

/**
 * Serializable representation of an ItemStack.
 * Only stores item ID and quantity - the ItemDefinition is looked up from ItemRegistry.
 */
public class ItemStackData {
    public String itemId;
    public int quantity;

    // Required for JSON deserialization
    public ItemStackData() {
    }

    public ItemStackData(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
