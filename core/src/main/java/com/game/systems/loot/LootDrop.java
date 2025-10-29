package com.game.systems.loot;

/**
 * Represents a single potential item drop.
 * Immutable data class defining what can drop, with what chance, and in what quantity.
 */
public class LootDrop {
    private final String itemId;
    private final float dropChance;  // 0.0 to 1.0 (0% to 100%)
    private final int minQuantity;
    private final int maxQuantity;

    /**
     * Creates a loot drop entry.
     *
     * @param itemId Item ID from ItemRegistry
     * @param dropChance Chance to drop (0.0 = never, 1.0 = always)
     * @param minQuantity Minimum quantity if dropped
     * @param maxQuantity Maximum quantity if dropped
     */
    public LootDrop(String itemId, float dropChance, int minQuantity, int maxQuantity) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be null or empty");
        }
        if (dropChance < 0.0f || dropChance > 1.0f) {
            throw new IllegalArgumentException("dropChance must be between 0.0 and 1.0");
        }
        if (minQuantity < 1) {
            throw new IllegalArgumentException("minQuantity must be at least 1");
        }
        if (maxQuantity < minQuantity) {
            throw new IllegalArgumentException("maxQuantity must be >= minQuantity");
        }

        this.itemId = itemId;
        this.dropChance = dropChance;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }

    /**
     * Convenience constructor for fixed quantity drops.
     */
    public LootDrop(String itemId, float dropChance, int quantity) {
        this(itemId, dropChance, quantity, quantity);
    }

    public String getItemId() {
        return itemId;
    }

    public float getDropChance() {
        return dropChance;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    /**
     * Creates a copy of this drop with modified drop chance.
     * Useful for applying chance multipliers.
     */
    public LootDrop withDropChance(float newChance) {
        return new LootDrop(itemId, newChance, minQuantity, maxQuantity);
    }

    /**
     * Creates a copy of this drop with modified quantities.
     * Useful for applying quantity multipliers.
     */
    public LootDrop withQuantity(int newMin, int newMax) {
        return new LootDrop(itemId, dropChance, newMin, newMax);
    }

    @Override
    public String toString() {
        return "LootDrop{" +
                "itemId='" + itemId + '\'' +
                ", dropChance=" + (dropChance * 100) + "%" +
                ", quantity=" + (minQuantity == maxQuantity ? minQuantity : minQuantity + "-" + maxQuantity) +
                '}';
    }
}
