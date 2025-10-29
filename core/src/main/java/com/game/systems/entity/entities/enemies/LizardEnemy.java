package com.game.systems.entity.entities.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.game.components.AIComponent;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
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
        // Lizard's simple attack - just bumps into the player
        if (ai.getStateTimer() > 0.8f) {
            // TODO: Deal damage when combat system is ready
            System.out.println("Lizard bumps the player!");
            ai.setState(AIComponent.AIState.CHASE);
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();
    }
}
