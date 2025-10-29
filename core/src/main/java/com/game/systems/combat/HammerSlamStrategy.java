package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Attack strategy for overhead slam/smash attacks.
 * Used by hammers, maces, and similar heavy weapons.
 *
 * Creates a circular area-of-effect hitbox around the impact point.
 */
public class HammerSlamStrategy implements AttackStrategy {

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

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        // Get weapon angle to determine impact point
        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        float angleRad = (float) Math.toRadians(weaponAngle);

        // Impact point is in front of attacker
        float impactDistance = weapon.getRange() * 0.6f; // Impact closer to attacker
        float impactX = attackerCenterX + (float) Math.cos(angleRad) * impactDistance;
        float impactY = attackerCenterY + (float) Math.sin(angleRad) * impactDistance;

        // Area-of-effect radius (larger than range to hit multiple targets)
        float aoeRadius = weapon.getRange() * 0.8f;

        // Check each potential target
        for (GameObject target : potentialTargets) {
            if (target == attacker) continue;
            if (!target.isActive()) continue; // Skip inactive targets
            if (attackComponent.hasHitEntity(target)) continue;

            ColliderComponent targetCollider = target.getComponent(ColliderComponent.class);
            Transform targetTransform = target.getComponent(Transform.class);

            if (targetCollider == null || targetTransform == null) continue;

            // Get target center
            Rectangle targetBounds = targetCollider.getBounds(target);
            float targetCenterX = targetBounds.x + targetBounds.width / 2f;
            float targetCenterY = targetBounds.y + targetBounds.height / 2f;

            // Check if target is within AoE radius of impact point
            float dx = targetCenterX - impactX;
            float dy = targetCenterY - impactY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= aoeRadius) {
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

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        float angleRad = (float) Math.toRadians(weaponAngle);

        // Impact point - during ACTIVE phase, hammer hits the ground in front
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float impactDistance = (phase == AttackComponent.AttackPhase.ACTIVE)
            ? weapon.getRange() * 0.6f
            : 0f; // Overhead during windup, no forward distance

        float impactX = attackerCenterX + (float) Math.cos(angleRad) * impactDistance;
        float impactY = attackerCenterY + (float) Math.sin(angleRad) * impactDistance;

        // Area of effect radius
        float aoeRadius = weapon.getRange() * 0.8f;

        // Return approximate rectangular hitbox (circle approximation)
        float size = aoeRadius * 2f;
        return new Rectangle(impactX - aoeRadius, impactY - aoeRadius, size, size);
    }

    @Override
    public float getWeaponAngleOffset(AttackComponent attackComponent, WeaponStats weapon) {
        // Hammer slam: lift overhead, then slam down
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float progress = attackComponent.getPhaseProgress();

        switch (phase) {
            case WINDUP:
                // Lift hammer overhead (perpendicular to attack direction)
                // Smoothly rotate 90° upward
                return 90f * progress;

            case ACTIVE:
                // Slam down from overhead to impact point
                // Rotate from 90° (overhead) through 0° (forward) to -30° (down impact)
                float startAngle = 90f;
                float endAngle = -30f; // Slight downward angle at impact
                return startAngle + (endAngle - startAngle) * progress;

            case RECOVERY:
                // Return to neutral from impact position
                return -30f * (1f - progress);

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

        float attackerCenterX = attackerTransform.getX() + 8f;
        float attackerCenterY = attackerTransform.getY() + 8f;

        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = getWeaponAngleOffset(attackComponent, weapon);
        float weaponAngle = baseAngle + angleOffset;
        float angleRad = (float) Math.toRadians(weaponAngle);

        // Calculate impact point
        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float impactDistance = (phase == AttackComponent.AttackPhase.ACTIVE)
            ? weapon.getRange() * 0.6f
            : 0f; // Overhead during windup, no forward distance

        float impactX = attackerCenterX + (float) Math.cos(angleRad) * impactDistance;
        float impactY = attackerCenterY + (float) Math.sin(angleRad) * impactDistance;

        // Area of effect radius
        float aoeRadius = weapon.getRange() * 0.8f;

        // Create octagon (8 vertices) to approximate a circle
        int numVertices = 8;
        float[] vertices = new float[numVertices * 2]; // x, y for each vertex

        for (int i = 0; i < numVertices; i++) {
            float angle = (float) (2 * Math.PI * i / numVertices);
            vertices[i * 2] = impactX + (float) Math.cos(angle) * aoeRadius;
            vertices[i * 2 + 1] = impactY + (float) Math.sin(angle) * aoeRadius;
        }

        return vertices;
    }

    @Override
    public String getName() {
        return "HammerSlam";
    }
}
