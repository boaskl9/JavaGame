package com.game.systems.entity;

/**
 * Base class for living/interactive entities (Player, NPCs, Enemies).
 * Provides common functionality like health, stats, etc.
 *
 * This is different from GameObject - GameObject is the low-level component container,
 * while Entity adds game-specific features for living things.
 */
public abstract class Entity extends GameObject {

    private int health;
    private int maxHealth;

    public Entity(int maxHealth) {
        super();
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    /**
     * Damage this entity.
     * @param amount Amount of damage to apply
     * @return true if the entity died from this damage
     */
    public boolean damage(int amount) {
        health -= amount;
        if (health < 0) {
            health = 0;
        }

        if (health == 0) {
            onDeath();
            return true;
        }

        return false;
    }

    /**
     * Heal this entity.
     * @param amount Amount of health to restore
     */
    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    /**
     * Check if this entity is alive.
     */
    public boolean isAlive() {
        return health > 0;
    }

    /**
     * Called when this entity dies.
     * Override to implement death behavior.
     */
    protected void onDeath() {
        setActive(false);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    /**
     * Get health as a percentage (0.0 to 1.0).
     */
    public float getHealthPercent() {
        return maxHealth > 0 ? (float) health / maxHealth : 0f;
    }
}
