package com.game.networking;

/**
 * Server-to-all broadcast when an entity despawns from the world.
 * Used for enemies dying, items being picked up, breakables being destroyed, etc.
 *
 * Flow:
 * 1. Server processes entity removal (death, pickup, timeout, etc.)
 * 2. Server removes entity from world
 * 3. Server broadcasts EntityDespawnPacket to all clients
 * 4. Clients remove entity from their local world
 *
 * This keeps entity state synchronized when entities are removed.
 */
public class EntityDespawnPacket extends Packet {
    /**
     * Global unique entity ID to despawn
     * Must match an entityId from a previous EntitySpawnPacket
     */
    public int entityId;

    /**
     * Level the entity was in
     * Clients only despawn if they have this entity
     */
    public String levelId;

    /**
     * Reason for despawn (for debugging/logging)
     * Values: "death", "pickup", "timeout", "destroyed", etc.
     */
    public String reason;

    /**
     * Default constructor for Kryo serialization
     */
    public EntityDespawnPacket() {
    }

    /**
     * Create an entity despawn packet
     * @param entityId Global unique ID to despawn
     * @param levelId Level the entity was in
     * @param reason Reason for despawn
     */
    public EntityDespawnPacket(int entityId, String levelId, String reason) {
        this.entityId = entityId;
        this.levelId = levelId;
        this.reason = reason;
    }

    /**
     * Create a despawn packet for entity death
     * @param entityId Entity that died
     * @param levelId Level entity was in
     * @return Despawn packet
     */
    public static EntityDespawnPacket death(int entityId, String levelId) {
        return new EntityDespawnPacket(entityId, levelId, "death");
    }

    /**
     * Create a despawn packet for item pickup
     * @param entityId Item that was picked up
     * @param levelId Level item was in
     * @return Despawn packet
     */
    public static EntityDespawnPacket pickup(int entityId, String levelId) {
        return new EntityDespawnPacket(entityId, levelId, "pickup");
    }

    /**
     * Create a despawn packet for destruction
     * @param entityId Entity that was destroyed
     * @param levelId Level entity was in
     * @return Despawn packet
     */
    public static EntityDespawnPacket destroyed(int entityId, String levelId) {
        return new EntityDespawnPacket(entityId, levelId, "destroyed");
    }

    /**
     * Create a despawn packet for timeout
     * @param entityId Entity that timed out
     * @param levelId Level entity was in
     * @return Despawn packet
     */
    public static EntityDespawnPacket timeout(int entityId, String levelId) {
        return new EntityDespawnPacket(entityId, levelId, "timeout");
    }

    @Override
    public PacketType getType() {
        return PacketType.ENTITY_DESPAWN;
    }
}
