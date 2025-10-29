package com.game.systems.loot;

import java.util.List;

/**
 * Modifier that multiplies drop chances for all or specific items.
 * Example: "Luck" stat gives x1.5 rare drop chance.
 */
public class ChanceMultiplierModifier implements LootModifier {

    private final float multiplier;
    private final String targetItemId; // null = applies to all items

    /**
     * Creates a chance multiplier that applies to all drops.
     *
     * @param multiplier Multiplier to apply (e.g., 1.5 = +50% chance, 2.0 = double chance)
     */
    public ChanceMultiplierModifier(float multiplier) {
        this(multiplier, null);
    }

    /**
     * Creates a chance multiplier for a specific item.
     *
     * @param multiplier Multiplier to apply
     * @param targetItemId Only apply to this item (null = all items)
     */
    public ChanceMultiplierModifier(float multiplier, String targetItemId) {
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
                // Apply multiplier, capped at 100% (1.0)
                float newChance = Math.min(1.0f, drop.getDropChance() * multiplier);
                drops.set(i, drop.withDropChance(newChance));
            }
        }
    }

    @Override
    public int getPriority() {
        return 50; // Medium priority - after adds, before quantities
    }

    @Override
    public String getDescription() {
        if (targetItemId != null) {
            return "x" + multiplier + " drop chance for " + targetItemId;
        }
        return "x" + multiplier + " drop chance for all items";
    }
}
