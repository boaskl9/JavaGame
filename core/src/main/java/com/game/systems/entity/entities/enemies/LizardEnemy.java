package com.game.systems.entity.entities.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.game.components.AIComponent;
import com.game.components.WeaponRenderComponent;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.combat.WeaponStats;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.game.systems.loot.LootTableComponent;

/**
 * Lizard enemy - a very basic enemy type.
 * Low health, slow movement, but can split into smaller slimes on death (future feature).
 * Uses the Frog sprite as a placeholder.
 */
public class LizardEnemy extends EnemyEntity {
    private static final int LIZARD_MAX_HEALTH = 15;
    private static final float LIZARD_SPEED = 30f;
    private static final float LIZARD_DETECTION_RANGE = 60f;
    private static final float LIZARD_ATTACK_RANGE = 16f;

    public LizardEnemy(WorldManager world, float x, float y) {
        super(world, LIZARD_MAX_HEALTH, x, y);

        // Configure AI - slimes are slow and dumb
        ai.setMoveSpeed(LIZARD_SPEED);
        ai.setDetectionRange(LIZARD_DETECTION_RANGE);
        ai.setAttackRange(LIZARD_ATTACK_RANGE);
        ai.setWanderInterval(2.5f);
        ai.setIdleTime(1.5f);

        // Lizards start in wander state
        ai.setState(AIComponent.AIState.WANDER);
        pickRandomWanderDirection();

        // Add loot table
        LootTableComponent lootTable = new LootTableComponent()
            .addDrop("stone", 0.8f, 1, 3)         // 80% chance for 1-3 stones
            .addDrop("wood", 0.5f, 1, 2)          // 50% chance for 1-2 wood
            .addDrop("health_potion", 0.15f, 1);  // 15% chance for 1 health potion
        addComponent(lootTable);

        // Add tags for potential context-aware drops
        addTag("monster:lizard");
        addTag("reptile");

        // Setup claw weapon rendering with 4-frame animation
        WeaponRenderComponent weaponRender = new WeaponRenderComponent();
        try {
            Texture clawSheet = new Texture(Gdx.files.internal("assets/FX/SlashFx/Claw/SpriteSheet.png"));
            weaponRender.setWeaponAnimationFrames(clawSheet, 4);  // 4 frames
            weaponRender.setAttachmentPoint(8f, 8f);  // Centered on lizard
            weaponRender.setPivotPoint(0f, 0f);       // Pivot at base of claw
            addComponent(weaponRender);
        } catch (Exception e) {
            System.err.println("Failed to load claw animation for LizardEnemy: " + e.getMessage());
        }

        // Load animations (using Lizard sprite)
        loadAnimations();
    }

    private void loadAnimations() {
        String spritePath = "assets/Actor/Monsters/Lizard/Lizard.png";
        Texture spriteSheet = new Texture(Gdx.files.internal(spritePath));

        // Load walk animation (4 frames per direction from a 4x4 sheet)
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "walk", spriteSheet, 4, 0.2f);

        // Load idle animation (single static frame from top-left corner of 4x4 sheet)
        AnimationBuilder.loadStatic(animation.getAnimator(), "idle", spriteSheet, 4, 4);
    }

    @Override
    protected WeaponStats getWeaponStats() {
        // Use claw weapon stats from the item registry
        ItemDefinition clawDef = ItemRegistry.get("claw_attack");
        if (clawDef != null && clawDef.getWeaponStats() != null) {
            return clawDef.getWeaponStats();
        }

        // Fallback to parent implementation if claw not found
        return super.getWeaponStats();
    }
}
