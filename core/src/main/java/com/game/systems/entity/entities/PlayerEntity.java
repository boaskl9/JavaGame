package com.game.systems.entity.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.game.components.AnimationComponent;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.components.HealthComponent;
import com.game.components.ItemMagnetComponent;
import com.game.components.RenderComponent;
import com.game.components.VelocityComponent;
import com.game.components.WeaponRenderComponent;
import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.audio.SoundSystem;
import com.game.systems.combat.WeaponStats;
import com.game.systems.entity.Transform;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.inventory.PlayerInventory;
import com.game.systems.item.ItemStack;
import com.game.systems.loot.LootSystem;
import com.game.systems.input.InputSource;

import static com.game.systems.audio.SoundRegistry.*;

/**
 * Player entity built using the new component-based architecture.
 * Extends Entity to get health and living entity features.
 *
 * MULTIPLAYER READY: No longer a singleton!
 * - Each player has a unique playerId
 * - Input handled via InputSource abstraction (supports local keyboard, network, AI, etc.)
 */
public class PlayerEntity extends com.game.systems.entity.Entity {
    private static final float WALK_SPEED = 80f;
    private static final float RUN_SPEED = 160f;
    private static final int SIZE = 16;
    private static final int DEFAULT_MAX_HEALTH = 28;

    // Multiplayer fields
    private int playerId = -1; // Assigned by PlayerManager
    private InputSource inputSource; // Input abstraction for local/network play

    private WorldManager world;
    private int lastDirectionAngle = 180; // Down

    // Component references (cached for performance)
    private Transform transform;
    private VelocityComponent velocity;
    private AnimationComponent animation;
    private ColliderComponent environmentCollider;  // For walls, trees (feet only)
    private ColliderComponent combatCollider;       // For enemies, projectiles (full body)
    private ItemMagnetComponent itemMagnet;
    private HealthComponent health;
    private AttackComponent attackComponent;
    private WeaponRenderComponent weaponRenderer;

    // Inventory
    private PlayerInventory inventory;

    // Damage number callback
    private DamageNumberCallback damageNumberCallback;

    // Debug: Store last attack hitbox for visualization
    private Rectangle lastAttackHitbox;

    /**
     * Create a new player entity.
     * NOTE: Use PlayerManager.addPlayer() to properly register the player.
     *
     * @param world The world manager
     * @param x Starting X position
     * @param y Starting Y position
     */
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

        // Environment collider - used for wall/obstacle collision (feet only)
        // NOT added to components - accessed via field reference by collision system
        environmentCollider = new ColliderComponent(SIZE * 0.5f, SIZE * 0.25f, SIZE * 0.25f, 0);

        // Combat collider - full body hitbox for enemy attacks
        // This IS added to components so attack system can find it via getComponent()
        combatCollider = new ColliderComponent(SIZE - 4, SIZE - 4, 2, 2);
        addComponent(combatCollider);

        RenderComponent render = new RenderComponent(SIZE, SIZE);
        addComponent(render);

        // Add item magnet
        itemMagnet = new ItemMagnetComponent();
        itemMagnet.setOwner(this);
        addComponent(itemMagnet);

        // Health component is added by Entity constructor (6 hearts = 12 HP)
        health = getComponent(HealthComponent.class);

        // Add attack component
        attackComponent = new AttackComponent();
        addComponent(attackComponent);

        // Add weapon renderer (for animated weapons)
        weaponRenderer = new WeaponRenderComponent();
        addComponent(weaponRenderer);

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

