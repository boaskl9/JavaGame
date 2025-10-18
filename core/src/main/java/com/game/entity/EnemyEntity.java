package com.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.game.components.AIComponent;
import com.game.components.AnimationComponent;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.components.VelocityComponent;
import com.game.integration.WorldManager;
import com.game.systems.entity.Entity;
import com.game.systems.entity.Transform;

/**
 * Base class for all enemy entities.
 * Provides common enemy functionality like AI, movement, and combat.
 */
public abstract class EnemyEntity extends Entity {
    protected static final int SIZE = 16;

    protected WorldManager world;
    protected PlayerEntity target; // The player (or other target)

    // Component references
    protected Transform transform;
    protected VelocityComponent velocity;
    protected AnimationComponent animation;
    protected ColliderComponent environmentCollider;  // For walls, obstacles (feet only)
    protected ColliderComponent combatCollider;       // For player attacks, projectiles (full body)
    protected AIComponent ai;

    protected int lastDirectionAngle = 180; // Down

    public EnemyEntity(WorldManager world, int maxHealth, float x, float y) {
        super(maxHealth);
        this.world = world;

        // Add core components
        transform = new Transform(x, y);
        addComponent(transform);

        velocity = new VelocityComponent();
        addComponent(velocity);

        animation = new AnimationComponent();
        addComponent(animation);

        // Environment collider - feet only, smaller for better navigation
        // 8x4 pixels at bottom center (50% width, 25% height)
        environmentCollider = new ColliderComponent(SIZE * 0.5f, SIZE * 0.25f, SIZE * 0.25f, 0);
        addComponent(environmentCollider);

        // Combat collider - full body, slightly smaller than sprite
        // 12x12 pixels centered (75% of sprite size)
        combatCollider = new ColliderComponent(SIZE * 0.75f, SIZE * 0.75f, SIZE * 0.125f, SIZE * 0.125f);
        // Note: Combat collider not added as component yet (will be used for combat system later)

        RenderComponent render = new RenderComponent(SIZE, SIZE);
        addComponent(render);

        // AI component
        ai = new AIComponent();
        addComponent(ai);
    }

    @Override
    public void update(float delta) {
        if (!isAlive()) {
            return;
        }

        // Update AI behavior
        updateAI(delta);

        // Update animation based on movement
        updateAnimation();

        // Apply movement with collision
        applyMovement(delta);

        // Update all components
        super.update(delta);
    }

    /**
     * Updates AI behavior. Override for custom AI logic.
     */
    protected void updateAI(float delta) {
        // Find player target if not set
        if (target == null) {
            target = findPlayer();
        }

        if (target == null || !target.isAlive()) {
            // No target - idle or wander
            handleNoTarget(delta);
            return;
        }

        float distanceToTarget = transform.getPosition().dst(target.getTransform().getPosition());

        // State machine
        switch (ai.getState()) {
            case IDLE:
                handleIdleState(delta, distanceToTarget);
                break;

            case WANDER:
                handleWanderState(delta, distanceToTarget);
                break;

            case CHASE:
                handleChaseState(delta, distanceToTarget);
                break;

            case ATTACK:
                handleAttackState(delta, distanceToTarget);
                break;

            case FLEE:
                handleFleeState(delta, distanceToTarget);
                break;
        }
    }

    /**
     * Handle behavior when there's no target.
     */
    protected void handleNoTarget(float delta) {
        if (ai.getState() == AIComponent.AIState.IDLE) {
            if (ai.getIdleTimer() >= ai.getIdleTime()) {
                ai.setState(AIComponent.AIState.WANDER);
                pickRandomWanderDirection();
            } else {
                velocity.setVelocity(0, 0);
            }
        } else if (ai.getState() == AIComponent.AIState.WANDER) {
            if (ai.getWanderTimer() >= ai.getWanderInterval()) {
                ai.setState(AIComponent.AIState.IDLE);
                velocity.setVelocity(0, 0);
            } else {
                // Move in wander direction
                Vector2 dir = ai.getWanderDirection();
                velocity.setVelocity(dir.x * ai.getMoveSpeed(), dir.y * ai.getMoveSpeed());
            }
        } else {
            // Return to idle
            ai.setState(AIComponent.AIState.IDLE);
            velocity.setVelocity(0, 0);
        }
    }

    /**
     * Handle IDLE state.
     */
    protected void handleIdleState(float delta, float distanceToTarget) {
        velocity.setVelocity(0, 0);

        // Check if player is in detection range
        if (distanceToTarget <= ai.getDetectionRange()) {
            ai.setState(AIComponent.AIState.CHASE);
        } else if (ai.getIdleTimer() >= ai.getIdleTime()) {
            // Randomly switch to wander
            if (MathUtils.random() < 0.7f) {
                ai.setState(AIComponent.AIState.WANDER);
                pickRandomWanderDirection();
            } else {
                ai.resetIdleTimer();
            }
        }
    }

    /**
     * Handle WANDER state.
     */
    protected void handleWanderState(float delta, float distanceToTarget) {
        // Check if player is in detection range
        if (distanceToTarget <= ai.getDetectionRange()) {
            ai.setState(AIComponent.AIState.CHASE);
            velocity.setVelocity(0, 0);
            return;
        }

        // Continue wandering
        if (ai.getWanderTimer() >= ai.getWanderInterval()) {
            ai.setState(AIComponent.AIState.IDLE);
            velocity.setVelocity(0, 0);
        } else {
            Vector2 dir = ai.getWanderDirection();
            velocity.setVelocity(dir.x * ai.getMoveSpeed(), dir.y * ai.getMoveSpeed());
        }
    }

