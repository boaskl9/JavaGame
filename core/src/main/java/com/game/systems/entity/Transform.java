package com.game.systems.entity;

import com.badlogic.gdx.math.Vector2;

/**
 * Transform component - position, rotation, scale.
 * Completely standalone and reusable.
 */
public class Transform implements Component {
    private Vector2 position;
    private float rotation; // in degrees
    private Vector2 scale;

    public Transform() {
        this.position = new Vector2();
        this.rotation = 0;
        this.scale = new Vector2(1, 1);
    }

    public Transform(float x, float y) {
        this();
        this.position.set(x, y);
    }

    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }

    public void setPosition(Vector2 pos) {
        position.set(pos);
    }

    public void translate(float dx, float dy) {
        position.add(dx, dy);
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public Vector2 getScale() {
        return scale;
    }

    public void setScale(float scaleX, float scaleY) {
        scale.set(scaleX, scaleY);
    }

    public void setScale(float uniformScale) {
        scale.set(uniformScale, uniformScale);
    }
}
