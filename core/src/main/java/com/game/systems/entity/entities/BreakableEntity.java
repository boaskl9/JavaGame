package com.game.systems.entity.entities;

import com.badlogic.gdx.graphics.Texture;
import com.game.components.AnimationComponent;
import com.game.components.ColliderComponent;
import com.game.components.HealthComponent;
import com.game.components.RenderComponent;
import com.game.systems.animation.AnimationBuilder;
import com.game.systems.breakable.BreakableObjectRegistry;
import com.game.systems.entity.Entity;
import com.game.systems.entity.Transform;
import com.game.systems.item.ItemStack;
import com.game.systems.loot.LootTableComponent;

import java.util.List;

/**
 * Abstract base class for all destructible/breakable objects (pots, crates, barrels, etc.).
 *
 * Features:
 * - Takes damage from player attacks (extends Entity with HealthComponent)
 * - Plays break animation on destruction
 * - Spawns loot drops via LootSystem
 * - Spawns particle effects
 * - Configurable via JSON (BreakableObjectRegistry)
 *
 * Subclasses should:
 * - Call super(x, y, objectType) in constructor
 * - Optionally override getBreakAnimationDuration() for custom timing
 */
public abstract class BreakableEntity extends Entity {
    protected static final int DEFAULT_SIZE = 16;

    protected Transform transform;
    protected ColliderComponent environmentCollider;  // For player movement collision (feet area)
    protected ColliderComponent combatCollider;       // For attack damage (full body)
    protected AnimationComponent animation;
    protected RenderComponent render;
    protected LootTableComponent lootTable;

    protected String objectType;
    protected BreakableObjectRegistry.BreakableConfig config;

    protected boolean isBreaking = false;
    protected float breakTimer = 0f;

    // Callback for spawning particles on destruction
    protected ParticleCallback particleCallback;

    /**
     * Creates a breakable entity at the specified position.
     * @param x World X position
     * @param y World Y position
     * @param objectType Type identifier (e.g., "pot", "crate") - must match JSON config
     */
    public BreakableEntity(float x, float y, String objectType) {
        super(1); // Default health, will be overridden by config

        this.objectType = objectType;

        // Load configuration
        this.config = BreakableObjectRegistry.get(objectType);
        if (config == null) {
            System.err.println("No config found for breakable object: " + objectType);
            config = createDefaultConfig();
        }

        // Set health from config
        HealthComponent health = getHealthComponent();
        if (health != null) {
            health.setMaxHealth(config.health);
            health.setHealth(config.health);
        }

        // Add Transform
        transform = new Transform(x, y);
        addComponent(transform);

        // Environment collider - small area at bottom for player walking collision
        // NOT added to components - accessed via field reference
        // Sized to be at bottom of object (like feet)
        environmentCollider = new ColliderComponent(
            DEFAULT_SIZE * 0.75f,  // 12px wide (75% of 16)
            DEFAULT_SIZE * 0.375f, // 6px tall (37.5% of 16)
            DEFAULT_SIZE * 0.125f, // 2px offset X (centered)
            0                       // 0px offset Y (at bottom)
        );

        // Combat collider - full body for taking damage from attacks
        // This IS added to components so AttackSystem can find it
        combatCollider = new ColliderComponent(DEFAULT_SIZE, DEFAULT_SIZE, 0, 0);
        addComponent(combatCollider);

        // Add Render component
        render = new RenderComponent(DEFAULT_SIZE, DEFAULT_SIZE);
        addComponent(render);

        // Add Animation component
        animation = new AnimationComponent();
        addComponent(animation);

        // Load animations
        loadAnimations();

        // Setup loot table from config
        lootTable = new LootTableComponent();
        for (BreakableObjectRegistry.LootEntry entry : config.lootTable) {
            lootTable.addDrop(entry.itemId, entry.chance, entry.minQty, entry.maxQty);
        }
        addComponent(lootTable);
    }

