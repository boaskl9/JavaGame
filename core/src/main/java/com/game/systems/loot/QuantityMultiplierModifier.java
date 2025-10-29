package com.game.systems.loot;

import java.util.List;

/**
 * Modifier that multiplies drop quantities for all or specific items.
 * Example: "Greedy" perk gives x2 coin drops.
 */
public class QuantityMultiplierModifier implements LootModifier {

    private final float multiplier;
    private final String targetItemId; // null = applies to all items

    /**
     * Creates a quantity multiplier that applies to all drops.
     *
     * @param multiplier Multiplier to apply (e.g., 1.5 = +50% quantity, 2.0 = double quantity)
     */
    public QuantityMultiplierModifier(float multiplier) {
        this(multiplier, null);
    }

    /**
     * Creates a quantity multiplier for a specific item.
     *
     * @param multiplier Multiplier to apply
     * @param targetItemId Only apply to this item (null = all items)
     */
    public QuantityMultiplierModifier(float multiplier, String targetItemId) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }
        this.multiplier = multiplier;
        this.targetItemId = targetItemId;
    }

    @Override
    public void modify(List<LootDrop> drops, LootContext context) {
        for (int i = 0; i < drops.size(); i++) {
            LootDrop drop = drops.get(i);

            // Check if this modifier applies to this item
            if (targetItemId == null || targetItemId.equals(drop.getItemId())) {
                // Apply multiplier to quantities
                int newMin = Math.max(1, (int) (drop.getMinQuantity() * multiplier));
                int newMax = Math.max(newMin, (int) (drop.getMaxQuantity() * multiplier));
                drops.set(i, drop.withQuantity(newMin, newMax));
            }
        }
    }

    @Override
    public int getPriority() {
        return 0; // Default priority - after chances
    }

    @Override
    public String getDescription() {
        if (targetItemId != null) {
            return "x" + multiplier + " quantity for " + targetItemId;
        }
        return "x" + multiplier + " quantity for all items";
    }
}
