package com.game.systems.loot;

import com.game.systems.entity.Component;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Component attached to enemies that defines their loot table.
 * Contains base drops that can be modified dynamically by player equipment, skills, etc.
 */
public class LootTableComponent implements Component {

    private final List<LootDrop> baseDrops;
    private final Random random;

    public LootTableComponent() {
        this.baseDrops = new ArrayList<>();
        this.random = new Random();
    }

    @Override
    public void update(float delta) {
        // Loot tables don't need per-frame updates
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
     * Adds a base drop to this loot table.
     * Returns this for method chaining.
     */
    public LootTableComponent addDrop(String itemId, float dropChance, int minQty, int maxQty) {
        baseDrops.add(new LootDrop(itemId, dropChance, minQty, maxQty));
        return this;
    }

    /**
     * Adds a base drop with fixed quantity.
     */
    public LootTableComponent addDrop(String itemId, float dropChance, int quantity) {
        return addDrop(itemId, dropChance, quantity, quantity);
    }

    /**
     * Adds a LootDrop directly.
     */
    public LootTableComponent addDrop(LootDrop drop) {
        baseDrops.add(drop);
        return this;
    }

    /**
     * Removes all drops for a specific item ID.
     */
    public void removeDrop(String itemId) {
        baseDrops.removeIf(drop -> drop.getItemId().equals(itemId));
    }

    /**
     * Clears all base drops.
     */
    public void clearDrops() {
        baseDrops.clear();
    }

    /**
     * Gets an immutable copy of the base drops.
     */
    public List<LootDrop> getBaseDrops() {
        return new ArrayList<>(baseDrops);
    }

    /**
     * Rolls for loot drops using the base table and applying modifiers from context.
     *
     * @param context Context containing modifiers, enemy, and player info
     * @return List of ItemStacks to drop
     */
    public List<ItemStack> rollDrops(LootContext context) {
        // Start with a copy of base drops
        List<LootDrop> effectiveDrops = new ArrayList<>(baseDrops);

        // Apply modifiers in priority order
        List<LootModifier> modifiers = new ArrayList<>(context.getActiveModifiers());
        modifiers.sort(Comparator.comparingInt(LootModifier::getPriority).reversed());

        for (LootDrop drop : effectiveDrops) {
            System.out.println("Before: Drop table contains: " + drop.toString());
        }

        for (LootModifier modifier : modifiers) {
            modifier.modify(effectiveDrops, context);
        }

        for (LootDrop drop : effectiveDrops) {
            System.out.println("After: Drop table contains: " + drop.toString());
        }

        // Roll each drop
        List<ItemStack> results = new ArrayList<>();
        for (LootDrop drop : effectiveDrops) {
            if (random.nextFloat() < drop.getDropChance()) {
                // Drop succeeds - determine quantity
                int quantity = drop.getMinQuantity();
                if (drop.getMaxQuantity() > drop.getMinQuantity()) {
                    quantity = drop.getMinQuantity() +
                              random.nextInt(drop.getMaxQuantity() - drop.getMinQuantity() + 1);
                }

                // Create item stack
                ItemStack stack = ItemFactory.create(drop.getItemId(), quantity);
                if (stack != null) {
                    results.add(stack);
                } else {
                    System.err.println("LootTable: Failed to create item '" + drop.getItemId() + "'");
                }
            }
        }

        return results;
    }

    /**
     * Rolls drops without any modifiers (for testing).
     */
    public List<ItemStack> rollDrops() {
        return rollDrops(new LootContext(null, null));
    }

    @Override
    public String toString() {
        return "LootTableComponent{" +
                "baseDrops=" + baseDrops.size() +
                '}';
    }
}