    /**
     * Loads the idle and break animations from config.
     */
    protected void loadAnimations() {
        try {
            // Load idle sprite (single static image)
            Texture idleTexture = new Texture(com.badlogic.gdx.Gdx.files.internal(config.spritePath));
            AnimationBuilder.loadStatic(animation.getAnimator(), "idle", idleTexture, 1, 1);

            // Load break animation (sprite sheet)
            if (config.breakAnimationPath != null) {
                Texture breakTexture = new Texture(com.badlogic.gdx.Gdx.files.internal(config.breakAnimationPath));
                AnimationBuilder.loadHorizontalAnimation(
                    animation.getAnimator(),
                    "break",
                    breakTexture,
                    config.breakAnimationFrames,
                    config.breakAnimationSpeed
                );
            }

            // Set initial state to idle
            animation.setState("idle", 0, false);

        } catch (Exception e) {
            System.err.println("Failed to load animations for " + objectType + ": " + e.getMessage());
        }
    }

    @Override
    public void update(float delta) {
        if (!isAlive() && !isBreaking) {
            return; // Dead and not animating, skip update
        }

        // If breaking, update break animation timer
        if (isBreaking) {
            breakTimer += delta;
            if (breakTimer >= getBreakAnimationDuration()) {
                // Animation complete, deactivate object
                setActive(false);
                isBreaking = false;
            }
        }

        // Update all components
        super.update(delta);
    }

    @Override
    protected void onDeath() {
        // NOTE: Don't call super.onDeath() - it calls setActive(false) immediately!
        // We need to stay active during the break animation, then deactivate after

        if (isBreaking) {
            return; // Already breaking, don't trigger twice
        }

        isBreaking = true;
        breakTimer = 0f;

        // Switch to break animation
        if (animation != null) {
            animation.setState("break", 0, false);
        }

        // Spawn loot drops using LootSystem (respects player equipment modifiers!)
        if (com.game.systems.loot.LootSystem.isInitialized()) {
            float centerX = transform.getX() + DEFAULT_SIZE / 2f;
            float centerY = transform.getY() + DEFAULT_SIZE / 2f;

            // LootSystem handles everything: modifiers, rolling, spawning, scattering
            // Player is null for breakables (no player-specific modifiers needed)
            // Equipment modifiers can be added later if we track who broke the object
            com.game.systems.loot.LootSystem.getInstance().generateAndSpawnLoot(
                this,      // Source GameObject (has LootTableComponent)
                null,      // Player (null = no equipment modifiers)
                centerX,   // Spawn X position
                centerY    // Spawn Y position
            );
        }

        // Spawn particle effect
        if (particleCallback != null) {
            float centerX = transform.getX() + DEFAULT_SIZE / 2f;
            float centerY = transform.getY() + DEFAULT_SIZE / 2f;
            particleCallback.spawnParticles(centerX, centerY, config.particleColor);
        }
    }

    /**
     * Gets the duration of the break animation.
     * Override in subclasses for custom timing.
     */
    protected float getBreakAnimationDuration() {
        return config.getBreakAnimationDuration();
    }

    /**
     * Creates a default config for when JSON config is missing.
     */
    private BreakableObjectRegistry.BreakableConfig createDefaultConfig() {
        BreakableObjectRegistry.BreakableConfig defaultConfig = new BreakableObjectRegistry.BreakableConfig();
        defaultConfig.objectType = objectType;
        defaultConfig.name = "Unknown Object";
        defaultConfig.health = 1;
        defaultConfig.spritePath = "assets/Objects/Pot.png";
        defaultConfig.breakAnimationPath = "";
        defaultConfig.breakAnimationFrames = 1;
        defaultConfig.breakAnimationSpeed = 0.1f;
        defaultConfig.particleColor = "white";
        return defaultConfig;
    }

    /**
     * Sets the particle spawn callback.
     */
    public void setParticleCallback(ParticleCallback callback) {
        this.particleCallback = callback;
    }

    // Getters
    public Transform getTransform() {
        return transform;
    }

    public ColliderComponent getEnvironmentCollider() {
        return environmentCollider;
    }

    public ColliderComponent getCombatCollider() {
        return combatCollider;
    }

    public String getObjectType() {
        return objectType;
    }

    /**
     * Callback interface for spawning particle effects on destruction.
     */
    public interface ParticleCallback {
        void spawnParticles(float x, float y, String particleType);
    }
}
