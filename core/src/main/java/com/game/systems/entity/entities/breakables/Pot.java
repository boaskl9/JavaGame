package com.game.systems.entity.entities.breakables;

import com.game.systems.entity.entities.BreakableEntity;

/**
 * Clay Pot - A fragile destructible object that breaks easily.
 *
 * Properties:
 * - Health: 2 (breaks in 2 hits)
 * - Loot: Coins, health potions, stones
 * - Visual: Clay pottery sprite with shattering animation
 *
 * Configuration loaded from BreakableObjectConfig.json ("pot" entry).
 */
public class Pot extends BreakableEntity {

    /**
     * Creates a clay pot at the specified position.
     * @param x World X position
     * @param y World Y position
     */
    public Pot(float x, float y) {
        super(x, y, "pot");
    }

    // No custom behavior needed - all handled by BreakableEntity base class
    // Override methods here if you want pot-specific behavior:

    // Example: Custom break duration
    // @Override
    // protected float getBreakAnimationDuration() {
    //     return 0.4f; // Slightly longer break animation
    // }

    // Example: Special effects on destruction
    // @Override
    // protected void onDeath() {
    //     super.onDeath(); // Call parent first
    //     // Add pot-specific behavior here
    // }
}
