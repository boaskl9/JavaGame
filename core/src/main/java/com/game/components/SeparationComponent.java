package com.game.components;

import com.game.systems.entity.Component;

/**
 * Component that defines separation behavior for entities.
 * Separation creates a "personal space" force that pushes entities away from nearby allies,
 * preventing bunching and creating natural flocking behavior.
 *
 * This is commonly used in boids/flocking algorithms and makes enemy groups look more intelligent.
 */
public class SeparationComponent implements Component {
    private float separationRadius;  // How far to detect nearby allies
    private float separationStrength; // How strongly to push away

    /**
     * Creates a separation component with default values.
     * Default: 32px radius, 150 strength
     */
    public SeparationComponent() {
        this(32f, 10f);
    }

    /**
     * Creates a separation component with custom parameters.
     * @param separationRadius How far (in pixels) to check for nearby allies
     * @param separationStrength How strong the push-away force is (higher = more separation)
     */
    public SeparationComponent(float separationRadius, float separationStrength) {
        this.separationRadius = separationRadius;
        this.separationStrength = separationStrength;
    }

    public float getSeparationRadius() {
        return separationRadius;
    }

    public void setSeparationRadius(float separationRadius) {
        this.separationRadius = separationRadius;
    }

    public float getSeparationStrength() {
        return separationStrength;
    }

    public void setSeparationStrength(float separationStrength) {
        this.separationStrength = separationStrength;
    }
}
