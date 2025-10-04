package com.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.game.main.GameWorld;

/**
 * Player character with fluid movement on a grid-based world
 */
public class Player implements Entity {
    private static final float MOVE_SPEED = 120f; // pixels per second

    private Vector2 position;      // pixel position (can be between grid cells)
    private Vector2 velocity;      // current velocity

    private GameWorld world;

    // Animation - 5 rows for different angles
    private Animation<TextureRegion> anim0;   // 0° - front
    private Animation<TextureRegion> anim45;  // 45° - front-right diagonal
    private Animation<TextureRegion> anim90;  // 90° - right side
    private Animation<TextureRegion> anim135; // 135° - back-right diagonal
    private Animation<TextureRegion> anim180; // 180° - back

    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private boolean flipX; // Whether to flip the sprite horizontally

    private Direction lastDirection;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT,
        UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT,
        IDLE
    }

    public Player(GameWorld world, int startGridX, int startGridY, Texture spriteSheet) {
        this.world = world;
        this.position = world.gridToWorld(startGridX, startGridY);
        this.velocity = new Vector2();
        this.lastDirection = Direction.DOWN;
        this.stateTime = 0;
        this.flipX = false;

        setupAnimations(spriteSheet);
        currentAnimation = anim0;
    }

    private void setupAnimations(Texture spriteSheet) {
        // Sprite sheet: 4 columns x 5 rows
        // Row 0: 0° (front), Row 1: 45°, Row 2: 90° (side), Row 3: 135°, Row 4: 180° (back)
        int frameWidth = spriteSheet.getWidth() / 4;
        int frameHeight = spriteSheet.getHeight() / 5;

        TextureRegion[][] frames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        // Create animations (0.15f = frame duration for smooth animation)
        anim0 = new Animation<>(0.15f, frames[0]);     // Front view
        anim45 = new Animation<>(0.15f, frames[1]);    // 45° angle
        anim90 = new Animation<>(0.15f, frames[2]);    // Side view
        anim135 = new Animation<>(0.15f, frames[3]);   // 135° angle
        anim180 = new Animation<>(0.15f, frames[4]);   // Back view
    }

    @Override
    public void update(float delta) {
        stateTime += delta;

        // Get input
        Vector2 inputVelocity = new Vector2();

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            inputVelocity.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            inputVelocity.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            inputVelocity.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            inputVelocity.x += 1;
        }

        // Normalize diagonal movement so you don't move faster diagonally
        if (inputVelocity.len() > 0) {
            inputVelocity.nor().scl(MOVE_SPEED);
            velocity.set(inputVelocity);

            // Update animation based on direction
            updateAnimation(inputVelocity);
        } else {
            velocity.set(0, 0);
        }

        // Calculate new position
        Vector2 newPosition = new Vector2(position);
        newPosition.add(velocity.x * delta, velocity.y * delta);

        // Check collision on X axis
        if (velocity.x != 0) {
            if (isPositionWalkable(newPosition.x, position.y)) {
                position.x = newPosition.x;
            }
        }

        // Check collision on Y axis
        if (velocity.y != 0) {
            if (isPositionWalkable(position.x, newPosition.y)) {
                position.y = newPosition.y;
            }
        }
    }

    private boolean isPositionWalkable(float worldX, float worldY) {
        // Check the four corners of the player sprite for collision
        int tileSize = world.getTileSize();
        float halfSize = tileSize * 0.4f; // Slightly smaller hitbox for smoother movement

        Vector2 topLeft = world.worldToGrid(worldX + 2, worldY + tileSize - 2);
        Vector2 topRight = world.worldToGrid(worldX + tileSize - 2, worldY + tileSize - 2);
        Vector2 bottomLeft = world.worldToGrid(worldX + 2, worldY + 2);
        Vector2 bottomRight = world.worldToGrid(worldX + tileSize - 2, worldY + 2);

        return world.isWalkable((int)topLeft.x, (int)topLeft.y) &&
            world.isWalkable((int)topRight.x, (int)topRight.y) &&
            world.isWalkable((int)bottomLeft.x, (int)bottomLeft.y) &&
            world.isWalkable((int)bottomRight.x, (int)bottomRight.y);
    }

    private void updateAnimation(Vector2 direction) {
        // Determine direction based on velocity angle
        float angle = direction.angleDeg();

        // Normalize angle to 0-360
        if (angle < 0) angle += 360;

        // Map angles to animations (8-directional)
        if (angle >= 337.5 || angle < 22.5) {
            // Left
            currentAnimation = anim90;
            flipX = true;
            lastDirection = Direction.LEFT;
        } else if (angle >= 22.5 && angle < 67.5) {
            // Up-Left diagonal
            currentAnimation = anim135;
            flipX = true;
            lastDirection = Direction.UP_LEFT;
        } else if (angle >= 67.5 && angle < 112.5) {
            // Up
            currentAnimation = anim180;
            flipX = false;
            lastDirection = Direction.UP;
        } else if (angle >= 112.5 && angle < 157.5) {
            // Up-Right diagonal
            currentAnimation = anim135;
            flipX = false;
            lastDirection = Direction.UP_RIGHT;
        } else if (angle >= 157.5 && angle < 202.5) {
            // Right
            currentAnimation = anim90;
            flipX = false;
            lastDirection = Direction.RIGHT;
        } else if (angle >= 202.5 && angle < 247.5) {
            // Down-Right diagonal
            currentAnimation = anim45;
            flipX = false;
            lastDirection = Direction.DOWN_RIGHT;
        } else if (angle >= 247.5 && angle < 292.5) {
            // Down
            currentAnimation = anim0;
            flipX = false;
            lastDirection = Direction.DOWN;
        } else if (angle >= 292.5 && angle < 337.5) {
            // Down-Left diagonal
            currentAnimation = anim45;
            flipX = true;
            lastDirection = Direction.DOWN_LEFT;
        }

        // Reset animation when direction changes
        stateTime = 0;
    }

    @Override
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        // Handle horizontal flipping
        if (currentFrame.isFlipX() != flipX) {
            currentFrame.flip(true, false);
        }

        batch.draw(currentFrame, position.x, position.y, world.getTileSize(), world.getTileSize());
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    @Override
    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }

    public Vector2 getGridPosition() {
        return world.worldToGrid(position.x, position.y);
    }

    public boolean isMoving() {
        return velocity.len() > 0;
    }
}
