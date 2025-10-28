package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Attack strategy for arc-based swing attacks.
 * Used by swords, axes, and similar melee weapons that swing in an arc.
 *
 * The attack sweeps through an arc over time, hitting enemies within the arc.
 */
public class ArcSwingStrategy implements AttackStrategy {

    @Override
    public List<GameObject> executeAttack(GameObject attacker,
                                         AttackComponent attackComponent,
                                         WeaponStats weapon,
                                         List<GameObject> potentialTargets) {
        List<GameObject> hitEntities = new ArrayList<>();

        Transform attackerTransform = attacker.getComponent(Transform.class);
        if (attackerTransform == null) {
            return hitEntities; // Can't attack without transform
        }

        // Get attacker center position
        float attackerCenterX = attackerTransform.getX() + 8f; // Assuming 16x16 sprite
        float attackerCenterY = attackerTransform.getY() + 8f;

        // Get current weapon angle in world space
        float weaponAngle = attackComponent.getAbsoluteWeaponAngle();

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue; // Don't hit self
            if (attackComponent.hasHitEntity(target)) continue; // Already hit this attack

            // Get target collider and position
            ColliderComponent targetCollider = target.getComponent(ColliderComponent.class);
            Transform targetTransform = target.getComponent(Transform.class);

            if (targetCollider == null || targetTransform == null) continue;

            // Get target bounds
            Rectangle targetBounds = targetCollider.getBounds(target);
            float targetCenterX = targetBounds.x + targetBounds.width / 2f;
            float targetCenterY = targetBounds.y + targetBounds.height / 2f;

            // Check if target is within range and arc
            if (isInArc(attackerCenterX, attackerCenterY,
                       targetCenterX, targetCenterY,
                       weaponAngle, weapon.getRange(), weapon.getSwingArc())) {
                hitEntities.add(target);
                attackComponent.markEntityHit(target); // Mark as hit to prevent double-hitting
            }
        }

        return hitEntities;
    }

    @Override
    public Rectangle getHitbox(GameObject attacker,
                              AttackComponent attackComponent,
                              WeaponStats weapon) {
        Transform attackerTransform = attacker.getComponent(Transform.class);
        if (attackerTransform == null) {
            return new Rectangle(0, 0, 0, 0);
        }

        // Create approximate rectangular hitbox for visualization
        float weaponAngle = attackComponent.getAbsoluteWeaponAngle();
        float angleRad = (float) Math.toRadians(weaponAngle);

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        // Calculate hitbox position extending from attacker in weapon direction
        float range = weapon.getRange();
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Create rectangle extending from attacker
        float hitboxWidth = range;
        float hitboxHeight = weapon.getSwingArc() * 0.3f; // Approximate height based on arc

        float hitboxX = attackerCenterX + dirX * range / 2f - hitboxWidth / 2f;
        float hitboxY = attackerCenterY + dirY * range / 2f - hitboxHeight / 2f;

        return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    @Override
    public String getName() {
        return "ArcSwing";
    }

    /**
     * Checks if a point is within an arc attack.
     *
     * @param attackerX Attacker X position
     * @param attackerY Attacker Y position
     * @param targetX Target X position
     * @param targetY Target Y position
     * @param weaponAngle Current weapon angle in degrees
     * @param range Maximum attack range
     * @param arcWidth Total arc width in degrees
     * @return true if target is within the attack arc
     */
    private boolean isInArc(float attackerX, float attackerY,
                           float targetX, float targetY,
                           float weaponAngle, float range, float arcWidth) {
        // Calculate distance to target
        float dx = targetX - attackerX;
        float dy = targetY - attackerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Check if within range
        if (distance > range) {
            return false;
        }

        // Calculate angle to target
        float angleToTarget = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Normalize angles to -180 to 180
        angleToTarget = normalizeAngle(angleToTarget);
        weaponAngle = normalizeAngle(weaponAngle);

        // Calculate angular difference
        float angleDiff = Math.abs(normalizeAngle(angleToTarget - weaponAngle));

        // Check if within arc (we use a narrower arc for actual hits than the visual swing)
        // This makes the combat feel more precise
        float hitArc = arcWidth * 0.4f; // Only 40% of the visual arc actually deals damage
        return angleDiff <= hitArc / 2f;
    }

    /**
     * Normalizes an angle to the range -180 to 180 degrees.
     */
    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }
}
