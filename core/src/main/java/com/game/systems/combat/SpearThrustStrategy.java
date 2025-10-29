package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Attack strategy for thrust/poke attacks.
 * Used by spears, rapiers, and similar weapons that attack in a straight line.
 *
 * Creates a narrow rectangular hitbox extending from the attacker.
 */
public class SpearThrustStrategy implements AttackStrategy {

    @Override
    public List<GameObject> executeAttack(GameObject attacker,
                                         AttackComponent attackComponent,
                                         WeaponStats weapon,
                                         List<GameObject> potentialTargets) {
        List<GameObject> hitEntities = new ArrayList<>();

        Transform attackerTransform = attacker.getComponent(Transform.class);
        if (attackerTransform == null) {
            return hitEntities;
        }

        // Get current weapon angle
        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;

        // Create thrust hitbox (narrow rectangle extending from attacker)
        Rectangle thrustBox = createThrustHitbox(attackerTransform, weaponAngle, weapon.getRange());

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue;
            if (!target.isActive()) continue; // Skip inactive targets
            if (attackComponent.hasHitEntity(target)) continue;

            ColliderComponent targetCollider = target.getComponent(ColliderComponent.class);
            if (targetCollider == null) continue;

            Rectangle targetBounds = targetCollider.getBounds(target);

            // Check if target overlaps with thrust hitbox
            if (thrustBox.overlaps(targetBounds)) {
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

        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        return createThrustHitbox(attackerTransform, weaponAngle, weapon.getRange());
    }

    @Override
    public float getWeaponAngleOffset(AttackComponent attackComponent, WeaponStats weapon) {
        // Thrust attacks don't change angle much - the weapon points forward throughout
        // We can add subtle angle changes for visual variety
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float progress = attackComponent.getPhaseProgress();

        switch (phase) {
            case WINDUP:
                // Slightly lower the weapon during windup
                return -5f * progress; // Dip down 5 degrees

            case ACTIVE:
                // Return to forward position during thrust
                return -5f * (1f - progress); // Return from -5° to 0°

            case RECOVERY:
            case COOLDOWN:
            case IDLE:
            default:
                return 0f; // Weapon at rest, pointing forward
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
        float thrustHeight = 8f; // Narrow thrust hitbox

        // Calculate direction vectors
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);
        float perpX = -dirY; // Perpendicular vector (for width)
        float perpY = dirX;

        float halfHeight = thrustHeight / 2f;

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
        return "SpearThrust";
    }

    /**
     * Creates a narrow rectangular hitbox extending from the attacker.
     */
    private Rectangle createThrustHitbox(Transform attackerTransform, float weaponAngle, float range) {
        float angleRad = (float) Math.toRadians(weaponAngle);

        float attackerCenterX = attackerTransform.getX() + 8f; // Assuming 16x16 sprite
        float attackerCenterY = attackerTransform.getY() + 8f;

        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Thrust hitbox is narrow (8 pixels wide) and extends forward
        float thrustWidth = range;
        float thrustHeight = 8f; // Narrow hitbox

        // Calculate rotated hitbox corners (approximate with axis-aligned box for simplicity)
        // Position hitbox extending from attacker in thrust direction
        float hitboxX = attackerCenterX + dirX * (thrustWidth / 2f + 4f) - thrustWidth / 2f;
        float hitboxY = attackerCenterY + dirY * (thrustWidth / 2f + 4f) - thrustHeight / 2f;

        return new Rectangle(hitboxX, hitboxY, thrustWidth, thrustHeight);
    }
}
