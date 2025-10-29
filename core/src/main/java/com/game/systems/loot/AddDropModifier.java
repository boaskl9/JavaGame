package com.game.systems.loot;

import java.util.List;

/**
 * Modifier that adds a new drop to the loot table.
 * Example: Coffee ring adds coffee drop to all enemies.
 */
public class AddDropModifier implements LootModifier {

    private final LootDrop dropToAdd;
    private final String description;

    /**
     * Creates a modifier that adds a specific drop.
     *
     * @param itemId Item to drop
     * @param dropChance Chance to drop (0.0-1.0)
     * @param minQuantity Minimum quantity
     * @param maxQuantity Maximum quantity
     */
    public AddDropModifier(String itemId, float dropChance, int minQuantity, int maxQuantity) {
        this.dropToAdd = new LootDrop(itemId, dropChance, minQuantity, maxQuantity);
        this.description = "Adds " + itemId + " drop (" + (dropChance * 100) + "% chance)";
    }

    /**
     * Convenience constructor for fixed quantity.
     */
    public AddDropModifier(String itemId, float dropChance, int quantity) {
        this(itemId, dropChance, quantity, quantity);
    }

    /**
     * Constructor that accepts a pre-configured LootDrop.
     */
    public AddDropModifier(LootDrop drop) {
        this.dropToAdd = drop;
        this.description = "Adds " + drop.getItemId() + " drop";
    }

    @Override
    public void modify(List<LootDrop> drops, LootContext context) {
        // Check if this item is already in the drops
        boolean exists = drops.stream()
                .anyMatch(drop -> drop.getItemId().equals(dropToAdd.getItemId()));

        if (exists) {
            // Item already drops - could combine chances or ignore
            // For now, we'll just increase the existing drop chance
            for (int i = 0; i < drops.size(); i++) {
                LootDrop existing = drops.get(i);
                if (existing.getItemId().equals(dropToAdd.getItemId())) {
                    // Combine chances (additive, capped at 1.0)
                    float combinedChance = Math.min(1.0f,
                            existing.getDropChance() + dropToAdd.getDropChance());
                    drops.set(i, existing.withDropChance(combinedChance));
                    break;
                }
            }
        } else {
            // Add new drop
            drops.add(dropToAdd);
        }
    }

    @Override
    public int getPriority() {
        return 100; // High priority so other modifiers can modify this drop
    }

    @Override
    public String getDescription() {
        return description;
    }
}
