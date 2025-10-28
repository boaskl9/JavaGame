package com.game.systems.combat;

import com.badlogic.gdx.math.Rectangle;
import com.game.components.AttackComponent;
import com.game.systems.entity.GameObject;

import java.util.List;

/**
 * Strategy interface for different weapon attack behaviors.
 * Implementations define how different weapon types execute attacks and how weapons move/animate.
 *
 * Examples:
 * - ArcSwingStrategy: Sword swings in an arc from one side to the other
 * - SpearThrustStrategy: Straight thrust forward and retract
 * - HammerSlamStrategy: Overhead lift then slam down
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
     * Gets the polygon vertices for accurate debug visualization of rotating hitboxes.
     * Returns an array of [x1, y1, x2, y2, x3, y3, x4, y4] representing the 4 corners.
     *
     * @param attacker The entity performing the attack
     * @param attackComponent The attacker's attack component
     * @param weapon The weapon being used
     * @return float array of 8 values representing 4 corner points, or null if not applicable
     */
    float[] getHitboxPolygon(GameObject attacker,
                            AttackComponent attackComponent,
                            WeaponStats weapon);

    /**
     * Gets the weapon angle offset for the current attack phase.
     * This is added to the base attack direction to get the final weapon angle.
     *
     * For example:
     * - Arc swing: Returns angle that sweeps from -60° to +60° during ACTIVE phase
     * - Thrust: Returns 0° during windup, small offset during active, 0° during recovery
     * - Overhead slam: Returns 90° (up) during windup, -90° (down) during active
     *
     * @param attackComponent The attack component (contains phase and timing info)
     * @param weapon The weapon being used
     * @return Angle offset in degrees to add to base attack direction
     */
    float getWeaponAngleOffset(AttackComponent attackComponent, WeaponStats weapon);

    /**
     * Gets the name of this attack strategy (for debugging).
     */
    String getName();
}
