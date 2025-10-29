package com.game.systems.loot;

import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Context information passed during loot drop generation.
 * Contains all data needed for intelligent, context-aware drop decisions.
 */
public class LootContext {
    private final EnemyEntity enemy;
    private final PlayerEntity player;
    private final List<LootModifier> activeModifiers;

    public LootContext(EnemyEntity enemy, PlayerEntity player) {
        this.enemy = enemy;
        this.player = player;
        this.activeModifiers = new ArrayList<>();
    }

    public LootContext(EnemyEntity enemy, PlayerEntity player, List<LootModifier> modifiers) {
        this.enemy = enemy;
        this.player = player;
        this.activeModifiers = new ArrayList<>(modifiers);
    }

    /**
     * Adds a modifier to this context.
     */
    public void addModifier(LootModifier modifier) {
        activeModifiers.add(modifier);
    }

    /**
     * Adds multiple modifiers to this context.
     */
    public void addModifiers(List<LootModifier> modifiers) {
        activeModifiers.addAll(modifiers);
    }

    public EnemyEntity getEnemy() {
        return enemy;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public List<LootModifier> getActiveModifiers() {
        return activeModifiers;
    }

    /**
     * Checks if the enemy has a specific tag.
     * Useful for context-aware modifiers.
     */
    public boolean enemyHasTag(String tag) {
        return enemy != null && enemy.hasTag(tag);
    }

    /**
     * Gets the enemy's class simple name for identification.
     */
    public String getEnemyType() {
        return enemy != null ? enemy.getClass().getSimpleName() : "Unknown";
    }
}
