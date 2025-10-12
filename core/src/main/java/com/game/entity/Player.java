package com.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.game.entity.animation.*;
import com.game.world.GameWorld;

public class Player implements Entity {
    private static final float WALK_SPEED = 80f;   // pixels per second
    private static final float RUN_SPEED = 160f;   // pixels per second

    private int sizeX, sizeY;

    private Vector2 position;
    private Vector2 velocity;
    private GameWorld world;

    private AnimationController animController;
    private Direction lastDirection;

    public Player(GameWorld world, int startGridX, int startGridY) {
        this.world = world;
        this.position = world.gridToWorld(startGridX, startGridY);
        this.velocity = new Vector2();
        this.lastDirection = Direction.DOWN;

        sizeX = 16;
        sizeY = 16;

        String spriteClass = "GladiatorBlue";

        String walkPath = String.format("Actor/Characters/%s/SeparateAnim/Walk.png", spriteClass);
        String idlePath = String.format("Actor/Characters/%s/SeparateAnim/Idle.png", spriteClass);

        // Load textures
        Texture walkSpriteSheet = new Texture(Gdx.files.internal(walkPath));
        Texture idleSpriteSheet = new Texture(Gdx.files.internal(idlePath));

        // Setup animation controller
        animController = new AnimationController();
        animController.loadAnimationSet(MovementStates.WALKING, walkSpriteSheet, 4, 0.22f);
        animController.loadAnimationSet(MovementStates.RUNNING, walkSpriteSheet, 4, 0.1f); // Faster animation
        animController.loadAnimationSet(MovementStates.IDLE, idleSpriteSheet, 1, 0.3f);
    }

    @Override
    public void update(float delta) {
        animController.update(delta);

        // Get input
        Vector2 inputVelocity = new Vector2();
        boolean isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

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

        // Determine movement state
        MovementStates currentState;
        if (inputVelocity.len() > 0) {
            // Normalize and apply speed
            float speed = isRunning ? RUN_SPEED : WALK_SPEED;
            inputVelocity.nor().scl(speed);
            velocity.set(inputVelocity);

            currentState = isRunning ? MovementStates.RUNNING : MovementStates.WALKING;

            // Update last direction when moving
            lastDirection = animController.getCurrentDirection();
        } else {
            velocity.set(0, 0);
            currentState = MovementStates.IDLE;
        }

        // Update animation
        animController.updateAnimation(velocity, currentState, lastDirection);

        // Apply movement with collision
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
        int tileSize = world.getTileSize();

        // Use pixel-perfect collision with a slightly smaller hitbox
        float hitboxWidth = tileSize - 4;
        float hitboxHeight = tileSize - 4;

        return world.isPositionWalkable(worldX + 2, worldY + 2, hitboxWidth, hitboxHeight);
    }

    @Override
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = animController.getCurrentFrame();
        if (currentFrame != null) {
            batch.draw(currentFrame, position.x, position.y,
                sizeX, sizeY);
        }
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

    public AnimationController getAnimController() {
        return animController;
    }
}
