package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Attack strategy for quick stab/jab attacks.
 * Used by daggers and similar fast, precise weapons.
 *
 * Creates a small, precise hitbox with a quick forward motion.
 */
public class StabStrategy implements AttackStrategy {

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

        // Create small stab hitbox
        Rectangle stabBox = createStabHitbox(attackerTransform, weaponAngle, weapon.getRange());

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue;
            if (attackComponent.hasHitEntity(target)) continue;

            ColliderComponent targetCollider = target.getComponent(ColliderComponent.class);
            if (targetCollider == null) continue;

            Rectangle targetBounds = targetCollider.getBounds(target);

            // Check if target overlaps with stab hitbox
            if (stabBox.overlaps(targetBounds)) {
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
        return createStabHitbox(attackerTransform, weaponAngle, weapon.getRange());
    }

    @Override
    public float getWeaponAngleOffset(AttackComponent attackComponent, WeaponStats weapon) {
        // Stab attacks have minimal angle change, just a quick jab
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float progress = attackComponent.getPhaseProgress();

        switch (phase) {
            case WINDUP:
                // Minimal pullback, slight downward angle
                return -10f * progress; // Pull down 10 degrees

            case ACTIVE:
                // Quick snap back to forward and slightly up for emphasis
                return -10f + 15f * progress; // Move from -10° to +5°

            case RECOVERY:
                // Return to neutral
                return 5f * (1f - progress); // Return from +5° to 0°

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
        // TODO: Implement rotating polygon for dagger
        return null;
    }

    @Override
    public String getName() {
        return "Stab";
    }

    /**
     * Creates a small, precise hitbox for stab attacks.
     */
    private Rectangle createStabHitbox(Transform attackerTransform, float weaponAngle, float range) {
        float angleRad = (float) Math.toRadians(weaponAngle);

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Stab hitbox is small and precise
        float stabWidth = range;
        float stabHeight = 6f; // Very narrow hitbox

        // Position hitbox extending from attacker
        float hitboxX = attackerCenterX + dirX * (stabWidth / 2f + 4f) - stabWidth / 2f;
        float hitboxY = attackerCenterY + dirY * (stabWidth / 2f + 4f) - stabHeight / 2f;

        return new Rectangle(hitboxX, hitboxY, stabWidth, stabHeight);
    }
}