        // Load attack animation (slash effect)
        String attackPath = String.format("Actor/Characters/%s/SeparateAnim/Attack.png", spriteClass);
        Texture attackTexture = new Texture(Gdx.files.internal(attackPath));
        AnimationBuilder.loadFourDirectional(animation.getAnimator(), "attack", attackTexture, 1, 0.5f);
    }

    @Override
    public void update(float delta) {
        // Update input source
        if (inputSource != null) {
            inputSource.update(delta);
        }

        // Update velocity component (handles knockback decay)
        velocity.update(delta);

        handleInput();
        handleAttack();

        // Update attack system (checks for hits during ACTIVE phase)
        if (attackComponent.isAttacking()) {
            com.game.systems.combat.AttackSystem.updateAttack(
                this,
                attackComponent,
                world.getGameObjects(),
                this::spawnDamageNumber
            );

            // Update attack hitbox for debug rendering
            if (attackComponent.getCurrentWeapon() != null) {
                com.game.systems.combat.AttackStrategy strategy =
                    com.game.systems.combat.AttackSystem.getStrategy(
                        attackComponent.getCurrentWeapon().getType()
                    );
                lastAttackHitbox = strategy.getHitbox(this, attackComponent,
                                                      attackComponent.getCurrentWeapon());
            }
        } else {
            // Clear attack hitbox when not attacking
            lastAttackHitbox = null;
        }

        updateAnimation();
        applyMovement(delta);

        // Update all components
        super.update(delta);
    }

    private void handleInput() {
        Vector2 inputVelocity = new Vector2();

        // Get input from InputSource
        if (inputSource != null && inputSource.isEnabled()) {
            Vector2 movementInput = inputSource.getMovementInput();
            boolean isRunning = inputSource.isRunning();

            // Apply speed
            if (movementInput.len() > 0) {
                float speed = isRunning ? RUN_SPEED : WALK_SPEED;
                inputVelocity.set(movementInput).scl(speed);

                // Apply movement penalty while attacking
                if (attackComponent.isAttacking() && attackComponent.getCurrentWeapon() != null) {
                    float movementMultiplier = attackComponent.getCurrentWeapon().getMovementMultiplier();
                    inputVelocity.scl(movementMultiplier);
                }
            }
        }

        velocity.setVelocity(inputVelocity);
    }

    private void handleAttack() {
        // Check for attack input from InputSource
        if (inputSource != null && inputSource.isEnabled() && inputSource.isAttackJustPressed()) {
            performAttack();
        }
    }

    private void performAttack() {
        // Check if can attack
        if (!attackComponent.canAttack()) {
            return;
        }

        // Get equipped weapon
        ItemStack weaponStack = inventory.getEquipment().getEquipped(EquipmentSlot.WEAPON);
        if (weaponStack == null || weaponStack.getDefinition().getWeaponStats() == null) {
            return; // No weapon equipped
        }

        WeaponStats weapon = weaponStack.getDefinition().getWeaponStats();

        // Get attack direction from InputSource
        Vector2 aimDirection = inputSource != null ? inputSource.getAimDirection() : new Vector2(1, 0);
        float attackAngle = aimDirection.angleDeg();

        // Play weapon swing sound
            SoundSystem.getInstance().playSound(
                SWORD_SWING
            );


        // Start attack (damage will be applied by AttackSystem during ACTIVE phase)
        attackComponent.startAttack(weapon, attackAngle);
    }

    private void updateAnimation() {
        Vector2 vel = velocity.getVelocity();
        String state;
        boolean isRunning = inputSource != null && inputSource.isRunning();
        int animationAngle = lastDirectionAngle;

        // Check if attacking
        if (attackComponent.isAttacking()) {
            state = "attack";
            // Use attack direction for animation
            animationAngle = com.game.systems.combat.CombatUtils.getDirectionalAngle(
                attackComponent.getAttackDirection()
            );
        } else if (vel.len() > 0) {
            state = isRunning ? "run" : "walk";
            lastDirectionAngle = getDirectionAngle(vel);
            animationAngle = lastDirectionAngle;
        } else {
            state = "idle";
            animationAngle = lastDirectionAngle;
        }

        // Determine if we should flip horizontally (for right-facing)
        boolean flip = animationAngle == 270;

        animation.setState(state, animationAngle, flip);
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

    public HealthComponent getHealthComponent() {
        return health;
    }

    /**
     * Get the player ID (assigned by PlayerManager).
     * @return The player ID
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Set the player ID (called by PlayerManager).
     * @param playerId The player ID
     */
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Get the input source for this player.
     * @return The input source
     */
    public InputSource getInputSource() {
        return inputSource;
    }

    /**
     * Set the input source for this player.
     * @param inputSource The input source (LocalKeyboardInput, NetworkInputSource, etc.)
     */
    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    /**
     * Enable or disable player input (for UI dialogs, console, etc.)
     */
    public void setInputEnabled(boolean enabled) {
        if (inputSource != null) {
            inputSource.setEnabled(enabled);
        }
    }

    public boolean isInputEnabled() {
        return inputSource != null && inputSource.isEnabled();
    }

    public AttackComponent getAttackComponent() {
        return attackComponent;
    }

    public WeaponRenderComponent getWeaponRenderer() {
        return weaponRenderer;
    }

    /**
     * Updates the weapon sprite based on equipped weapon.
     * Call this when equipment changes.
     */
    public void updateWeaponSprite() {
        ItemStack weaponStack = inventory.getEquipment().getEquipped(EquipmentSlot.WEAPON);

        if (weaponStack != null && weaponStack.getDefinition().getWeaponSpritePath() != null) {
            // Load and set weapon sprite
            String spritePath = weaponStack.getDefinition().getWeaponSpritePath();
            try {
                Texture weaponTexture = new Texture(Gdx.files.internal(spritePath));
                weaponRenderer.setWeaponSprite(weaponTexture);
                // Pivot point is auto-calculated based on sprite size
            } catch (Exception e) {
                System.err.println("Failed to load weapon sprite: " + spritePath);
                e.printStackTrace();
            }
        } else {
            // No weapon equipped, clear sprite
            weaponRenderer.setWeaponSprite(null);
        }
    }

    /**
     * Gets the current attack hitbox for debug rendering.
     * Returns null when not attacking.
     */
    public Rectangle getAttackHitbox() {
        return lastAttackHitbox;
    }

    /**
     * Spawns a damage number at the specified position.
     * This method signature matches AttackSystem.DamageCallback.
     */
    private void spawnDamageNumber(float x, float y, int damage) {
        if (damageNumberCallback == null) return;
        damageNumberCallback.spawnDamageNumber(x, y, damage);
    }

    /**
     * Sets the damage number callback for spawning floating damage numbers.
     */
    public void setDamageNumberCallback(DamageNumberCallback callback) {
        this.damageNumberCallback = callback;
    }

    /**
     * Callback interface for spawning damage numbers.
     */
    public interface DamageNumberCallback {
        void spawnDamageNumber(float x, float y, int damage);
    }

    // ========== Save/Load Support ==========

    /**
     * Load player state from save data.
     * Updates position and health (inventory is loaded separately via PlayerInventory).
     */
    public void loadFromSaveData(com.game.save.PlayerData data) {
        // Update position
        transform.setPosition(data.x, data.y);

        // Update health
        getHealthComponent().setMaxHealth(data.maxHealth);
        getHealthComponent().setHealth(data.currentHealth);

        System.out.println("PlayerEntity: Loaded from save - Position: (" + data.x + ", " + data.y +
                         "), Health: " + data.currentHealth + "/" + data.maxHealth);
    }
}
