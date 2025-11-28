package com.game.networking;

/**
 * Represents a snapshot of an entity's state at a specific point in time.
 * Used for interpolation to smooth network-controlled entity movement.
 *
 * Based on the interpolation buffer pattern from Minecraft/Stardew Valley:
 * - Server sends snapshots at 20Hz
 * - Client buffers 100-200ms of snapshots
 * - Rendering interpolates between snapshots for smooth 60 FPS motion
 */
public class EntitySnapshot {
    public final long timestamp; // milliseconds since epoch
    public final float x;
    public final float y;
    public final float vx; // velocity X (for extrapolation if needed)
    public final float vy; // velocity Y
    public final String animation;
    public final int direction;
    public final boolean flipX;
    public final int health;
    public final int maxHealth;

    public EntitySnapshot(long timestamp, float x, float y, float vx, float vy,
                         String animation, int direction, boolean flipX,
                         int health, int maxHealth) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.animation = animation;
        this.direction = direction;
        this.flipX = flipX;
        this.health = health;
        this.maxHealth = maxHealth;
    }

    /**
     * Linearly interpolate between two snapshots.
     * @param older The older snapshot
     * @param newer The newer snapshot
     * @param renderTime The time to interpolate to
     * @return Interpolated snapshot at renderTime
     */
    public static EntitySnapshot interpolate(EntitySnapshot older, EntitySnapshot newer, long renderTime) {
        if (older.timestamp == newer.timestamp) {
            return newer; // Same time, no interpolation needed
        }

        // Calculate interpolation factor (0.0 to 1.0)
        float t = (float) (renderTime - older.timestamp) / (float) (newer.timestamp - older.timestamp);
        t = Math.max(0, Math.min(1, t)); // Clamp to [0, 1]

        // Interpolate position
        float x = older.x + (newer.x - older.x) * t;
        float y = older.y + (newer.y - older.y) * t;

        // Interpolate velocity
        float vx = older.vx + (newer.vx - older.vx) * t;
        float vy = older.vy + (newer.vy - older.vy) * t;

        // Use newer snapshot's discrete values (animation, health)
        return new EntitySnapshot(
            renderTime,
            x, y, vx, vy,
            newer.animation,
            newer.direction,
            newer.flipX,
            newer.health,
            newer.maxHealth
        );
    }

    /**
     * Extrapolate from a snapshot using velocity.
     * Used when we don't have a newer snapshot yet.
     * @param snapshot The snapshot to extrapolate from
     * @param renderTime The future time to extrapolate to
     * @param maxExtrapolationMs Maximum time to extrapolate (prevents wild guesses)
     * @return Extrapolated snapshot
     */
    public static EntitySnapshot extrapolate(EntitySnapshot snapshot, long renderTime, long maxExtrapolationMs) {
        long deltaMs = renderTime - snapshot.timestamp;
        if (deltaMs > maxExtrapolationMs) {
            deltaMs = maxExtrapolationMs; // Cap extrapolation
        }

        float deltaSec = deltaMs / 1000f;
        float x = snapshot.x + snapshot.vx * deltaSec;
        float y = snapshot.y + snapshot.vy * deltaSec;

        return new EntitySnapshot(
            renderTime,
            x, y,
            snapshot.vx, snapshot.vy,
            snapshot.animation,
            snapshot.direction,
            snapshot.flipX,
            snapshot.health,
            snapshot.maxHealth
        );
    }
}
