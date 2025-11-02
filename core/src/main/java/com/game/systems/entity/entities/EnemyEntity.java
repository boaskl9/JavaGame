package com.game.systems.entity.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.game.components.AIComponent;
import com.game.components.AnimationComponent;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.components.SeparationComponent;
import com.game.components.VelocityComponent;
import com.game.integration.WorldManager;
import com.game.systems.combat.WeaponStats;
import com.game.systems.entity.Entity;
import com.game.systems.entity.Transform;

import java.util.HashSet;
import java.util.Set;

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
    protected AttackComponent attackComponent;
    protected SeparationComponent separation;         // For enemy-enemy spacing

    protected int lastDirectionAngle = 180; // Down

    // Tags for context-aware loot drops and other systems
    private final Set<String> tags;

    // Damage number callback
    private DamageNumberCallback damageNumberCallback;

    // Death callback
    private DeathCallback deathCallback;

    // Pathfinding
    protected Array<Vector2> currentPath;
    protected int currentWaypointIndex;
    protected float pathfindingUpdateTimer = 0f;
    protected static final float PATHFINDING_UPDATE_INTERVAL = 0.5f;
    protected static final float WAYPOINT_ARRIVAL_THRESHOLD = 8f;

    public EnemyEntity(WorldManager world, int maxHealth, float x, float y) {
        super(maxHealth);
        this.world = world;
        this.tags = new HashSet<>();

        // Add core components
        transform = new Transform(x, y);
        addComponent(transform);

        velocity = new VelocityComponent();
        addComponent(velocity);

        animation = new AnimationComponent();
        addComponent(animation);

        // Environment collider - very small "point" at feet for smooth corner navigation
        // NOT added to components - accessed via field reference by collision system
        environmentCollider = new ColliderComponent(4f, 4f, SIZE * 0.375f, SIZE * 0.0625f);

        // Combat collider - full body, slightly smaller than sprite (75% of sprite size)
        // This IS added to components so attack system can find it via getComponent()
        combatCollider = new ColliderComponent(SIZE * 0.75f, SIZE * 0.75f, SIZE * 0.125f, SIZE * 0.125f);
        addComponent(combatCollider);

        RenderComponent render = new RenderComponent(SIZE, SIZE);
        addComponent(render);

        // AI component
        ai = new AIComponent();
        addComponent(ai);

        // Attack component
        attackComponent = new AttackComponent();
        addComponent(attackComponent);
    }

    @Override
    public void update(float delta) {
        if (!isAlive()) {
            return;
        }

        // Update velocity component (handles knockback decay)
        velocity.update(delta);

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
     * Handle CHASE state - use NavMesh pathfinding.
     */
    protected void handleChaseState(float delta, float distanceToTarget) {
        // Lost sight of player
        if (distanceToTarget > ai.getDetectionRange() * 1.5f) {
            ai.setState(AIComponent.AIState.IDLE);
            velocity.setVelocity(0, 0);
            currentPath = null;
            return;
        }

        // Close enough to attack
        if (distanceToTarget <= ai.getAttackRange()) {
            ai.setState(AIComponent.AIState.ATTACK);
            velocity.setVelocity(0, 0);
            currentPath = null;
            return;
        }

        // Update pathfinding periodically
        if (world.getGridPathfinder() != null) {
            pathfindingUpdateTimer += delta;
            if (pathfindingUpdateTimer >= PATHFINDING_UPDATE_INTERVAL || currentPath == null) {
                pathfindingUpdateTimer = 0f;
                updatePath();
            }

            // Follow current path
            if (currentPath != null && currentWaypointIndex < currentPath.size) {
                Vector2 currentWaypoint = currentPath.get(currentWaypointIndex);
                Vector2 feetPos = getFeetPosition();

                // Check if we've reached the current waypoint (use feet position)
                float distToWaypoint = feetPos.dst(currentWaypoint);
                if (distToWaypoint < WAYPOINT_ARRIVAL_THRESHOLD) {
                    currentWaypointIndex++;

                    // If we've reached the last waypoint, stop
                    if (currentWaypointIndex >= currentPath.size) {
                        currentPath = null;
                        return;
                    }

                    currentWaypoint = currentPath.get(currentWaypointIndex);
                }

                // Move towards current waypoint (from feet position)
                Vector2 direction = new Vector2(
                    currentWaypoint.x - feetPos.x,
                    currentWaypoint.y - feetPos.y
                ).nor();

                // Apply separation force to avoid bunching
                Vector2 separationForce = calculateSeparationForce();

                // Blend pathfinding direction with separation (pathfinding dominates)
                Vector2 finalDirection = direction.scl(ai.getMoveSpeed());
                finalDirection.add(separationForce.scl(0.6f)); // Separation weight: 60%

                velocity.setVelocity(finalDirection.x, finalDirection.y);
            } else {
                // No path - stop
                velocity.setVelocity(0, 0);
            }
        } else {
            // No GridPathfinder - fall back to direct movement
            Vector2 direction = new Vector2(
                target.getTransform().getX() - transform.getX(),
                target.getTransform().getY() - transform.getY()
            ).nor();

            // Apply separation force to avoid bunching
            Vector2 separationForce = calculateSeparationForce();

            // Blend direct movement with separation
            Vector2 finalDirection = direction.scl(ai.getMoveSpeed());
            finalDirection.add(separationForce.scl(0.6f)); // Separation weight: 60%

            velocity.setVelocity(finalDirection.x, finalDirection.y);
        }
    }

    /**
     * Updates the path to the target using GridPathfinder.
     */
    protected void updatePath() {
        if (target == null || world.getGridPathfinder() == null) {
            currentPath = null;
            return;
        }

        // Use feet position (environment collider center) for pathfinding
        Vector2 startPos = getFeetPosition();
        Vector2 targetPos = getTargetFeetPosition();

        currentPath = world.getGridPathfinder().findPath(startPos, targetPos);

        currentWaypointIndex = 0;

        if (currentPath == null) {
            System.out.println(getClass().getSimpleName() + ": No path found to target");
        }
    }

    /**
     * Gets the position of the enemy's feet (center of environment collider).
     */
    protected Vector2 getFeetPosition() {
        float x = transform.getX() + environmentCollider.getOffsetX() + environmentCollider.getWidth() / 2f;
        float y = transform.getY() + environmentCollider.getOffsetY() + environmentCollider.getHeight() / 2f;
        return new Vector2(x, y);
    }

    /**
     * Gets the target's feet position.
     */
    protected Vector2 getTargetFeetPosition() {
        if (target == null) {
            return transform.getPosition();
        }

        // Player also has environment collider at feet
        ColliderComponent targetCollider = target.getEnvironmentCollider();
        if (targetCollider != null) {
            Transform targetTransform = target.getTransform();
            float x = targetTransform.getX() + targetCollider.getOffsetX() + targetCollider.getWidth() / 2f;
            float y = targetTransform.getY() + targetCollider.getOffsetY() + targetCollider.getHeight() / 2f;
            return new Vector2(x, y);
        }

        return target.getTransform().getPosition();
    }

    /**
     * Calculates a separation force to push this enemy away from nearby allies.
     * This prevents enemy bunching and creates natural flocking behavior.
     *
     * @return A vector representing the separation force (normalized direction * strength)
     */
    protected Vector2 calculateSeparationForce() {
        if (separation == null) {
            return new Vector2(0, 0);
        }

        Vector2 separationForce = new Vector2(0, 0);
        int nearbyCount = 0;
        Vector2 averagePosition = new Vector2(0, 0);

        // Find all nearby enemies
        for (var obj : world.getGameObjects()) {
            if (obj instanceof EnemyEntity && obj != this && obj.isActive()) {
                EnemyEntity other = (EnemyEntity) obj;

                float distance = transform.getPosition().dst(other.getTransform().getPosition());

                // Check if within separation radius
                if (distance < separation.getSeparationRadius() && distance > 0.1f) {
                    averagePosition.add(other.getTransform().getPosition());
                    nearbyCount++;
                }
            }
        }

        // If there are nearby enemies, calculate force away from their average position
        if (nearbyCount > 0) {
            averagePosition.scl(1f / nearbyCount); // Calculate average

            // Direction away from average position
            separationForce.set(
                transform.getX() - averagePosition.x,
                transform.getY() - averagePosition.y
            );

            // Normalize and apply strength
            if (separationForce.len() > 0) {
                separationForce.nor().scl(separation.getSeparationStrength());
            }
        }

        return separationForce;
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
     * Performs an attack using the unified attack system.
     */
    protected void performAttack(float delta) {
        // Start attack on first frame of ATTACK state
        if (ai.getStateTimer() < delta) {
            // Calculate attack direction toward player
            if (target != null) {
                Transform targetTransform = target.getComponent(Transform.class);
                if (targetTransform != null) {
                    float enemyCenterX = transform.getX() + SIZE / 2f;
                    float enemyCenterY = transform.getY() + SIZE / 2f;
                    float playerCenterX = targetTransform.getX() + SIZE / 2f;
                    float playerCenterY = targetTransform.getY() + SIZE / 2f;

                    float attackAngle = com.game.systems.combat.CombatUtils.calculateAngle(
                        enemyCenterX, enemyCenterY,
                        playerCenterX, playerCenterY
                    );

                    WeaponStats weapon = getWeaponStats();
                    attackComponent.startAttack(weapon, attackAngle);
                }
            }
        }

        // Apply movement penalty while attacking
        if (attackComponent.isAttacking() && attackComponent.getCurrentWeapon() != null) {
            // Slow down or stop during attack
            float movementMultiplier = attackComponent.getCurrentWeapon().getMovementMultiplier();
            velocity.setVelocity(velocity.getVelocity().scl(movementMultiplier));
        }

        // Update attack system (checks for hits during ACTIVE phase)
        if (attackComponent.isAttacking()) {
            java.util.List<com.game.systems.entity.GameObject> targets = new java.util.ArrayList<>();
            if (target != null) {
                targets.add(target);
            }

            com.game.systems.combat.AttackSystem.updateAttack(
                this,
                attackComponent,
                targets,
                this::spawnDamageNumber
            );
        }

        // Return to chase when attack is complete
        if (!attackComponent.isAttacking() && ai.getStateTimer() > 0.2f) {
            ai.setState(AIComponent.AIState.CHASE);
        }
    }

    /**
     * Spawns a damage number at the specified position.
     */
    private void spawnDamageNumber(float x, float y, int damage) {
        if (damageNumberCallback == null) return;
        damageNumberCallback.spawnDamageNumber(x, y, damage);
    }

    /**
     * Gets the attack damage for this enemy. Override in subclasses.
     */
    protected int getAttackDamage() {
        return 2; // Default enemy damage
    }

    /**
     * Gets the weapon stats for this enemy's attack.
     * Override in subclasses to customize weapon behavior.
     */
    protected WeaponStats getWeaponStats() {
        // Default enemy weapon: simple melee attack
        return new WeaponStats(
            com.game.systems.combat.WeaponType.DAGGER, // Fast attack type
            getAttackDamage(),  // Damage
            1.0f,               // 1 attack per second
            ai.getAttackRange(), // Range from AI component
            80f,                // Knockback
            0.2f,               // Windup duration
            0.15f,              // Swing duration
            0.15f,              // Recovery duration
            120f,               // 120-degree arc
            0.3f                // 30% movement speed while attacking
        );
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

        // Generate and spawn loot drops
        if (com.game.systems.loot.LootSystem.isInitialized()) {
            com.game.systems.loot.LootSystem.getInstance().generateAndSpawnLoot(this, target);
        }

        // Notify death callback to spawn animation and remove enemy
        if (deathCallback != null) {
            float centerX = transform.getX() + 8f;
            float centerY = transform.getY() + 8f;
            deathCallback.onDeath(this, centerX, centerY);
        }
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

    public Array<Vector2> getCurrentPath() {
        return currentPath;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    /**
     * Sets the damage number callback for spawning floating damage numbers.
     */
    public void setDamageNumberCallback(DamageNumberCallback callback) {
        this.damageNumberCallback = callback;
    }

    /**
     * Sets the death callback for spawning death animations.
     */
    public void setDeathCallback(DeathCallback callback) {
        this.deathCallback = callback;
    }

    /**
     * Callback interface for spawning damage numbers.
     */
    public interface DamageNumberCallback {
        void spawnDamageNumber(float x, float y, int damage);
    }

    /**
     * Callback interface for handling enemy death.
     */
    public interface DeathCallback {
        void onDeath(EnemyEntity enemy, float x, float y);
    }

    // Tag system methods for context-aware loot drops

    /**
     * Adds a tag to this enemy.
     * Tags are used for context-aware loot drops and other systems.
     * Example tags: "animal:cow", "undead:skeleton", "humanoid:goblin"
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * Checks if this enemy has a specific tag.
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Removes a tag from this enemy.
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * Gets all tags on this enemy.
     */
    public Set<String> getTags() {
        return new HashSet<>(tags);
    }
}
