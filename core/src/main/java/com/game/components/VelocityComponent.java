package com.game.components;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.Component;

/**
 * Component for entities that move.
 * Handles both movement velocity (from input/AI) and knockback velocity (from hits).
 */
public class VelocityComponent implements Component {
    private Vector2 movementVelocity;  // Velocity from movement (input/AI)
    private Vector2 knockbackVelocity; // Velocity from knockback (temporary)
    private Vector2 combinedVelocity;  // Combined result (cached)

    private static final float KNOCKBACK_DECAY = 8f; // Knockback decay rate per second

    public VelocityComponent() {
        this.movementVelocity = new Vector2();
        this.knockbackVelocity = new Vector2();
        this.combinedVelocity = new Vector2();
    }

    /**
     * Gets the final velocity (movement + knockback).
     * This is what movement systems should use.
     */
    public Vector2 getVelocity() {
        combinedVelocity.set(movementVelocity).add(knockbackVelocity);
        return combinedVelocity;
    }

    /**
     * Sets the movement velocity (from input/AI).
     * Does not affect knockback velocity.
     */
    public void setVelocity(float x, float y) {
        movementVelocity.set(x, y);
    }

    /**
     * Sets the movement velocity (from input/AI).
     * Does not affect knockback velocity.
     */
    public void setVelocity(Vector2 vel) {
        movementVelocity.set(vel);
    }

    /**
     * Adds to knockback velocity (used by combat system).
     * This is temporary and decays over time.
     */
    public void addVelocity(float dx, float dy) {
        knockbackVelocity.add(dx, dy);
    }

    /**
     * Updates knockback decay.
     * Should be called every frame.
     */
    public void update(float delta) {
        // Decay knockback velocity toward zero
        if (knockbackVelocity.len2() > 0.1f) {
            float decay = KNOCKBACK_DECAY * delta;
            knockbackVelocity.scl(Math.max(0, 1f - decay));

            // Stop completely when very small
            if (knockbackVelocity.len2() < 1f) {
                knockbackVelocity.set(0, 0);
            }
        }
    }

    /**
     * Gets the current knockback velocity (for debugging).
     */
    public Vector2 getKnockbackVelocity() {
        return knockbackVelocity;
    }
}
