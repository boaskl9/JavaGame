package com.game.systems.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone sprite animation system that can be used in any project.
 * No dependencies on entities, worlds, or game-specific logic.
 */
public class SpriteAnimator {
    private Map<AnimationKey, Animation<TextureRegion>> animations;
    private Animation<TextureRegion> currentAnimation;
    private String currentState;
    private int currentDirection;
    private float stateTime;
    private boolean flipX;

    public SpriteAnimator() {
        animations = new HashMap<>();
        currentState = "";
        currentDirection = 0;
        stateTime = 0;
        flipX = false;
    }

    /**
     * Register an animation for a specific state and direction angle.
     * @param stateName Name of the state (e.g., "walk", "idle", "run")
     * @param directionAngle Direction in degrees (0=down, 90=left, 180=up, 270=right)
     * @param frames Array of texture regions for this animation
     * @param frameDuration Duration of each frame in seconds
     */
    public void addAnimation(String stateName, int directionAngle, TextureRegion[] frames, float frameDuration) {
        AnimationKey key = new AnimationKey(stateName, directionAngle);
        animations.put(key, new Animation<>(frameDuration, frames));
    }

    /**
     * Set the current animation based on state and direction vector.
     * @param stateName The animation state to play
     * @param direction Direction vector (will be converted to angle)
     * @param flipHorizontal Whether to flip the sprite horizontally
     */
    public void setState(String stateName, Vector2 direction, boolean flipHorizontal) {
        int angle = vectorToAngle(direction);
        setState(stateName, angle, flipHorizontal);
    }

    /**
     * Set the current animation based on state and direction angle.
     * @param stateName The animation state to play
     * @param directionAngle Direction in degrees
     * @param flipHorizontal Whether to flip the sprite horizontally
     */
    public void setState(String stateName, int directionAngle, boolean flipHorizontal) {
        // Reset animation timer if state or direction changed
        if (!stateName.equals(currentState) || directionAngle != currentDirection) {
            stateTime = 0;
            currentState = stateName;
            currentDirection = directionAngle;
        }

        AnimationKey key = new AnimationKey(stateName, directionAngle);
        Animation<TextureRegion> newAnimation = animations.get(key);

        if (newAnimation != null) {
            currentAnimation = newAnimation;
        }

        flipX = flipHorizontal;
    }

    /**
     * Update the animation timer.
     * @param delta Time since last update in seconds
     */
    public void update(float delta) {
        stateTime += delta;
    }

    /**
     * Get the current animation frame.
     * @return Current texture region to render
     */
    public TextureRegion getCurrentFrame() {
        if (currentAnimation == null) {
            return null;
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        // Handle horizontal flipping
        if (currentFrame.isFlipX() != flipX) {
            currentFrame.flip(true, false);
        }

        return currentFrame;
    }

    /**
     * Convert a direction vector to an angle in degrees.
     */
    private int vectorToAngle(Vector2 direction) {
        if (direction.len() == 0) {
            return currentDirection; // Maintain last direction if no movement
        }

        float angle = direction.angleDeg();
        if (angle < 0) angle += 360;

        // Round to nearest cardinal/diagonal direction
        if (angle >= 337.5 || angle < 22.5) return 270;      // Right
        else if (angle >= 22.5 && angle < 67.5) return 315;  // Up-Right
        else if (angle >= 67.5 && angle < 112.5) return 0;   // Up
        else if (angle >= 112.5 && angle < 157.5) return 45; // Up-Left
        else if (angle >= 157.5 && angle < 202.5) return 90; // Left
        else if (angle >= 202.5 && angle < 247.5) return 135;// Down-Left
        else if (angle >= 247.5 && angle < 292.5) return 180;// Down
        else return 225; // Down-Right
    }

    public String getCurrentState() {
        return currentState;
    }

    public int getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Internal key class for storing animations.
     */
    private static class AnimationKey {
        String state;
        int angle;

        AnimationKey(String state, int angle) {
            this.state = state;
            this.angle = angle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnimationKey that = (AnimationKey) o;
            return angle == that.angle && state.equals(that.state);
        }

        @Override
        public int hashCode() {
            return 31 * state.hashCode() + angle;
        }
    }
}
