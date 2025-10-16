package com.game.components;

import com.badlogic.gdx.math.Rectangle;
import com.game.systems.entity.Component;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Component that defines a collision box.
 * Works with the SpatialQuery system for collision detection.
 */
public class ColliderComponent implements Component {
    private float width;
    private float height;
    private float offsetX;
    private float offsetY;

    public ColliderComponent(float width, float height) {
        this.width = width;
        this.height = height;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    public ColliderComponent(float width, float height, float offsetX, float offsetY) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * Get the collision bounds for this object.
     */
    public Rectangle getBounds(GameObject gameObject) {
        Transform transform = gameObject.getComponent(Transform.class);
        if (transform == null) {
            return new Rectangle(0, 0, width, height);
        }

        return new Rectangle(
            transform.getX() + offsetX,
            transform.getY() + offsetY,
            width,
            height
        );
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
