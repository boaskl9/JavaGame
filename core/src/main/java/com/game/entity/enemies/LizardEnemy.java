package com.game.entity.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.game.components.AIComponent;
import com.game.entity.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.loot.LootTableComponent;

/**
 * Slime enemy - a very basic enemy type.
 * Low health, slow movement, but can split into smaller slimes on death (future feature).
 * Uses the Frog sprite as a placeholder.
 */
public class LizardEnemy extends EnemyEntity {
    private static final int SLIME_MAX_HEALTH = 15;
    private static final float SLIME_SPEED = 30f;
    private static final float SLIME_DETECTION_RANGE = 60f;
    private static final float SLIME_ATTACK_RANGE = 16f;

    private boolean canSplit;

    public LizardEnemy(WorldManager world, float x, float y) {
        this(world, x, y, true);
    }

    public LizardEnemy(WorldManager world, float x, float y, boolean canSplit) {
        super(world, SLIME_MAX_HEALTH, x, y);
        this.canSplit = canSplit;

        // Configure AI - slimes are slow and dumb
        ai.setMoveSpeed(SLIME_SPEED);
        ai.setDetectionRange(SLIME_DETECTION_RANGE);
        ai.setAttackRange(SLIME_ATTACK_RANGE);
        ai.setWanderInterval(2.5f);
        ai.setIdleTime(1.5f);

        // Slimes start in wander state
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

        // Load animations (using Frog as placeholder)
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
    protected void performAttack(float delta) {
        // Slime's simple attack - just bumps into the player
        if (ai.getStateTimer() > 0.8f) {
            // TODO: Deal damage when combat system is ready
            System.out.println("Slime bumps the player!");
            ai.setState(AIComponent.AIState.CHASE);
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();

        // Future feature: split into smaller slimes
        if (canSplit && MathUtils.randomBoolean(0.5f)) {
            System.out.println("Slime would split into smaller slimes here!");
            // TODO: Spawn 2-3 smaller slimes when spawning system is ready
        }

        // TODO: Drop slime gel item
    }
}
