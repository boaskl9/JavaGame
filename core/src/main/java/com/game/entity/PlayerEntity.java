package com.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.game.components.AnimationComponent;
import com.game.components.ColliderComponent;
import com.game.components.ItemMagnetComponent;
import com.game.components.RenderComponent;
import com.game.components.VelocityComponent;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.inventory.PlayerInventory;

/**
 * Player entity built using the new component-based architecture.
 * Extends Entity to get health and living entity features.
 */
public class PlayerEntity extends com.game.systems.entity.Entity {
    private static final float WALK_SPEED = 80f;
    private static final float RUN_SPEED = 160f;
    private static final int SIZE = 16;
    private static final int DEFAULT_MAX_HEALTH = 100;

    private WorldManager world;
    private int lastDirectionAngle = 180; // Down

    // Component references (cached for performance)
    private Transform transform;
    private VelocityComponent velocity;
    private AnimationComponent animation;
    private ColliderComponent environmentCollider;  // For walls, trees (feet only)
    private ColliderComponent combatCollider;       // For enemies, projectiles (full body)
    private ItemMagnetComponent itemMagnet;

    // Inventory
    private PlayerInventory inventory;

    public PlayerEntity(WorldManager world, float x, float y) {
        super(DEFAULT_MAX_HEALTH);
        this.world = world;

        // Add components
        transform = new Transform(x, y);
        addComponent(transform);

        velocity = new VelocityComponent();
        addComponent(velocity);

        animation = new AnimationComponent();
        addComponent(animation);

        environmentCollider = new ColliderComponent(SIZE * 0.5f, SIZE * 0.25f, SIZE * 0.25f, 0);
        addComponent(environmentCollider);

        // Combat collider - full body hitbox for enemy attacks
        // This would be used when enemies attack the player
        combatCollider = new ColliderComponent(SIZE - 4, SIZE - 4, 2, 2);
        // Note: Don't add to components yet - we'll add it later when we implement combat

        RenderComponent render = new RenderComponent(SIZE, SIZE);
        addComponent(render);

        // Add item magnet
        itemMagnet = new ItemMagnetComponent();
        itemMagnet.setOwner(this);
        addComponent(itemMagnet);

        // Initialize inventory
        inventory = new PlayerInventory();

        // Load animations
        loadAnimations();
    }

    private void loadAnimations() {
        String spriteClass = "Villager5";
        String walkPath = String.format("Actor/Characters/%s/SeparateAnim/Walk.png", spriteClass);
        String idlePath = String.format("Actor/Characters/%s/SeparateAnim/Idle.png", spriteClass);

        Texture walkTexture = new Texture(Gdx.files.internal(walkPath));
        Texture idleTexture = new Texture(Gdx.files.internal(idlePath));

        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "walk", walkTexture, 4, 0.22f);
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "run", walkTexture, 4, 0.1f);
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "idle", idleTexture, 1, 0.3f);
    }

    @Override
    public void update(float delta) {
        handleInput();
        updateAnimation();
        applyMovement(delta);

        // Update all components
        super.update(delta);
    }

    private void handleInput() {
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

        // Apply speed
        if (inputVelocity.len() > 0) {
            float speed = isRunning ? RUN_SPEED : WALK_SPEED;
            inputVelocity.nor().scl(speed);
        }

        velocity.setVelocity(inputVelocity);
    }

    private void updateAnimation() {
        Vector2 vel = velocity.getVelocity();
        String state;
        boolean isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        if (vel.len() > 0) {
            state = isRunning ? "run" : "walk";
            lastDirectionAngle = getDirectionAngle(vel);
        } else {
            state = "idle";
        }

        // Determine if we should flip horizontally (for right-facing)
        boolean flip = lastDirectionAngle == 270;

        animation.setState(state, lastDirectionAngle, flip);
    }

    private int getDirectionAngle(Vector2 direction) {
        float angle = direction.angleDeg();
        if (angle < 0) angle += 360;

        if (angle >= 315 || angle < 45) return 270;      // Right
        else if (angle >= 45 && angle < 135) return 0;   // Up
        else if (angle >= 135 && angle < 225) return 90; // Left
        else return 180; // Down
    }

    private void applyMovement(float delta) {
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

    private boolean isPositionWalkable(float x, float y) {
        // Use the environment collider (feet) for wall/terrain collision
        float hitboxWidth = environmentCollider.getWidth();
        float hitboxHeight = environmentCollider.getHeight();
        float offsetX = environmentCollider.getOffsetX();
        float offsetY = environmentCollider.getOffsetY();

        return world.isPositionWalkable(x + offsetX, y + offsetY, hitboxWidth, hitboxHeight);
    }

    /**
     * Get the combat hitbox for enemy attacks.
     * This is the full body hitbox.
     */
    public ColliderComponent getCombatCollider() {
        return combatCollider;
    }

    /**
     * Get the environment collider (feet).
     * This is used for walls and terrain.
     */
    public ColliderComponent getEnvironmentCollider() {
        return environmentCollider;
    }

    public void setWorld(WorldManager world) {
        this.world = world;
    }

    public Transform getTransform() {
        return transform;
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public ItemMagnetComponent getItemMagnet() {
        return itemMagnet;
    }
}
