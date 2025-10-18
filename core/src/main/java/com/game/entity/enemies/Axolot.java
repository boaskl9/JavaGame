package com.game.entity.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.game.entity.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;

/**
 * Frog enemy - a weak, fast-moving enemy that jumps around.
 * Good for early game encounters.
 */
public class Axolot extends EnemyEntity {
    private static final int FROG_MAX_HEALTH = 20;
    private static final float FROG_SPEED = 50f;
    private static final float FROG_DETECTION_RANGE = 80f;
    private static final float FROG_ATTACK_RANGE = 16f;

    public Axolot(WorldManager world, float x, float y) {
        super(world, FROG_MAX_HEALTH, x, y);

        // Configure AI
        ai.setMoveSpeed(FROG_SPEED);
        ai.setDetectionRange(FROG_DETECTION_RANGE);
        ai.setAttackRange(FROG_ATTACK_RANGE);
        ai.setWanderInterval(1.5f);  // Frogs hop around more frequently
        ai.setIdleTime(0.8f);          // Short idle time

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
        // Frog's simple attack: lunge forward after a brief delay
        if (ai.getStateTimer() > 0.5f) {
            // TODO: Implement actual damage dealing when weapon/combat system is ready
            ai.setState(com.game.components.AIComponent.AIState.CHASE);
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // TODO: Drop items (frog legs, coins, etc.) when item drop system is ready
    }
}
