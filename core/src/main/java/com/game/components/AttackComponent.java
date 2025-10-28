package com.game.components;

import com.game.systems.combat.WeaponStats;
import com.game.systems.entity.Component;
import com.game.systems.entity.GameObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Component that manages attack state, phases, and cooldowns.
 * Supports multi-phase attacks with windup, active damage, and recovery.
 */
public class AttackComponent implements Component {

    /**
     * Attack phases for more sophisticated attack timing.
     */
    public enum AttackPhase {
        IDLE,       // Not attacking, can start new attack
        WINDUP,     // Preparing attack (weapon pulls back)
        ACTIVE,     // Damage-dealing phase (weapon swings through arc)
        RECOVERY,   // Attack finishing (weapon returns to rest)
        COOLDOWN    // Waiting before next attack can start
    }

    private AttackPhase phase;
    private float phaseTimer;
    private float attackDirection; // Base attack direction in degrees (0=right, 90=up, etc.)
    private WeaponStats currentWeapon;

    // Arc attack tracking
    private float arcStartAngle;   // Where arc swing starts (relative to attack direction)
    private float arcEndAngle;     // Where arc swing ends
    private float currentArcAngle; // Current angle during swing

    // Hit tracking - prevents hitting same entity multiple times in one attack
    private Set<GameObject> hitEntities;

    public AttackComponent() {
        this.phase = AttackPhase.IDLE;
        this.phaseTimer = 0f;
        this.attackDirection = 0f;
        this.currentWeapon = null;
        this.hitEntities = new HashSet<>();
        this.arcStartAngle = 0f;
        this.arcEndAngle = 0f;
        this.currentArcAngle = 0f;
    }

    /**
     * Updates the attack component, advancing through attack phases.
     * @param delta Time since last frame in seconds
     */
    public void update(float delta) {
        if (phase == AttackPhase.IDLE) {
            return;
        }

        if (currentWeapon == null) {
            reset();
            return;
        }

        phaseTimer += delta;

        switch (phase) {
            case WINDUP:
                // Update arc angle during windup (weapon pulls back)
                float windupProgress = Math.min(1f, phaseTimer / currentWeapon.getWindupDuration());
                // During windup, weapon moves from rest (0) to start position (negative arc)
                currentArcAngle = arcStartAngle * windupProgress;

                if (phaseTimer >= currentWeapon.getWindupDuration()) {
                    // Transition to active phase
                    phase = AttackPhase.ACTIVE;
                    phaseTimer = 0f;
                    currentArcAngle = arcStartAngle;
                }
                break;

            case ACTIVE:
                // Update arc angle during active swing
                float activeProgress = Math.min(1f, phaseTimer / currentWeapon.getSwingDuration());
                // Swing from start angle to end angle
                currentArcAngle = arcStartAngle + (arcEndAngle - arcStartAngle) * activeProgress;

                if (phaseTimer >= currentWeapon.getSwingDuration()) {
                    // Transition to recovery
                    phase = AttackPhase.RECOVERY;
                    phaseTimer = 0f;
                }
                break;

            case RECOVERY:
                // Update arc angle during recovery (weapon returns to rest)
                float recoveryProgress = Math.min(1f, phaseTimer / currentWeapon.getRecoveryDuration());
                // Return from end angle back to 0 (rest position)
                currentArcAngle = arcEndAngle * (1f - recoveryProgress);

                if (phaseTimer >= currentWeapon.getRecoveryDuration()) {
                    // Transition to cooldown
                    phase = AttackPhase.COOLDOWN;
                    phaseTimer = 0f;
                }
                break;

            case COOLDOWN:
                if (phaseTimer >= currentWeapon.getCooldown()) {
                    // Attack complete, return to idle
                    reset();
                }
                break;
        }
    }

    /**
     * Attempts to start an attack.
     * @param weapon The weapon being used
     * @param direction Base attack direction in degrees
     * @return true if attack started, false if already attacking/on cooldown
     */
    public boolean startAttack(WeaponStats weapon, float direction) {
        if (phase != AttackPhase.IDLE) {
            return false; // Can't attack while already attacking or on cooldown
        }

        this.currentWeapon = weapon;
        this.attackDirection = direction;
        this.phase = AttackPhase.WINDUP;
        this.phaseTimer = 0f;
        this.hitEntities.clear();

        // Calculate arc angles based on weapon
        float halfArc = weapon.getSwingArc() / 2f;
        this.arcStartAngle = -halfArc;  // Start at -60° (for 120° arc)
        this.arcEndAngle = halfArc;     // End at +60°
        this.currentArcAngle = 0f;

        return true;
    }

    /**
     * Checks if entity can currently start a new attack.
     */
    public boolean canAttack() {
        return phase == AttackPhase.IDLE;
    }

    /**
     * Checks if currently in any attack phase (not idle/cooldown).
     */
    public boolean isAttacking() {
        return phase == AttackPhase.WINDUP ||
               phase == AttackPhase.ACTIVE ||
               phase == AttackPhase.RECOVERY;
    }

    /**
     * Checks if in the active damage-dealing phase.
     */
    public boolean isInActivePhase() {
        return phase == AttackPhase.ACTIVE;
    }

    /**
     * Marks an entity as hit during this attack (prevents double-hitting).
     */
    public void markEntityHit(GameObject entity) {
        hitEntities.add(entity);
    }

    /**
     * Checks if an entity has already been hit during this attack.
     */
    public boolean hasHitEntity(GameObject entity) {
        return hitEntities.contains(entity);
    }

    /**
     * Forces attack state to IDLE (for interrupts or resets).
     */
    public void reset() {
        phase = AttackPhase.IDLE;
        phaseTimer = 0f;
        hitEntities.clear();
        currentArcAngle = 0f;
    }

    /**
     * Gets the absolute angle of the weapon in world space.
     * This is the base attack direction + current arc offset.
     */
    public float getAbsoluteWeaponAngle() {
        return attackDirection + currentArcAngle;
    }

    /**
     * Gets the progress through the current phase (0.0 to 1.0).
     */
    public float getPhaseProgress() {
        if (currentWeapon == null || phase == AttackPhase.IDLE) {
            return 0f;
        }

        float phaseDuration = 0f;
        switch (phase) {
            case WINDUP:
                phaseDuration = currentWeapon.getWindupDuration();
                break;
            case ACTIVE:
                phaseDuration = currentWeapon.getSwingDuration();
                break;
            case RECOVERY:
                phaseDuration = currentWeapon.getRecoveryDuration();
                break;
            case COOLDOWN:
                phaseDuration = currentWeapon.getCooldown();
                break;
        }

        return phaseDuration > 0 ? Math.min(1f, phaseTimer / phaseDuration) : 0f;
    }

    // Getters

    public AttackPhase getPhase() {
        return phase;
    }

    public float getPhaseTimer() {
        return phaseTimer;
    }

    public float getAttackDirection() {
        return attackDirection;
    }

    public WeaponStats getCurrentWeapon() {
        return currentWeapon;
    }

    public float getCurrentArcAngle() {
        return currentArcAngle;
    }

    public float getArcStartAngle() {
        return arcStartAngle;
    }

    public float getArcEndAngle() {
        return arcEndAngle;
    }

    public float getCooldownPercent() {
        if (phase != AttackPhase.COOLDOWN || currentWeapon == null) {
            return 0f;
        }
        return phaseTimer / currentWeapon.getCooldown();
    }
}
