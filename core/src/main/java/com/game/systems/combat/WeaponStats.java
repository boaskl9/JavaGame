package com.game.systems.combat;

/**
 * Combat statistics for weapons.
 * Immutable data class that defines weapon behavior, attack patterns, and timing.
 */
public class WeaponStats {
    private final WeaponType type;      // Type of weapon (SWORD, SPEAR, etc.)
    private final int damage;           // Base damage per hit
    private final float attackSpeed;    // Attacks per second (1.0 = 1 attack/sec)
    private final float range;          // Attack reach in pixels
    private final float knockback;      // Knockback force applied to hit entities

    // Attack pattern timing
    private final float windupDuration;    // Time before attack becomes active (seconds)
    private final float swingDuration;     // Time during damage-dealing phase (seconds)
    private final float recoveryDuration;  // Time after attack before returning to idle (seconds)

    // Attack geometry
    private final float swingArc;          // Total arc angle for swing attacks (degrees, e.g., 120°)

    // Movement penalty
    private final float movementMultiplier; // Speed multiplier while attacking (0.0-1.0, e.g., 0.5 = half speed)

    /**
     * Creates weapon stats with full attack pattern configuration.
     *
     * @param type Weapon type
     * @param damage Base damage dealt per hit
     * @param attackSpeed Attacks per second (e.g., 1.0 = 1 attack/sec, 2.0 = 2 attacks/sec)
     * @param range Attack hitbox reach in pixels
     * @param knockback Knockback force (pixels per second applied to velocity)
     * @param windupDuration Windup phase duration in seconds
     * @param swingDuration Active damage phase duration in seconds
     * @param recoveryDuration Recovery phase duration in seconds
     * @param swingArc Total arc angle for swing attacks in degrees
     * @param movementMultiplier Movement speed multiplier during attack (0.0-1.0)
     */
    public WeaponStats(WeaponType type, int damage, float attackSpeed, float range, float knockback,
                       float windupDuration, float swingDuration, float recoveryDuration,
                       float swingArc, float movementMultiplier) {
        this.type = type;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.range = range;
        this.knockback = knockback;
        this.windupDuration = windupDuration;
        this.swingDuration = swingDuration;
        this.recoveryDuration = recoveryDuration;
        this.swingArc = swingArc;
        this.movementMultiplier = Math.max(0f, Math.min(1f, movementMultiplier)); // Clamp to 0-1
    }

    /**
     * Legacy constructor for backwards compatibility.
     * Uses default timing values based on attack speed.
     */
    public WeaponStats(int damage, float attackSpeed, float range, float knockback) {
        this(
            WeaponType.SWORD, // Default to sword
            damage,
            attackSpeed,
            range,
            knockback,
            0.15f,  // Default windup
            0.3f,   // Default swing
            0.15f,  // Default recovery
            120f,   // Default arc (120 degrees)
            0.5f    // Default movement penalty (half speed)
        );
    }

    /**
     * Gets the cooldown duration between attacks in seconds.
     * This is the time from attack start to when next attack can begin.
     */
    public float getCooldown() {
        // Total attack time is windup + swing + recovery, but attackSpeed overrides this
        // We use whichever is longer: the attack speed cooldown or the total attack duration
        float attackSpeedCooldown = 1.0f / attackSpeed;
        float totalAttackDuration = windupDuration + swingDuration + recoveryDuration;
        return Math.max(attackSpeedCooldown - totalAttackDuration, 0.1f);
    }

    /**
     * Gets the total duration of the entire attack (all phases except cooldown).
     */
    public float getTotalAttackDuration() {
        return windupDuration + swingDuration + recoveryDuration;
    }

    // Getters

    public WeaponType getType() {
        return type;
    }

    public int getDamage() {
        return damage;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public float getRange() {
        return range;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getWindupDuration() {
        return windupDuration;
    }

    public float getSwingDuration() {
        return swingDuration;
    }

    public float getRecoveryDuration() {
        return recoveryDuration;
    }

    public float getSwingArc() {
        return swingArc;
    }

    public float getMovementMultiplier() {
        return movementMultiplier;
    }

    @Override
    public String toString() {
        return "WeaponStats{" +
                "type=" + type +
                ", damage=" + damage +
                ", attackSpeed=" + attackSpeed +
                ", range=" + range +
                ", knockback=" + knockback +
                ", windupDuration=" + windupDuration +
                ", swingDuration=" + swingDuration +
                ", recoveryDuration=" + recoveryDuration +
                ", swingArc=" + swingArc +
                ", movementMultiplier=" + movementMultiplier +
                ", cooldown=" + getCooldown() +
                '}';
    }
}

