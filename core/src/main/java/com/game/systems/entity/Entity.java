package com.game.systems.entity;

import com.game.components.HealthComponent;
import com.game.networking.EntitySnapshot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Base class for living/interactive entities (Player, NPCs, Enemies).
 * Provides common functionality like health, stats, etc.
 *
 * This is different from GameObject - GameObject is the low-level component container,
 * while Entity adds game-specific features for living things.
 *
 * Health is managed via HealthComponent - all health operations delegate to it.
 */
public abstract class Entity extends GameObject {
    /**
     * If true, this entity is controlled by network updates and should NOT run game logic.
     * Only the server or local player should run full simulation.
     * Remote players/enemies are network-controlled puppets that only render.
     */
    protected boolean networkControlled = false;

    /**
     * Interpolation buffer for network-controlled entities.
     * Stores recent snapshots (100-200ms) for smooth rendering.
     * Client renders from this buffer, not directly from latest snapshot.
     */
    private final Deque<EntitySnapshot> snapshotBuffer = new ArrayDeque<>();
    private static final int MAX_SNAPSHOT_BUFFER_SIZE = 10; // ~200ms at 20Hz
    private static final long INTERPOLATION_DELAY_MS = 150; // Render 150ms behind server time

    public Entity(int maxHealth) {
        super();
        // Add HealthComponent to manage health
        HealthComponent healthComponent = new HealthComponent(maxHealth);
        addComponent(healthComponent);
    }

    /**
     * Mark this entity as network-controlled (remote player/enemy).
     * Network-controlled entities skip game logic and only render based on server state.
     */
    public void setNetworkControlled(boolean networkControlled) {
        this.networkControlled = networkControlled;
    }

    public boolean isNetworkControlled() {
        return networkControlled;
    }

    /**
     * Gets the HealthComponent for this entity.
     * @return The HealthComponent, or null if not found
     */
    protected HealthComponent getHealthComponent() {
        return getComponent(HealthComponent.class);
    }

    /**
     * Damage this entity.
     * @param amount Amount of damage to apply
     * @return true if the entity died from this damage
     */
    public boolean damage(int amount) {
        HealthComponent health = getHealthComponent();
        if (health == null) {
            return false;
        }

        boolean died = health.damage(amount);

        if (died) {
            onDeath();
        }

        return died;
    }

    /**
     * Heal this entity.
     * @param amount Amount of health to restore
     */
    public void heal(int amount) {
        HealthComponent health = getHealthComponent();
        if (health != null) {
            health.heal(amount);
        }
    }

    /**
     * Check if this entity is alive.
     */
    public boolean isAlive() {
        HealthComponent health = getHealthComponent();
        return health != null && health.isAlive();
    }

    /**
     * Called when this entity dies.
     * Override to implement death behavior.
     */
    protected void onDeath() {
        setActive(false);
    }

    public int getHealth() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getCurrentHealth() : 0;
    }

    public void setHealth(int health) {
        HealthComponent healthComp = getHealthComponent();
        if (healthComp != null) {
            healthComp.setHealth(health);
        }
    }

    public int getMaxHealth() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getMaxHealth() : 0;
    }

    public void setMaxHealth(int maxHealth) {
        HealthComponent health = getHealthComponent();
        if (health != null) {
            health.setMaxHealth(maxHealth);
        }
    }

    /**
     * Get health as a percentage (0.0 to 1.0).
     */
    public float getHealthPercent() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getHealthPercent() : 0f;
    }

    // ===== NETWORK INTERPOLATION METHODS =====

    /**
     * Enqueue a snapshot for interpolation (network-controlled entities only).
     * Called when receiving state updates from server.
     */
    public void enqueueSnapshot(EntitySnapshot snapshot) {
        if (!networkControlled) {
            return; // Only network-controlled entities use interpolation
        }

        snapshotBuffer.addLast(snapshot);

        // Keep buffer size manageable (drop old snapshots)
        while (snapshotBuffer.size() > MAX_SNAPSHOT_BUFFER_SIZE) {
            snapshotBuffer.removeFirst();
        }
    }

    /**
     * Get interpolated snapshot for rendering.
     * Returns a snapshot interpolated between buffered snapshots at (currentTime - delay).
     *
     * @param currentTimeMs Current client time in milliseconds
     * @return Interpolated snapshot, or null if no snapshots available
     */
    public EntitySnapshot getInterpolatedSnapshot(long currentTimeMs) {
        if (snapshotBuffer.isEmpty()) {
            return null;
        }

        // Calculate render time (current time - interpolation delay)
        long renderTime = currentTimeMs - INTERPOLATION_DELAY_MS;

        // Find two snapshots surrounding renderTime
        EntitySnapshot older = null;
        EntitySnapshot newer = null;

        for (EntitySnapshot snapshot : snapshotBuffer) {
            if (snapshot.timestamp <= renderTime) {
                older = snapshot;
            } else {
                newer = snapshot;
                break;
            }
        }

        // Interpolate between snapshots
        if (older != null && newer != null) {
            return EntitySnapshot.interpolate(older, newer, renderTime);
        } else if (older != null) {
            // No newer snapshot - extrapolate slightly (max 50ms)
            return EntitySnapshot.extrapolate(older, renderTime, 50);
        } else if (newer != null) {
            // No older snapshot - use the newer one directly
            return newer;
        }

        return null;
    }

    /**
     * Get number of buffered snapshots (for debugging).
     */
    public int getSnapshotBufferSize() {
        return snapshotBuffer.size();
    }

    /**
     * Clear snapshot buffer (e.g., on disconnect or teleport).
     */
    public void clearSnapshotBuffer() {
        snapshotBuffer.clear();
    }
}
