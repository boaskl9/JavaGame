package com.game.systems.loot;

import java.util.List;

/**
 * Interface for modifiers that can alter loot drops dynamically.
 * Applied by player equipment, skills, buffs, etc.
 *
 * Modifiers are applied in priority order (higher priority first).
 */
public interface LootModifier {

    /**
     * Modifies the list of loot drops based on context.
     * Can add new drops, modify existing ones, or remove drops.
     *
     * @param drops The current list of drops (mutable)
     * @param context Context information (enemy, player, etc.)
     */
    void modify(List<LootDrop> drops, LootContext context);

    /**
     * Gets the priority of this modifier for ordering.
     * Higher priority modifiers are applied first.
     * Default priority is 0.
     *
     * Suggested values:
     * - 100: Add new drops (so they can be modified by other modifiers)
     * - 50: Modify chances
     * - 0: Modify quantities
     * - -50: Remove drops
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Gets a description of this modifier (for debugging/UI).
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
}
