package com.game.systems.item;

/**
 * Factory for creating ItemStack instances.
 * Provides convenient methods for item creation.
 */
public class ItemFactory {

    /**
     * Creates a single item stack.
     * @param itemId The item ID
     * @param quantity The stack quantity
     * @return The created item stack, or null if item doesn't exist
     */
    public static ItemStack create(String itemId, int quantity) {
        ItemDefinition definition = ItemRegistry.get(itemId);
        if (definition == null) {
            System.err.println("Warning: Attempted to create unknown item: " + itemId);
            return null;
        }
        return new ItemStack(definition, quantity);
    }

    /**
     * Creates a single item (quantity = 1).
     * @param itemId The item ID
     * @return The created item stack, or null if item doesn't exist
     */
    public static ItemStack create(String itemId) {
        return create(itemId, 1);
    }

    /**
     * Creates an item stack with maximum stack size.
     * @param itemId The item ID
     * @return The created item stack, or null if item doesn't exist
     */
    public static ItemStack createMaxStack(String itemId) {
        ItemDefinition definition = ItemRegistry.get(itemId);
        if (definition == null) {
            System.err.println("Warning: Attempted to create unknown item: " + itemId);
            return null;
        }
        return new ItemStack(definition, definition.getMaxStackSize());
    }
}
