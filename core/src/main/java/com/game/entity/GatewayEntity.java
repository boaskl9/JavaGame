package com.game.entity;

import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Gateway entity using component-based architecture.
 * No longer depends on the old Entity interface.
 */
public class GatewayEntity extends GameObject {
    private String targetLevel;
    private String targetSpawn;

    public GatewayEntity(float x, float y, float width, float height, String targetLevel, String targetSpawn) {
        super();
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
