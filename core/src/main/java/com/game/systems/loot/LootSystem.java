package com.game.systems.loot;

import com.game.entity.EnemyEntity;
import com.game.entity.PlayerEntity;
import com.game.integration.WorldItemManager;
import com.game.systems.entity.Transform;
import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager system that orchestrates loot drop generation.
 * Collects modifiers from player, rolls drops, and spawns items in the world.
 *
 * Usage:
 * 1. Initialize once: LootSystem.initialize(worldItemManager)
 * 2. Use anywhere: LootSystem.getInstance().generateAndSpawnLoot(...)
 */
public class LootSystem {

    private static LootSystem instance;
    private final WorldItemManager worldItemManager;

    // Drop scatter configuration
    private static final float DROP_SCATTER_RADIUS = 12f;  // Pixels to scatter drops
    private static final float DROP_GRACE_PERIOD = 0.5f;   // Seconds before items can be picked up

    /**
     * Private constructor for singleton pattern.
     */
    private LootSystem(WorldItemManager worldItemManager) {
        if (worldItemManager == null) {
            throw new IllegalArgumentException("WorldItemManager cannot be null");
        }
        this.worldItemManager = worldItemManager;
    }

    /**
     * Initializes the singleton instance.
     * Should be called once during game startup.
     *
     * @param worldItemManager The world item manager for spawning drops
     */
    public static void initialize(WorldItemManager worldItemManager) {
        if (instance != null) {
            System.err.println("LootSystem already initialized! Replacing existing instance.");
        }
        instance = new LootSystem(worldItemManager);
    }

    /**
     * Gets the singleton instance.
     * Throws exception if not initialized.
     *
     * @return The LootSystem instance
     */
    public static LootSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LootSystem not initialized! Call LootSystem.initialize() first.");
        }
        return instance;
    }

    /**
     * Checks if the LootSystem has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Generates and spawns loot for an enemy death.
     *
     * @param enemy The enemy that died
     * @param player The player that killed the enemy (for modifiers)
     * @return List of item stacks that were dropped
     */
    public List<ItemStack> generateAndSpawnLoot(EnemyEntity enemy, PlayerEntity player) {
        // Get enemy's loot table
        LootTableComponent lootTable = enemy.getComponent(LootTableComponent.class);
        if (lootTable == null) {
            return new ArrayList<>(); // Enemy has no loot table
        }

        // Collect active modifiers from player
        List<LootModifier> modifiers = collectPlayerModifiers(player);

        // Create context
        LootContext context = new LootContext(enemy, player, modifiers);

        // Roll drops
        List<ItemStack> drops = lootTable.rollDrops(context);

        // Spawn items in world
        if (!drops.isEmpty()) {
            Transform enemyTransform = enemy.getTransform();
            if (enemyTransform != null) {
                float centerX = enemyTransform.getX() + 8f;  // Center of 16x16 enemy
                float centerY = enemyTransform.getY() + 8f;
                spawnLoot(drops, centerX, centerY);
            }
        }

        return drops;
    }

    /**
     * Collects all active loot modifiers from the player's equipment and effects.
     * TODO: Implement once equipment/skill system is in place.
     *
     * @param player The player to collect modifiers from
     * @return List of active modifiers
     */
    private List<LootModifier> collectPlayerModifiers(PlayerEntity player) {
        List<LootModifier> modifiers = new ArrayList<>();

        if (player == null) {
            return modifiers;
        }

        // TODO: Implement modifier collection from:
        // - Equipped weapons (e.g., hide harvester)
        // - Equipped accessories/trinkets (e.g., coffee ring)
        // - Active skills/buffs
        // - Player stats (e.g., luck modifier)

        // For now, return empty list
        return modifiers;
    }

    /**
     * Spawns loot items in the world with scatter effect.
     *
     * @param drops List of item stacks to spawn
     * @param centerX Center X position
     * @param centerY Center Y position
     */
    private void spawnLoot(List<ItemStack> drops, float centerX, float centerY) {
        for (ItemStack drop : drops) {
            // Calculate random scatter offset
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.random() * DROP_SCATTER_RADIUS;
            float offsetX = (float) (Math.cos(angle) * distance);
            float offsetY = (float) (Math.sin(angle) * distance);

            // Spawn item
            worldItemManager.spawnItem(
                drop,
                centerX + offsetX,
                centerY + offsetY,
                DROP_GRACE_PERIOD
            );
        }
    }

    /**
     * Sets the drop scatter radius (for testing/configuration).
     */
    public static float getDropScatterRadius() {
        return DROP_SCATTER_RADIUS;
    }
}
