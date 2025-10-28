package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.components.VelocityComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Utility class for combat calculations.
 * Provides helper methods for hitboxes, damage, and knockback.
 */
public class CombatUtils {

    /**
     * Creates a directional attack hitbox based on attacker position and direction.
     * The hitbox extends from the attacker in the attack direction.
     *
     * @param attackerX Attacker X position
     * @param attackerY Attacker Y position
     * @param attackerWidth Attacker width
     * @param attackerHeight Attacker height
     * @param direction Attack direction in degrees (0=right, 90=up, 180=left, 270=down)
     * @param range Attack reach in pixels
     * @return Rectangle representing the attack hitbox
     */
    public static Rectangle createDirectionalHitbox(float attackerX, float attackerY,
                                                    float attackerWidth, float attackerHeight,
                                                    float direction, float range) {
        // Normalize direction to 0-360
        direction = direction % 360;
        if (direction < 0) direction += 360;

        // Convert to radians
        float radians = (float) Math.toRadians(direction);

        // Calculate direction vector
        float dirX = (float) Math.cos(radians);
        float dirY = (float) Math.sin(radians);

        // Hitbox dimensions
        float hitboxWidth = range;
        float hitboxHeight = Math.max(attackerWidth, attackerHeight) * 0.8f; // Narrower than attacker

        // Calculate hitbox position (extends from attacker in attack direction)
        float centerX = attackerX + attackerWidth / 2f;
        float centerY = attackerY + attackerHeight / 2f;

        // Offset hitbox to start at attacker edge and extend outward
        float offsetX = dirX * (attackerWidth / 2f);
        float offsetY = dirY * (attackerHeight / 2f);

        // For simplicity, create an axis-aligned bounding box
        // More sophisticated version would rotate the hitbox
        float hitboxX = centerX + offsetX - hitboxWidth / 2f + dirX * range / 2f;
        float hitboxY = centerY + offsetY - hitboxHeight / 2f + dirY * range / 2f;

        return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    /**
     * Creates an arc-shaped attack hitbox for enemy attacks.
     * The arc covers the enemy plus a forward-facing cone toward a target.
     *
     * This method checks if a point (target) is within the arc attack range.
     *
     * @param attackerX Attacker center X
     * @param attackerY Attacker center Y
     * @param targetX Target X position
     * @param targetY Target Y position
     * @param attackerRadius Attacker size radius
     * @param attackRange Additional range beyond attacker
     * @param arcAngleDegrees Arc width in degrees (e.g., 120 for 120° cone)
     * @return true if target is within arc attack
     */
    public static boolean isInArcAttack(float attackerX, float attackerY,
                                       float targetX, float targetY,
                                       float attackerRadius, float attackRange,
                                       float arcAngleDegrees) {
        // Calculate distance to target
        float dx = targetX - attackerX;
        float dy = targetY - attackerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Check if within range (attacker size + attack range)
        float maxRange = attackerRadius + attackRange;
        if (distance > maxRange) {
            return false; // Too far away
        }

        // Calculate angle to target
        float angleToTarget = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Calculate direction attacker is facing (toward target)
        float facingDirection = angleToTarget;

        // Check if target is within arc angle
        // Arc is centered on facingDirection, extends ±arcAngle/2
        float halfArc = arcAngleDegrees / 2f;

        // Normalize angles to -180 to 180
        angleToTarget = normalizeAngle(angleToTarget);
        facingDirection = normalizeAngle(facingDirection);

        // Calculate angular difference
        float angleDiff = Math.abs(normalizeAngle(angleToTarget - facingDirection));

        return angleDiff <= halfArc;
    }

    /**
     * Normalizes an angle to the range -180 to 180 degrees.
     */
    private static float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Applies knockback to a GameObject.
     * Calculates knockback direction from attacker to target and applies force.
     *
     * @param attacker The attacking entity
     * @param target The target entity
     * @param knockbackForce Knockback force to apply
     */
    public static void applyKnockback(GameObject attacker, GameObject target, float knockbackForce) {
        if (knockbackForce <= 0) return;

        VelocityComponent targetVelocity = target.getComponent(VelocityComponent.class);
        if (targetVelocity == null) return;

        Transform attackerTransform = attacker.getComponent(Transform.class);
        Transform targetTransform = target.getComponent(Transform.class);

        // Calculate knockback direction (away from attacker)
        float dx = targetTransform.getX() - attackerTransform.getX();
        float dy = targetTransform.getY() - attackerTransform.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance < 0.01f) {
            // Entities are overlapping, push in random direction
            dx = 1;
            dy = 0;
            distance = 1;
        }

        // Normalize direction
        float dirX = dx / distance;
        float dirY = dy / distance;

        // Apply knockback (add to current velocity)
        targetVelocity.addVelocity(dirX * knockbackForce, dirY * knockbackForce);
    }

