package com.game.components;

import com.game.systems.entity.Component;

/**
 * Component that manages entity health.
 * Health is represented in quarter-heart increments (4 HP = 1 full heart).
 */
public class HealthComponent implements Component {
    private int currentHealth;
    private int maxHealth;

    /**
     * Creates a health component.
     * @param maxHealth Maximum health (4 HP = 1 full heart)
     */
    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    @Override
    public void update(float delta) {
        // Health doesn't need per-frame updates
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
     * Damages the entity.
     * @param amount Amount of damage (4 = 1 full heart, 1 = quarter heart)
     * @return true if entity died from this damage
     */
    public boolean damage(int amount) {
        currentHealth -= amount;
        if (currentHealth < 0) {
            currentHealth = 0;
        }
        return currentHealth == 0;
    }

    /**
     * Heals the entity.
     * @param amount Amount to heal (4 = 1 full heart, 1 = quarter heart)
     */
    public void heal(int amount) {
        currentHealth += amount;
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }

    /**
     * Sets current health directly.
     */
    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
    }

    /**
     * Sets maximum health and adjusts current health if needed.
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Gets the total number of heart containers (full or empty).
     * Each heart = 4 HP.
     */
    public int getTotalHeartContainers() {
        return (maxHealth + 3) / 4; // Round up to nearest heart
    }

    /**
     * Gets the fill level for a specific heart (0-4).
     * @param heartIndex The heart index (0 = first heart)
     * @return HP in this heart (0-4)
     */
    public int getHeartFillLevel(int heartIndex) {
        int healthInThisHeart = currentHealth - (heartIndex * 4);
        return Math.max(0, Math.min(4, healthInThisHeart));
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public boolean isFull() {
        return currentHealth >= maxHealth;
    }

    /**
     * Gets health as a percentage (0.0 to 1.0).
     */
    public float getHealthPercent() {
        if (maxHealth == 0) return 0;
        return (float) currentHealth / maxHealth;
    }
}
