package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.systems.entity.GameObject;

import java.util.List;

/**
 * Strategy interface for different weapon attack behaviors.
 * Implementations define how different weapon types execute attacks.
 *
 * Examples:
 * - ArcSwingStrategy: Sword swings in an arc
 * - SpearThrustStrategy: Straight-line thrust attack
 * - HammerSlamStrategy: Circular area-of-effect slam
 */
public interface AttackStrategy {

    /**
     * Executes the attack, checking for hits and dealing damage.
     * Called during the ACTIVE phase of the attack.
     *
     * @param attacker The entity performing the attack
     * @param attackComponent The attacker's attack component
     * @param weapon The weapon being used
     * @param potentialTargets List of potential target entities to check
     * @return List of entities that were hit by this attack
     */
    List<GameObject> executeAttack(GameObject attacker,
                                   AttackComponent attackComponent,
                                   WeaponStats weapon,
                                   List<GameObject> potentialTargets);

    /**
     * Gets the current hitbox for this attack.
     * Used for debug visualization.
     *
     * @param attacker The entity performing the attack
     * @param attackComponent The attacker's attack component
     * @param weapon The weapon being used
     * @return Rectangle representing the current attack hitbox (may be approximate for non-rectangular attacks)
     */
    Rectangle getHitbox(GameObject attacker,
                       AttackComponent attackComponent,
                       WeaponStats weapon);

    /**
     * Gets the name of this attack strategy (for debugging).
     */
    String getName();
}
