package com.game.components;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.Component;

/**
 * AI component for enemy behavior.
 * Supports different AI states and behaviors.
 */
public class AIComponent implements Component {
    public enum AIState {
        IDLE,        // Standing still or wandering
        WANDER,      // Random movement
        CHASE,       // Chasing target
        ATTACK,      // Attacking target
        FLEE,        // Running away
        PATROL       // Following waypoints
    }

    private AIState currentState;
    private float stateTimer;           // Time in current state
    private float detectionRange;       // How far can this entity detect the player
    private float attackRange;          // How close to attack
    private float moveSpeed;            // Movement speed
    private float wanderInterval;      // Time between wander direction changes
    private float idleTime;            // How long to stay idle

    private Vector2 wanderDirection;
    private float wanderTimer;
    private float idleTimer;

    // Target tracking
    private Vector2 targetPosition;
    private boolean hasTarget;

    public AIComponent() {
        this.currentState = AIState.IDLE;
        this.stateTimer = 0f;
        this.detectionRange = 100f;
        this.attackRange = 20f;
        this.moveSpeed = 40f;
        this.wanderInterval = 2f;
        this.idleTime = 1.5f;
        this.wanderDirection = new Vector2();
        this.wanderTimer = 0f;
        this.idleTimer = 0f;
        this.targetPosition = new Vector2();
        this.hasTarget = false;
    }

    @Override
    public void update(float delta) {
        stateTimer += delta;

        // Update state-specific timers
        switch (currentState) {
            case WANDER:
                wanderTimer += delta;
                break;
            case IDLE:
                idleTimer += delta;
                break;
        }
    }

    @Override
    public void onAttach() {
        // No-op
    }

    @Override
    public void onDetach() {
        // No-op
    }

    /**
     * Sets the AI state and resets the state timer.
     */
    public void setState(AIState state) {
        if (this.currentState != state) {
            this.currentState = state;
            this.stateTimer = 0f;

            // Reset state-specific timers
            if (state == AIState.IDLE) {
                idleTimer = 0f;
            } else if (state == AIState.WANDER) {
                wanderTimer = 0f;
            }
        }
    }

    public AIState getState() {
        return currentState;
    }

    public float getStateTimer() {
        return stateTimer;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public void setDetectionRange(float range) {
        this.detectionRange = range;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(float range) {
        this.attackRange = range;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float speed) {
        this.moveSpeed = speed;
    }

    public float getWanderInterval() {
        return wanderInterval;
    }

    public void setWanderInterval(float interval) {
        this.wanderInterval = interval;
    }

    public float getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(float time) {
        this.idleTime = time;
    }

    public Vector2 getWanderDirection() {
        return wanderDirection;
    }

    public void setWanderDirection(float x, float y) {
        this.wanderDirection.set(x, y);
    }

    public float getWanderTimer() {
        return wanderTimer;
    }

    public float getIdleTimer() {
        return idleTimer;
    }

    public void resetWanderTimer() {
        this.wanderTimer = 0f;
    }

    public void resetIdleTimer() {
        this.idleTimer = 0f;
    }

    public Vector2 getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Vector2 position) {
        this.targetPosition.set(position);
        this.hasTarget = true;
    }

    public void setTargetPosition(float x, float y) {
        this.targetPosition.set(x, y);
        this.hasTarget = true;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    public boolean hasTarget() {
        return hasTarget;
    }
}
