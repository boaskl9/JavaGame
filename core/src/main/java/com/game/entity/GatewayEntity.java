package com.game.entity;

import com.game.components.ColliderComponent;
import com.game.systems.entity.Transform;
import com.game.world.WorldObject;

/**
 * Gateway world object - used for level transitions.
 * Extends WorldObject since it's not a living entity, just an environmental trigger.
 */
public class GatewayEntity extends WorldObject {
    private final String targetLevel;
    private final String targetSpawn;

    public GatewayEntity(float x, float y, float width, float height, String targetLevel, String targetSpawn) {
        super("gateway");
        this.targetLevel = targetLevel;
        this.targetSpawn = targetSpawn;

        // Add components
        Transform transform = new Transform(x, y);
        addComponent(transform);

        ColliderComponent collider = new ColliderComponent(width, height);
        addComponent(collider);
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getTargetSpawn() {
        return targetSpawn;
    }
}
