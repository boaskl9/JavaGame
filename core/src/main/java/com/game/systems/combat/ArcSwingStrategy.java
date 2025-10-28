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
        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;

        // Create rotating hitbox parameters
        float range = weapon.getRange();
        float hitboxWidth = range;
        float hitboxHeight = 12f; // Hitbox thickness

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue; // Don't hit self
            if (attackComponent.hasHitEntity(target)) continue; // Already hit this attack

            // Get target collider and position
            ColliderComponent targetCollider = target.getComponent(ColliderComponent.class);
            Transform targetTransform = target.getComponent(Transform.class);

            if (targetCollider == null || targetTransform == null) continue;

            // Get target center
            Rectangle targetBounds = targetCollider.getBounds(target);
            float targetCenterX = targetBounds.x + targetBounds.width / 2f;
            float targetCenterY = targetBounds.y + targetBounds.height / 2f;

            // Check if target is within the rotating hitbox
            if (isPointInRotatedRectangle(
                    targetCenterX, targetCenterY,
                    attackerCenterX, attackerCenterY,
                    hitboxWidth, hitboxHeight,
                    weaponAngle)) {
                hitEntities.add(target);
                attackComponent.markEntityHit(target);
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

        // Note: This creates an axis-aligned bounding box for debug visualization.
        // The actual collision detection uses a rotated rectangle.
        // This is an approximation that shows the general area of the attack.

        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        float angleRad = (float) Math.toRadians(weaponAngle);

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        float range = weapon.getRange();
        float hitboxHeight = 12f; // Same as actual collision hitbox thickness

        // Calculate the four corners of the rotated rectangle
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);
        float perpX = -dirY; // Perpendicular vector
        float perpY = dirX;

        // Rectangle extends from attacker center outward
        float halfHeight = hitboxHeight / 2f;

        // Four corners of the rotated rectangle
        float[] cornersX = new float[4];
        float[] cornersY = new float[4];

        // Bottom-left (start of weapon)
        cornersX[0] = attackerCenterX + perpX * -halfHeight;
        cornersY[0] = attackerCenterY + perpY * -halfHeight;

        // Bottom-right (start of weapon)
        cornersX[1] = attackerCenterX + perpX * halfHeight;
        cornersY[1] = attackerCenterY + perpY * halfHeight;

        // Top-left (tip of weapon)
        cornersX[2] = attackerCenterX + dirX * range + perpX * -halfHeight;
        cornersY[2] = attackerCenterY + dirY * range + perpY * -halfHeight;

        // Top-right (tip of weapon)
        cornersX[3] = attackerCenterX + dirX * range + perpX * halfHeight;
        cornersY[3] = attackerCenterY + dirY * range + perpY * halfHeight;

        // Find min/max for axis-aligned bounding box
        float minX = Math.min(Math.min(cornersX[0], cornersX[1]), Math.min(cornersX[2], cornersX[3]));
        float maxX = Math.max(Math.max(cornersX[0], cornersX[1]), Math.max(cornersX[2], cornersX[3]));
        float minY = Math.min(Math.min(cornersY[0], cornersY[1]), Math.min(cornersY[2], cornersY[3]));
        float maxY = Math.max(Math.max(cornersY[0], cornersY[1]), Math.max(cornersY[2], cornersY[3]));

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public float getWeaponAngleOffset(AttackComponent attackComponent, WeaponStats weapon) {
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float progress = attackComponent.getPhaseProgress();
        float halfArc = weapon.getSwingArc() / 2f;

        switch (phase) {
            case WINDUP:
                // Pull weapon back to starting position
                // Smoothly move from 0° to -halfArc (e.g., 0° to -60°)
                return -halfArc * progress;

            case ACTIVE:
                // Swing from start to end
                // Move from -halfArc to +halfArc (e.g., -60° to +60°)
                return -halfArc + (halfArc * 2f * progress);

            case RECOVERY:
                // Return weapon to neutral position
                // Move from +halfArc back to 0° (e.g., +60° to 0°)
                return halfArc * (1f - progress);

            case COOLDOWN:
            case IDLE:
            default:
                return 0f; // Weapon at rest
        }
    }

    @Override
    public float[] getHitboxPolygon(GameObject attacker,
                                    AttackComponent attackComponent,
                                    WeaponStats weapon) {
        Transform attackerTransform = attacker.getComponent(Transform.class);
        if (attackerTransform == null) {
            return null;
        }

        // Calculate the 4 corners of the rotated rectangle
        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        float angleRad = (float) Math.toRadians(weaponAngle);

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        float range = weapon.getRange();
        float hitboxHeight = 12f; // Same as actual collision hitbox thickness

        // Calculate direction vectors
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);
        float perpX = -dirY; // Perpendicular vector (for width)
        float perpY = dirX;

        float halfHeight = hitboxHeight / 2f;

        // Calculate 4 corners of the rotated rectangle
        // Rectangle extends from attacker center outward in weapon direction

        // Corner 1: Bottom-left (start, -width/2)
        float x1 = attackerCenterX + perpX * -halfHeight;
        float y1 = attackerCenterY + perpY * -halfHeight;

        // Corner 2: Bottom-right (start, +width/2)
        float x2 = attackerCenterX + perpX * halfHeight;
        float y2 = attackerCenterY + perpY * halfHeight;

        // Corner 3: Top-right (tip, +width/2)
        float x3 = attackerCenterX + dirX * range + perpX * halfHeight;
        float y3 = attackerCenterY + dirY * range + perpY * halfHeight;

        // Corner 4: Top-left (tip, -width/2)
        float x4 = attackerCenterX + dirX * range + perpX * -halfHeight;
        float y4 = attackerCenterY + dirY * range + perpY * -halfHeight;

        // Return as array: [x1, y1, x2, y2, x3, y3, x4, y4]
        return new float[] { x1, y1, x2, y2, x3, y3, x4, y4 };
    }

    @Override
    public String getName() {
        return "ArcSwing";
    }

    /**
     * Checks if a point is within a rotated rectangle.
     * The rectangle extends from the attacker position in the direction of weaponAngle.
     *
     * @param pointX Point X to check
     * @param pointY Point Y to check
     * @param rectCenterX Center X of the rectangle (attacker position)
     * @param rectCenterY Center Y of the rectangle
     * @param rectWidth Width of the rectangle (weapon range)
     * @param rectHeight Height of the rectangle (hitbox thickness)
     * @param angle Rotation angle in degrees
     * @return true if point is within the rotated rectangle
     */
    private boolean isPointInRotatedRectangle(float pointX, float pointY,
                                             float rectCenterX, float rectCenterY,
                                             float rectWidth, float rectHeight,
                                             float angle) {
        // Translate point to rectangle's local space (origin at rectangle center)
        float localX = pointX - rectCenterX;
        float localY = pointY - rectCenterY;

        // Rotate point backwards by -angle to align with axis-aligned rectangle
        float angleRad = (float) Math.toRadians(-angle);
        float cosAngle = (float) Math.cos(angleRad);
        float sinAngle = (float) Math.sin(angleRad);

        float rotatedX = localX * cosAngle - localY * sinAngle;
        float rotatedY = localX * sinAngle + localY * cosAngle;

        // Rectangle extends forward from center, so it's from 0 to rectWidth in X
        // and from -rectHeight/2 to +rectHeight/2 in Y
        float halfHeight = rectHeight / 2f;

        return rotatedX >= 0 && rotatedX <= rectWidth &&
               rotatedY >= -halfHeight && rotatedY <= halfHeight;
    }
}
