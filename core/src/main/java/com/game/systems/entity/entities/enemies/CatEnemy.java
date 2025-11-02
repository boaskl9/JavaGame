package com.game.systems.entity.entities.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.game.components.SeparationComponent;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;

/**
 * Cat enemy - a medium-difficulty enemy with moderate speed and health.
 * More aggressive than frogs.
 */
public class CatEnemy extends EnemyEntity {
    private static final int CAT_MAX_HEALTH = 40;
    private static final float CAT_SPEED = 60f;
    private static final float CAT_DETECTION_RANGE = 1200f;
    private static final float CAT_ATTACK_RANGE = 60f;

    private float attackCooldown = 0f;
    private static final float ATTACK_COOLDOWN_TIME = 1.5f;

    public CatEnemy(WorldManager world, float x, float y) {
        super(world, CAT_MAX_HEALTH, x, y);

        // Configure AI
        ai.setMoveSpeed(CAT_SPEED);
        ai.setDetectionRange(CAT_DETECTION_RANGE);
        ai.setAttackRange(CAT_ATTACK_RANGE);
        ai.setWanderInterval(3f);
        ai.setIdleTime(2f);

        // Configure separation behavior (larger radius for faster enemy)
        separation = new SeparationComponent(40f, 10f);
        addComponent(separation);

        // Load animations
        loadAnimations();
    }

    private void loadAnimations() {
        String spritePath = "assets/Actor/Animals/Cat/SpriteSheet.png";
        Texture spriteSheet = new Texture(Gdx.files.internal(spritePath));

        // Load walk animation (4 frames per direction from a 4x4 sheet)
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "walk", spriteSheet, 4, 0.18f);

        // Load idle animation (single static frame from top-left corner of 4x4 sheet)
        AnimationBuilder.loadStatic(animation.getAnimator(), "idle", spriteSheet, 4, 4);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }
    }

    @Override
    protected void performAttack(float delta) {
        // Cat's attack with cooldown
        if (attackCooldown <= 0 && ai.getStateTimer() > 0.3f) {
            // TODO: Deal damage to player when combat system is ready
            System.out.println("Cat scratches!");
            attackCooldown = ATTACK_COOLDOWN_TIME;
            ai.setState(com.game.components.AIComponent.AIState.CHASE);
        } else if (ai.getStateTimer() > 2.0f) {
            // Give up and chase again if attack window is missed
            ai.setState(com.game.components.AIComponent.AIState.CHASE);
        }
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // TODO: Drop items when item drop system is ready
    }
}
