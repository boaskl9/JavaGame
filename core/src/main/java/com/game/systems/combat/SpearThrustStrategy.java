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
        float weaponAngle = attackComponent.getAbsoluteWeaponAngle();

        // Create thrust hitbox (narrow rectangle extending from attacker)
        Rectangle thrustBox = createThrustHitbox(attackerTransform, weaponAngle, weapon.getRange());

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue;
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

        float weaponAngle = attackComponent.getAbsoluteWeaponAngle();
        return createThrustHitbox(attackerTransform, weaponAngle, weapon.getRange());
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
