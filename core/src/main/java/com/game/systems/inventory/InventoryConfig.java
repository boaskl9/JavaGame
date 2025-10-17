package com.game.systems.inventory;

/**
 * Configuration constants for the inventory system.
 * All values are public and mutable to allow easy tuning.
 */
public class InventoryConfig {

    // Inventory sizes
    public static int DEFAULT_INVENTORY_SIZE = 8;      // Slots when no bags equipped
    public static int MAX_BAG_SLOTS = 16;               // Number of bag equipment slots

    // World items
    public static int MAX_WORLD_ITEMS = 100;           // Global limit for dropped items
    public static float ITEM_DROP_SPREAD = 16f;        // Radius for dropped item pile (pixels)

    // Item magnetism
    public static float ITEM_MAGNET_RADIUS = 32f;      // Pickup magnetism range (pixels)
    public static float ITEM_MAGNET_ACCELERATION = 400f; // Magnetism acceleration (pixels/sec²)
    public static float ITEM_MAGNET_MAX_SPEED = 300f;  // Maximum magnetism speed (pixels/sec)

    // Visual
    public static float ITEM_PICKUP_SCALE = 0.4f;      // Scale of items on ground
    public static float ITEM_BOUNCE_HEIGHT = 1.5f;       // Bounce animation height
    public static float ITEM_BOUNCE_SPEED = 2f;        // Bounce animation speed

    private InventoryConfig() {
        // Prevent instantiation
    }
}
