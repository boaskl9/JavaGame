package com.game.systems.entity.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Particle effect entity that spawns debris/shards when objects break.
 * Creates multiple small particles that scatter outward and fade over time.
 */
public class DestructionParticleEntity extends GameObject {
    private static final float PARTICLE_LIFETIME = 0.5f; // Seconds
    private static final int PARTICLE_COUNT = 8; // Number of debris pieces
    private static final float PARTICLE_SIZE_MIN = 2f;
    private static final float PARTICLE_SIZE_MAX = 4f;
    private static final float SCATTER_SPEED_MIN = 30f;
    private static final float SCATTER_SPEED_MAX = 80f;
    private static final float GRAVITY = -200f; // Pixels per second squared

    private Transform transform;
    private List<Particle> particles;
    private float stateTime;
    private boolean alive;
    private Texture particleTexture;

    /**
     * Creates a destruction particle effect at the specified position.
     * @param x World X position (center)
     * @param y World Y position (center)
     * @param particleType Type hint for particle color ("brown" for pottery, "wood" for crates, etc.)
     */
    public DestructionParticleEntity(float x, float y, String particleType) {
        this.stateTime = 0f;
        this.alive = true;
        this.particles = new ArrayList<>();

        // Set up transform
        transform = new Transform(x, y);
        addComponent(transform);

        // Create simple particle texture (small square)
        particleTexture = createParticleTexture(getColorFromType(particleType));

        // Spawn particles with random velocities
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float size = MathUtils.random(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX);
            float angle = MathUtils.random(0f, 360f);
            float speed = MathUtils.random(SCATTER_SPEED_MIN, SCATTER_SPEED_MAX);

            Vector2 velocity = new Vector2(
                MathUtils.cosDeg(angle) * speed,
                MathUtils.sinDeg(angle) * speed
            );

            particles.add(new Particle(x, y, velocity, size));
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        stateTime += delta;

        // Update all particles
        for (Particle particle : particles) {
            particle.update(delta);
        }

        // Remove when lifetime expires
        if (stateTime >= PARTICLE_LIFETIME) {
            alive = false;
            dispose();
        }
    }

    /**
     * Renders all particles with fade-out effect.
     */
    public void render(SpriteBatch batch) {
        if (!alive || particleTexture == null) return;

        // Calculate alpha based on lifetime (fade out)
        float alpha = 1f - (stateTime / PARTICLE_LIFETIME);
        alpha = Math.max(0f, Math.min(1f, alpha)); // Clamp to 0-1

        // Save old color
        Color oldColor = batch.getColor().cpy();

        for (Particle particle : particles) {
            // Set alpha for fade effect
            batch.setColor(1f, 1f, 1f, alpha);

            batch.draw(
                particleTexture,
                particle.position.x - particle.size / 2f,
                particle.position.y - particle.size / 2f,
                particle.size,
                particle.size
            );
        }

        // Restore old color
        batch.setColor(oldColor);
    }

    /**
     * Creates a simple colored texture for particles.
     */
    private Texture createParticleTexture(Color color) {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Maps particle type hint to color.
     */
    private Color getColorFromType(String type) {
        if (type == null) return Color.GRAY;

        switch (type.toLowerCase()) {
            case "brown":
            case "pottery":
            case "clay":
                return new Color(0.6f, 0.4f, 0.2f, 1f); // Brown

            case "wood":
            case "wooden":
                return new Color(0.7f, 0.5f, 0.3f, 1f); // Tan/wood

            case "stone":
            case "rock":
                return Color.GRAY;

            case "metal":
            case "iron":
                return new Color(0.7f, 0.7f, 0.8f, 1f); // Silver-gray

            default:
                return Color.WHITE;
        }
    }

    /**
     * Disposes of particle textures.
     */
    private void dispose() {
        if (particleTexture != null) {
            particleTexture.dispose();
            particleTexture = null;
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public Transform getTransform() {
        return transform;
    }

    /**
     * Inner class representing a single particle.
     */
    private static class Particle {
        Vector2 position;
        Vector2 velocity;
        float size;

        Particle(float x, float y, Vector2 velocity, float size) {
            this.position = new Vector2(x, y);
            this.velocity = velocity;
            this.size = size;
        }

        void update(float delta) {
            // Apply velocity
            position.add(velocity.x * delta, velocity.y * delta);

            // Apply gravity
            velocity.y += GRAVITY * delta;
        }
    }
}
