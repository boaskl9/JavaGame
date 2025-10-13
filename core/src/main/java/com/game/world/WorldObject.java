package com.game.world;

import com.game.systems.entity.GameObject;

/**
 * Base class for non-living world objects (Gateways, Triggers, Pickups, etc.).
 * These are objects that exist in the world but aren't "alive" like entities.
 *
 * This provides a clear semantic distinction:
 * - Entity = Living things (Player, NPCs, Enemies)
 * - WorldObject = Environmental objects (Gateways, Triggers, Items)
 */
public abstract class WorldObject extends GameObject {

    private String objectType;

    public WorldObject(String objectType) {
        super();
        this.objectType = objectType;
    }

    /**
     * Get the type of this world object (e.g., "gateway", "trigger", "pickup").
     */
    public String getObjectType() {
        return objectType;
    }

    /**
     * Called when an entity interacts with this world object.
     * Override to implement interaction behavior.
     */
    public void onInteract(com.game.systems.entity.Entity entity) {
        // Override in subclasses
    }
}