    /**
     * Handle CHASE state - move directly towards target.
     */
    protected void handleChaseState(float delta, float distanceToTarget) {
        // Lost sight of player
        if (distanceToTarget > ai.getDetectionRange() * 1.5f) {
            ai.setState(AIComponent.AIState.IDLE);
            velocity.setVelocity(0, 0);
            return;
        }

        // Close enough to attack
        if (distanceToTarget <= ai.getAttackRange()) {
            ai.setState(AIComponent.AIState.ATTACK);
            velocity.setVelocity(0, 0);
            return;
        }

        // Move directly towards target
        Vector2 direction = new Vector2(
            target.getTransform().getX() - transform.getX(),
            target.getTransform().getY() - transform.getY()
        ).nor();

        velocity.setVelocity(direction.x * ai.getMoveSpeed(), direction.y * ai.getMoveSpeed());
    }

    /**
     * Handle ATTACK state.
     */
    protected void handleAttackState(float delta, float distanceToTarget) {
        velocity.setVelocity(0, 0);

        // Too far to attack - chase again
        if (distanceToTarget > ai.getAttackRange() * 1.2f) {
            ai.setState(AIComponent.AIState.CHASE);
            return;
        }

        // Perform attack (override in subclasses)
        performAttack(delta);
    }

    /**
     * Handle FLEE state.
     */
    protected void handleFleeState(float delta, float distanceToTarget) {
        // Run away from player
        Vector2 direction = new Vector2(
            transform.getX() - target.getTransform().getX(),
            transform.getY() - target.getTransform().getY()
        ).nor();

        velocity.setVelocity(direction.x * ai.getMoveSpeed() * 1.5f, direction.y * ai.getMoveSpeed() * 1.5f);

        // If far enough, go back to idle
        if (distanceToTarget > ai.getDetectionRange() * 2f) {
            ai.setState(AIComponent.AIState.IDLE);
        }
    }

    /**
     * Performs an attack. Override in subclasses for custom attack behavior.
     */
    protected void performAttack(float delta) {
        // Override in subclasses
        // For now, just wait a bit before going back to chase
        if (ai.getStateTimer() > 1.0f) {
            ai.setState(AIComponent.AIState.CHASE);
        }
    }

    /**
     * Picks a random direction for wandering.
     */
    protected void pickRandomWanderDirection() {
        float angle = MathUtils.random(0f, 360f);
        float x = MathUtils.cosDeg(angle);
        float y = MathUtils.sinDeg(angle);
        ai.setWanderDirection(x, y);
        ai.resetWanderTimer();
    }

    /**
     * Updates animation based on movement direction.
     * Uses the same animation system as PlayerEntity.
     */
    protected void updateAnimation() {
        Vector2 vel = velocity.getVelocity();
        String state;

        if (vel.len() > 0.1f) {
            state = "walk";
            lastDirectionAngle = getDirectionAngle(vel);
        } else {
            state = "idle";
        }

        // Determine if we should flip horizontally (for right-facing)
        // Angle 270 = right in the coordinate system
        boolean flip = lastDirectionAngle == 270;

        animation.setState(state, lastDirectionAngle, flip);
    }

    /**
     * Converts a velocity vector to a direction angle.
     * Returns: 0 (up), 90 (left), 180 (down), 270 (right)
     */
    protected int getDirectionAngle(Vector2 direction) {
        float angle = direction.angleDeg();
        if (angle < 0) angle += 360;

        if (angle >= 315 || angle < 45) return 270;      // Right
        else if (angle >= 45 && angle < 135) return 0;   // Up
        else if (angle >= 135 && angle < 225) return 90; // Left
        else return 180; // Down
    }

    /**
     * Applies movement with collision detection.
     */
    protected void applyMovement(float delta) {
        Vector2 vel = velocity.getVelocity();
        if (vel.len() == 0) return;

        Vector2 newPosition = new Vector2(transform.getPosition());
        newPosition.add(vel.x * delta, vel.y * delta);

        // Check collision on X axis
        if (vel.x != 0) {
            if (isPositionWalkable(newPosition.x, transform.getY())) {
                transform.setPosition(newPosition.x, transform.getY());
            }
        }

        // Check collision on Y axis
        if (vel.y != 0) {
            if (isPositionWalkable(transform.getX(), newPosition.y)) {
                transform.setPosition(transform.getX(), newPosition.y);
            }
        }
    }

    /**
     * Checks if a position is walkable for this enemy.
     */
    protected boolean isPositionWalkable(float x, float y) {
        // Use the environment collider (feet) for collision detection
        float hitboxWidth = environmentCollider.getWidth();
        float hitboxHeight = environmentCollider.getHeight();
        float offsetX = environmentCollider.getOffsetX();
        float offsetY = environmentCollider.getOffsetY();

        return world.isPositionWalkable(x + offsetX, y + offsetY, hitboxWidth, hitboxHeight);
    }

    /**
     * Finds the player in the world.
     */
    protected PlayerEntity findPlayer() {
        // Search for player in world objects
        for (var obj : world.getGameObjects()) {
            if (obj instanceof PlayerEntity) {
                return (PlayerEntity) obj;
            }
        }
        return null;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        System.out.println(getClass().getSimpleName() + " died at " + transform.getPosition());
        // Override in subclasses for death effects, drops, etc.
    }

    // Getters
    public Transform getTransform() {
        return transform;
    }

    public AIComponent getAI() {
        return ai;
    }

    public ColliderComponent getEnvironmentCollider() {
        return environmentCollider;
    }

    public ColliderComponent getCombatCollider() {
        return combatCollider;
    }
}
