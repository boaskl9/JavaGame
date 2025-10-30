package com.game.systems.entity;

import com.game.components.HealthComponent;

/**
 * Base class for living/interactive entities (Player, NPCs, Enemies).
 * Provides common functionality like health, stats, etc.
 *
 * This is different from GameObject - GameObject is the low-level component container,
 * while Entity adds game-specific features for living things.
 *
 * Health is managed via HealthComponent - all health operations delegate to it.
 */
public abstract class Entity extends GameObject {

    public Entity(int maxHealth) {
        super();
        // Add HealthComponent to manage health
        HealthComponent healthComponent = new HealthComponent(maxHealth);
        addComponent(healthComponent);
    }

    /**
     * Gets the HealthComponent for this entity.
     * @return The HealthComponent, or null if not found
     */
    protected HealthComponent getHealthComponent() {
        return getComponent(HealthComponent.class);
    }

    /**
     * Damage this entity.
     * @param amount Amount of damage to apply
     * @return true if the entity died from this damage
     */
    public boolean damage(int amount) {
        HealthComponent health = getHealthComponent();
        if (health == null) {
            return false;
        }

        boolean died = health.damage(amount);

        if (died) {
            onDeath();
        }

        return died;
    }

    /**
     * Heal this entity.
     * @param amount Amount of health to restore
     */
    public void heal(int amount) {
        HealthComponent health = getHealthComponent();
        if (health != null) {
            health.heal(amount);
        }
    }

    /**
     * Check if this entity is alive.
     */
    public boolean isAlive() {
        HealthComponent health = getHealthComponent();
        return health != null && health.isAlive();
    }

    /**
     * Called when this entity dies.
     * Override to implement death behavior.
     */
    protected void onDeath() {
        setActive(false);
    }

    public int getHealth() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getCurrentHealth() : 0;
    }

    public void setHealth(int health) {
        HealthComponent healthComp = getHealthComponent();
        if (healthComp != null) {
            healthComp.setHealth(health);
        }
    }

    public int getMaxHealth() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getMaxHealth() : 0;
    }

    public void setMaxHealth(int maxHealth) {
        HealthComponent health = getHealthComponent();
        if (health != null) {
            health.setMaxHealth(maxHealth);
        }
    }

    /**
     * Get health as a percentage (0.0 to 1.0).
     */
    public float getHealthPercent() {
        HealthComponent health = getHealthComponent();
        return health != null ? health.getHealthPercent() : 0f;
    }
}
