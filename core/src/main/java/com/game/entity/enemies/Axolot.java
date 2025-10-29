package com.game.entity.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.game.entity.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.loot.LootTableComponent;

/**
 * axolotl enemy - a weak, fast-moving enemy that jumps around.
 * Good for early game encounters.
 */
public class Axolot extends EnemyEntity {
    private static final int AXOLOTL_MAX_HEALTH = 20;
    private static final float AXOLOTL_SPEED = 50f;
    private static final float AXOLOTL_DETECTION_RANGE = 80f;
    private static final float AXOLOTL_ATTACK_RANGE = 16f;

    public Axolot(WorldManager world, float x, float y) {
        super(world, AXOLOTL_MAX_HEALTH, x, y);

        // Configure AI
        ai.setMoveSpeed(AXOLOTL_SPEED);
        ai.setDetectionRange(AXOLOTL_DETECTION_RANGE);
        ai.setAttackRange(AXOLOTL_ATTACK_RANGE);
        ai.setWanderInterval(1.5f);
        ai.setIdleTime(0.8f);          // Short idle time

        // Add loot table
        LootTableComponent lootTable = new LootTableComponent()
            .addDrop("ruby_ring", 0.8f, 1, 3)         // 80% chance for 1-3 stones// 50% chance for 1-2 wood
            .addDrop("health_potion", 0.15f, 1);  // 15% chance for 1 health potion
        addComponent(lootTable);

        // Load animations
        loadAnimations();
    }

    private void loadAnimations() {
        String spritePath = "assets/Actor/Monsters/Axolot/SpriteSheet.png";
        Texture spriteSheet = new Texture(Gdx.files.internal(spritePath));

        // Load walk animation (4 frames per direction from a 4x4 sheet)
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "walk", spriteSheet, 4, 0.15f);

        // Load idle animation (single static frame from top-left corner of 4x4 sheet)
        AnimationBuilder.loadStatic(animation.getAnimator(), "idle", spriteSheet, 4, 4);
    }

    @Override
    protected void performAttack(float delta) {
        // axolotl's simple attack: lunge forward after a brief delay
        if (ai.getStateTimer() > 0.5f) {
            // TODO: Implement actual damage dealing when weapon/combat system is ready
            ai.setState(com.game.components.AIComponent.AIState.CHASE);
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // TODO: Drop items (axolotl legs, coins, etc.) when item drop system is ready
    }
}
