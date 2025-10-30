package com.game.systems.combat;

import com.game.components.AttackComponent;
import com.game.systems.entity.Entity;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified attack system that handles attack execution for all entities.
 * Manages attack strategies, hit detection, damage application, and knockback.
 *
 * This system is used by both player and enemies for consistent combat behavior.
 */
public class AttackSystem {

    // Strategy registry: maps weapon types to attack strategies
    private static final Map<WeaponType, AttackStrategy> strategies = new HashMap<>();

    static {
        // Register default strategies
        ArcSwingStrategy arcSwing = new ArcSwingStrategy();
        strategies.put(WeaponType.SWORD, arcSwing);
        strategies.put(WeaponType.AXE, arcSwing);
        strategies.put(WeaponType.WHIP, arcSwing);
        strategies.put(WeaponType.CLAW, arcSwing); // Claws use arc swings with smaller arc

        strategies.put(WeaponType.DAGGER, new StabStrategy());
        strategies.put(WeaponType.SPEAR, new SpearThrustStrategy());
        strategies.put(WeaponType.HAMMER, new HammerSlamStrategy());
        strategies.put(WeaponType.STAFF, new SpearThrustStrategy()); // Staff uses thrust-like attacks

        // BOW will need projectile strategy in the future
    }

    /**
     * Registers a custom attack strategy for a weapon type.
     */
    public static void registerStrategy(WeaponType type, AttackStrategy strategy) {
        strategies.put(type, strategy);
    }

    /**
     * Gets the attack strategy for a weapon type.
     * Falls back to ArcSwingStrategy if no strategy is registered.
     */
    public static AttackStrategy getStrategy(WeaponType type) {
        return strategies.getOrDefault(type, new ArcSwingStrategy());
    }

    /**
     * Updates an entity's attack, checking for hits during the ACTIVE phase.
     * Should be called every frame while the entity is attacking.
     *
     * @param attacker The attacking entity
     * @param attackComponent The attacker's attack component
     * @param potentialTargets List of potential targets
     * @param damageCallback Optional callback for spawning damage numbers
     */
    public static void updateAttack(GameObject attacker,
                                   AttackComponent attackComponent,
                                   List<GameObject> potentialTargets,
                                   DamageCallback damageCallback) {
        // Only check for hits during ACTIVE phase
        if (!attackComponent.isInActivePhase()) {
            return;
        }

        WeaponStats weapon = attackComponent.getCurrentWeapon();
        if (weapon == null) {
            return;
        }


        // Get attack strategy for this weapon type
        AttackStrategy strategy = getStrategy(weapon.getType());

        // Execute attack and get hit entities
        List<GameObject> hitEntities = strategy.executeAttack(
            attacker,
            attackComponent,
            weapon,
            potentialTargets
        );

        // Apply damage and knockback to hit entities
        for (GameObject target : hitEntities) {
            applyDamage(attacker, target, weapon, damageCallback);
            System.out.print("damage! : " + damageCallback.toString());
        }
    }

    /**
     * Applies damage and knockback to a target entity.
     *
     * @param attacker The attacking entity
     * @param target The target entity
     * @param weapon The weapon used
     * @param damageCallback Optional callback for spawning damage numbers
     */
    private static void applyDamage(GameObject attacker, GameObject target,
                                   WeaponStats weapon, DamageCallback damageCallback) {
        // Apply damage if target is an Entity
        if (target instanceof Entity) {
            Entity targetEntity = (Entity) target;
            targetEntity.damage(weapon.getDamage());

            // Spawn damage number
            if (damageCallback != null) {
                Transform targetTransform = target.getComponent(Transform.class);
                if (targetTransform != null) {
                    float x = targetTransform.getX() + 8f;
                    float y = targetTransform.getY() + 16f;
                    damageCallback.onDamage(x, y, weapon.getDamage());
                }
            }
        }

        // Apply knockback
        if (weapon.getKnockback() > 0) {
            CombatUtils.applyKnockback(attacker, target, weapon.getKnockback());
        }
    }

    /**
     * Callback interface for damage number spawning.
     */
    public interface DamageCallback {
        void onDamage(float x, float y, int damage);
    }
}
