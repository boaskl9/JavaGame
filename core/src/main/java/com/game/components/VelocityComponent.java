package com.game.components;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.Component;

/**
 * Component for entities that move.
 */
public class VelocityComponent implements Component {
    private Vector2 velocity;

    public VelocityComponent() {
        this.velocity = new Vector2();
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setVelocity(float x, float y) {
        velocity.set(x, y);
    }

    public void setVelocity(Vector2 vel) {
        velocity.set(vel);
    }

    public void addVelocity(float dx, float dy) {
        velocity.add(dx, dy);
    }
}
