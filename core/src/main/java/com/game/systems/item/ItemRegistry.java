package com.game.systems.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Central registry for all item definitions.
 * This is a pure data registry with no dependencies on game systems.
 */
public class ItemRegistry {
    private static final Map<String, ItemDefinition> items = new HashMap<>();

    /**
     * Registers an item definition.
     * @param definition The item definition to register
     * @throws IllegalArgumentException if an item with the same ID already exists
     */
    public static void register(ItemDefinition definition) {
        if (items.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Item already registered: " + definition.getId());
        }
        items.put(definition.getId(), definition);
    }

    /**
     * Gets an item definition by ID.
     * @param id The item ID
     * @return The item definition, or null if not found
     */
    public static ItemDefinition get(String id) {
        return items.get(id);
    }

    /**
     * Checks if an item is registered.
     * @param id The item ID
     * @return true if the item exists
     */
    public static boolean has(String id) {
        return items.containsKey(id);
    }

    /**
     * Gets all registered item definitions.
     * @return Collection of all item definitions
     */
    public static Collection<ItemDefinition> getAll() {
        return items.values();
    }

    /**
     * Clears all registered items.
     * Useful for testing or reloading item definitions.
     */
    public static void clear() {
        items.clear();
    }

    /**
     * Gets the number of registered items.
     * @return The count of registered items
     */
    public static int size() {
        return items.size();
    }
}
