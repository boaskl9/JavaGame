package com.game.entity.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.game.entity.Player;
import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;

public class AnimationController {
    // Store animations for each state and direction
    private Map<AnimationKey, Animation<TextureRegion>> animations;

    private Animation<TextureRegion> currentAnimation;
    private MovementStates currentState;
    private Direction currentDirection;

    private float stateTime;
    private boolean flipX;

    public AnimationController() {
        animations = new HashMap<>();
        currentState = MovementStates.IDLE;
        currentDirection = Direction.DOWN;
        stateTime = 0;
        flipX = false;
    }

    /**
     * Load a sprite sheet for a specific movement state
     * @param state The movement state (WALKING, RUNNING, IDLE, etc.)
     * @param spriteSheet The texture containing the animation frames
     * @param framesPerDirection Number of animation frames per direction (rows)
     * @param frameDuration How long each frame displays (e.g., 0.15f)
     */
    public void loadAnimationSet(MovementStates state, Texture spriteSheet, int framesPerDirection, float frameDuration) {
        // Sprite sheet layout: 4 columns (down, up, left, right) x framesPerDirection rows
        int frameWidth = spriteSheet.getWidth() / 4;  // 4 columns for 4 directions
        int frameHeight = spriteSheet.getHeight() / framesPerDirection;  // N rows for animation frames

        TextureRegion[][] frames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        // Extract each column as a separate animation
        // Column 0: Down, Column 1: Up, Column 2: Left, Column 3: Right
        TextureRegion[] downFrames = new TextureRegion[framesPerDirection];
        TextureRegion[] upFrames = new TextureRegion[framesPerDirection];
        TextureRegion[] leftFrames = new TextureRegion[framesPerDirection];

        for (int i = 0; i < framesPerDirection; i++) {
            downFrames[i] = frames[i][0];  // Column 0
            upFrames[i] = frames[i][1];    // Column 1
            leftFrames[i] = frames[i][2];  // Column 2 (also used for right with flip)
        }

        // Store animations for each direction
        animations.put(new AnimationKey(state, 0), new Animation<>(frameDuration, downFrames));    // Down
        animations.put(new AnimationKey(state, 180), new Animation<>(frameDuration, upFrames));    // Up
        animations.put(new AnimationKey(state, 90), new Animation<>(frameDuration, leftFrames));   // Left/Right
    }

    /**
     * Load a single-frame sprite sheet (for idle animations, etc.)
     */
    public void loadStaticAnimationSet(MovementStates state, Texture spriteSheet) {
        // For static animations, use a very slow frame rate
        loadAnimationSet(state, spriteSheet, 4,1.0f);
    }

    /**
     * Update the current animation based on movement direction and state
     * @param direction Movement direction vector
     * @param state Current movement state
     * @param lastDirection Last known direction (for maintaining direction when idle)
     */
    public void updateAnimation(Vector2 direction, MovementStates state, Direction lastDirection) {
        Direction newDirection;
        int animAngle;
        boolean newFlipX;

        // If moving, calculate direction from velocity
        if (direction.len() > 0) {
            float angle = direction.angleDeg();
            if (angle < 0) angle += 360;

            // Map angle to direction and animation angle
            if (angle >= 337.5 || angle < 22.5) {
                // Right
                newDirection = Direction.RIGHT;
                animAngle = 90;
                newFlipX = true;
            } else if (angle >= 22.5 && angle < 67.5) {
                // Up-Right diagonal
                newDirection = Direction.UP_RIGHT;
                animAngle = 135;
                newFlipX = true;
            } else if (angle >= 67.5 && angle < 112.5) {
                // Up
                newDirection = Direction.UP;
                animAngle = 180;
                newFlipX = false;
            } else if (angle >= 112.5 && angle < 157.5) {
                // Up-Left diagonal
                newDirection = Direction.UP_LEFT;
                animAngle = 135;
                newFlipX = false;
            } else if (angle >= 157.5 && angle < 202.5) {
                // Left
                newDirection = Direction.LEFT;
                animAngle = 90;
                newFlipX = false;
            } else if (angle >= 202.5 && angle < 247.5) {
                // Down-Left diagonal
                newDirection = Direction.DOWN_LEFT;
                animAngle = 45;
                newFlipX = false;
            } else if (angle >= 247.5 && angle < 292.5) {
                // Down
                newDirection = Direction.DOWN;
                animAngle = 0;
                newFlipX = false;
            } else {
                // Down-Right diagonal
                newDirection = Direction.DOWN_RIGHT;
                animAngle = 45;
                newFlipX = true;
            }
        } else {
            // If idle, maintain last direction
            newDirection = lastDirection;
            animAngle = getAngleForDirection(lastDirection);
            newFlipX = isDirectionFlipped(lastDirection);
        }

        // Reset animation if state or direction changed
        if (state != currentState || newDirection != currentDirection) {
            stateTime = 0;
            currentState = state;
            currentDirection = newDirection;
        }

        // Get the appropriate animation
        AnimationKey key = new AnimationKey(state, animAngle);
        Animation<TextureRegion> newAnimation = animations.get(key);

        if (newAnimation != null) {
            currentAnimation = newAnimation;
        }

        flipX = newFlipX;
    }

    private int getAngleForDirection(Direction dir) {
        switch (dir) {
            case DOWN:
            case DOWN_LEFT:
            case DOWN_RIGHT:
                return dir == Direction.DOWN ? 0 : 45;
            case LEFT:
            case RIGHT:
                return 90;
            case UP_LEFT:
            case UP_RIGHT:
                return 135;
            case UP:
                return 180;
            default:
                return 0;
        }
    }

    private boolean isDirectionFlipped(Direction dir) {
        switch (dir) {
            case RIGHT:
            case UP_RIGHT:
            case DOWN_RIGHT:
                return true;
            default:
                return false;
        }
    }

    public void update(float delta) {
        stateTime += delta;
    }

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

    public MovementStates getCurrentState() {
        return currentState;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Internal key class for storing animations by state and angle
     */
    private static class AnimationKey {
        MovementStates state;
        int angle;

        AnimationKey(MovementStates state, int angle) {
            this.state = state;
            this.angle = angle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnimationKey that = (AnimationKey) o;
            return angle == that.angle && state == that.state;
        }

        @Override
        public int hashCode() {
            return 31 * state.hashCode() + angle;
        }
    }
}
