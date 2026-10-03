package com.game.systems.entity.entities.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.game.components.AIComponent;
import com.game.components.SeparationComponent;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ProjectileEntity;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.loot.LootTableComponent;

/**
 * Axolotl enemy - a weak, fast ranged enemy. It keeps its distance and spits water at the player
 * when it has a clear shot, backing off if the player gets too close.
 */
public class Axolot extends EnemyEntity {
    private static final int AXOLOTL_MAX_HEALTH = 20;
    private static final float AXOLOTL_SPEED = 50f;
    private static final float AXOLOTL_DETECTION_RANGE = 120f;
    private static final float AXOLOTL_ATTACK_RANGE = 90f;   // Starts shooting within this distance
    private static final float RETREAT_DISTANCE = 36f;       // Backs off when the player is closer than this

    private static final float SHOT_SPEED = 110f;
    private static final float SHOT_WINDUP = 0.35f;          // Pause before each shot, so it can be dodged
    private static final float SHOT_INTERVAL = 1.4f;
    private static final float SHOT_SPREAD_DEGREES = 4f;

    private float shotCooldown = 0.5f;
    private float windupTimer = 0f;

    public Axolot(WorldManager world, float x, float y) {
        super(world, AXOLOTL_MAX_HEALTH, x, y);

        // Configure AI
        ai.setMoveSpeed(AXOLOTL_SPEED);
        ai.setDetectionRange(AXOLOTL_DETECTION_RANGE);
        ai.setAttackRange(AXOLOTL_ATTACK_RANGE);
        ai.setWanderInterval(1.5f);
        ai.setIdleTime(0.8f);          // Short idle time

        // Configure separation behavior
        separation = new SeparationComponent(36f, 50f);
        addComponent(separation);

        // Add loot table
        LootTableComponent lootTable = new LootTableComponent()
            .addDrop("ruby_ring", 0.8f, 1, 3)
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
    protected void updateAI(float delta) {
        shotCooldown = Math.max(0f, shotCooldown - delta);
        super.updateAI(delta);
    }

    @Override
    protected boolean canStartAttack(float distanceToTarget) {
        return distanceToTarget <= ai.getAttackRange() && hasClearShot();
    }

    @Override
    protected void handleAttackState(float delta, float distanceToTarget) {
        // Out of range or behind a wall: go find a better spot
        if (distanceToTarget > ai.getAttackRange() * 1.2f || !hasClearShot()) {
            windupTimer = 0f;
            ai.setState(AIComponent.AIState.CHASE);
            return;
        }

        Vector2 from = getShotOrigin();
        Vector2 to = getTargetCenter();

        // Keep some distance; otherwise stand still and face the target
        if (distanceToTarget < RETREAT_DISTANCE) {
            Vector2 away = new Vector2(from).sub(to).nor().scl(ai.getMoveSpeed());
            away.add(calculateSeparationForce().scl(0.6f));
            velocity.setVelocity(away.x, away.y);
        } else {
            velocity.setVelocity(0, 0);
            lastDirectionAngle = getDirectionAngle(new Vector2(to).sub(from));
        }

        performAttack(delta);
    }

    @Override
    protected void performAttack(float delta) {
        if (shotCooldown > 0f || target == null) return;

        windupTimer += delta;
        if (windupTimer < SHOT_WINDUP) return;

        windupTimer = 0f;
        shotCooldown = SHOT_INTERVAL;
        shoot();
    }

    private void shoot() {
        Vector2 from = getShotOrigin();
        Vector2 to = getTargetCenter();
        Vector2 shot = ProjectileEntity.spread(
            ProjectileEntity.aim(from.x, from.y, to.x, to.y, SHOT_SPEED), SHOT_SPREAD_DEGREES);

        world.addGameObject(new ProjectileEntity(
            world, ProjectileEntity.Kind.WATER_SPIT, this, from.x, from.y, shot.x, shot.y));
    }

    private boolean hasClearShot() {
        return target != null && hasClearLine(getShotOrigin(), getTargetCenter());
    }

    private Vector2 getShotOrigin() {
        return new Vector2(transform.getX() + SIZE / 2f, transform.getY() + SIZE / 2f);
    }

    private Vector2 getTargetCenter() {
        return new Vector2(target.getTransform().getX() + SIZE / 2f, target.getTransform().getY() + SIZE / 2f);
    }
}