    /**
     * Calculates the angle from one point to another.
     * Returns angle in degrees (0=right, 90=up, 180=left, 270=down).
     */
    public static float calculateAngle(float fromX, float fromY, float toX, float toY) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    /**
     * Calculates the angle from one position to another using Transform.
     */
    public static float calculateAngle(Transform from, Transform to) {
        return calculateAngle(from.getX(), from.getY(), to.getX(), to.getY());
    }

    /**
     * Calculates the angle from attacker to mouse cursor position.
     */
    public static float calculateAngleToMouse(float attackerX, float attackerY,
                                             float mouseWorldX, float mouseWorldY) {
        return calculateAngle(attackerX, attackerY, mouseWorldX, mouseWorldY);
    }

    /**
     * Gets the directional animation angle from an attack angle.
     * Converts continuous angle to 4-directional angles (0, 90, 180, 270).
     */
    public static int getDirectionalAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;

        // Map to 4 directions
        if (angle >= 315 || angle < 45) {
            return 270; // Right
        } else if (angle >= 45 && angle < 135) {
            return 0; // Up
        } else if (angle >= 135 && angle < 225) {
            return 90; // Left
        } else {
            return 180; // Down
        }
    }

    /**
     * Checks if a point is within an arc attack centered on a specific angle.
     *
     * @param centerX Arc center X position
     * @param centerY Arc center Y position
     * @param targetX Target point X
     * @param targetY Target point Y
     * @param centerAngle Center angle of the arc in degrees
     * @param range Maximum distance from center
     * @param arcWidth Total arc width in degrees (e.g., 120)
     * @return true if target is within the arc
     */
    public static boolean isPointInArc(float centerX, float centerY,
                                      float targetX, float targetY,
                                      float centerAngle, float range, float arcWidth) {
        // Calculate distance to target
        float dx = targetX - centerX;
        float dy = targetY - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Check if within range
        if (distance > range) {
            return false;
        }

        // Calculate angle to target
        float angleToTarget = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Normalize angles
        angleToTarget = normalizeAngle(angleToTarget);
        centerAngle = normalizeAngle(centerAngle);

        // Calculate angular difference
        float angleDiff = Math.abs(normalizeAngle(angleToTarget - centerAngle));

        // Check if within arc
        return angleDiff <= arcWidth / 2f;
    }

    /**
     * Creates an arc-shaped attack hitbox based on current weapon angle.
     * This is an improved version that creates a proper arc hitbox.
     *
     * @param centerX Attack center X
     * @param centerY Attack center Y
     * @param weaponAngle Current weapon angle in degrees
     * @param range Attack range
     * @param arcWidth Arc width in degrees
     * @return Approximate rectangular hitbox (for debug visualization)
     */
    public static Rectangle createArcHitbox(float centerX, float centerY,
                                           float weaponAngle, float range, float arcWidth) {
        // Convert to radians
        float angleRad = (float) Math.toRadians(weaponAngle);

        // Calculate direction vector
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Create approximate rectangular hitbox extending in weapon direction
        float hitboxWidth = range;
        float hitboxHeight = Math.max(12f, arcWidth * 0.3f);

        float hitboxX = centerX + dirX * range / 2f - hitboxWidth / 2f;
        float hitboxY = centerY + dirY * range / 2f - hitboxHeight / 2f;

        return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }
}
