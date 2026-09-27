package com.game.networking;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-all broadcast when an entity spawns in the world.
 * Used for enemies, items, breakables, and any other networked entities.
 *
 * Flow:
 * 1. Server spawns entity (enemy, item drop, etc.)
 * 2. Server assigns global unique entityId
 * 3. Server broadcasts EntitySpawnPacket to all clients
 * 4. Clients in same level create entity locally
 * 5. Entity updates come through StateUpdatePacket
 *
 * This enables full entity synchronization across clients.
 */
public class EntitySpawnPacket extends Packet {
    /**
     * Global unique entity ID (assigned by server)
     * Used to identify this entity in future packets
     */
    public int entityId;

    /**
     * Entity type discriminator
     * Values: "enemy", "item", "breakable", "projectile", etc.
     */
    public String entityType;

    /**
     * Level this entity exists in
     * Clients only spawn if they're in the same level
     */
    public String levelId;

    /**
     * X position to spawn at
     */
    public float x;

    /**
     * Y position to spawn at
     */
    public float y;

    /**
     * Type-specific metadata for creating the entity
     * Examples:
     * - Enemy: {"enemyType": "lizard", "health": "10"}
     * - Item: {"itemId": "health_potion", "quantity": "1"}
     * - Breakable: {"objectType": "pot"}
     */
    public Map<String, String> metadata;

    /**
     * Default constructor for Kryo serialization
     */
    public EntitySpawnPacket() {
        this.metadata = new HashMap<>();
    }

    /**
     * Create an entity spawn packet
     * @param entityId Global unique ID
     * @param entityType Type discriminator
     * @param levelId Level the entity is in
     * @param x X coordinate
     * @param y Y coordinate
     */
    public EntitySpawnPacket(int entityId, String entityType, String levelId, float x, float y) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.levelId = levelId;
        this.x = x;
        this.y = y;
        this.metadata = new HashMap<>();
    }

    /**
     * Add metadata key-value pair
     * @param key Metadata key
     * @param value Metadata value (will be converted to string)
     */
    public EntitySpawnPacket withMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Get metadata value
     * @param key Metadata key
     * @return Value or null if not found
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Get metadata value as integer
     * @param key Metadata key
     * @param defaultValue Default if not found or not parseable
     * @return Integer value
     */
    public int getMetadataInt(String key, int defaultValue) {
        String value = getMetadata(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get metadata value as float
     * @param key Metadata key
     * @param defaultValue Default if not found or not parseable
     * @return Float value
     */
    public float getMetadataFloat(String key, float defaultValue) {
        String value = getMetadata(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public PacketType getType() {
        return PacketType.ENTITY_SPAWN;
    }
}
