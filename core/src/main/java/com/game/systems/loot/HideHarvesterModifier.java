package com.game.systems.loot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context-aware modifier that adds hide/pelt drops based on enemy type.
 * Intelligently maps enemy tags to appropriate hide items.
 *
 * Example: Weapon with this modifier causes:
 * - Cow (tagged "animal:cow") → drops "cow_hide"
 * - Rabbit (tagged "animal:rabbit") → drops "rabbit_hide"
 * - Wolf (tagged "animal:wolf") → drops "wolf_pelt"
 */
public class HideHarvesterModifier implements LootModifier {

    private final Map<String, String> tagToItemMap;
    private final float dropChance;
    private final int minQuantity;
    private final int maxQuantity;

    /**
     * Creates a hide harvester with default mappings.
     *
     * @param dropChance Chance to drop hide (0.0-1.0)
     * @param minQuantity Minimum hide quantity
     * @param maxQuantity Maximum hide quantity
     */
    public HideHarvesterModifier(float dropChance, int minQuantity, int maxQuantity) {
        this.dropChance = dropChance;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.tagToItemMap = new HashMap<>();

        // Default mappings - can be extended via addMapping()
        addMapping("animal:cow", "cow_hide");
        addMapping("animal:rabbit", "rabbit_hide");
        addMapping("animal:wolf", "wolf_pelt");
        addMapping("animal:bear", "bear_pelt");
        addMapping("animal:deer", "deer_hide");
        addMapping("animal:pig", "pig_hide");
        addMapping("animal:sheep", "wool"); // Sheep drop wool instead
    }

    /**
     * Convenience constructor with fixed quantity.
     */
    public HideHarvesterModifier(float dropChance, int quantity) {
        this(dropChance, quantity, quantity);
    }

    /**
     * Creates a hide harvester with 100% drop chance.
     */
    public HideHarvesterModifier() {
        this(1.0f, 1, 1);
    }

    /**
     * Adds a custom tag-to-item mapping.
     *
     * @param tag The enemy tag to check for (e.g., "animal:chicken")
     * @param itemId The item to drop (e.g., "chicken_feather")
     */
    public void addMapping(String tag, String itemId) {
        tagToItemMap.put(tag, itemId);
    }

    /**
     * Removes a tag mapping.
     */
    public void removeMapping(String tag) {
        tagToItemMap.remove(tag);
    }

    @Override
    public void modify(List<LootDrop> drops, LootContext context) {
        if (context.getEnemy() == null) {
            return; // No enemy context
        }

        // Check each mapping to see if enemy has the tag
        for (Map.Entry<String, String> entry : tagToItemMap.entrySet()) {
            String tag = entry.getKey();
            String itemId = entry.getValue();

            if (context.enemyHasTag(tag)) {
                // Enemy has this tag - add the corresponding drop
                LootDrop hideDrop = new LootDrop(itemId, dropChance, minQuantity, maxQuantity);

                // Check if this item already exists in drops
                boolean exists = drops.stream()
                        .anyMatch(drop -> drop.getItemId().equals(itemId));

                if (!exists) {
                    drops.add(hideDrop);
                } else {
                    // Increase drop chance if already exists
                    for (int i = 0; i < drops.size(); i++) {
                        LootDrop existing = drops.get(i);
                        if (existing.getItemId().equals(itemId)) {
                            float combinedChance = Math.min(1.0f,
                                    existing.getDropChance() + dropChance);
                            drops.set(i, existing.withDropChance(combinedChance));
                            break;
                        }
                    }
                }

                // Only process first matching tag
                break;
            }
        }
    }

    @Override
    public int getPriority() {
        return 100; // High priority to add drops early
    }

    @Override
    public String getDescription() {
        return "Harvests hides from animals (" + (dropChance * 100) + "% chance)";
    }

    /**
     * Gets the item ID that would drop for a specific tag.
     * Returns null if no mapping exists.
     */
    public String getItemForTag(String tag) {
        return tagToItemMap.get(tag);
    }
}
