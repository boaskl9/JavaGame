package com.game.systems.inventory;

import com.game.systems.item.ItemStack;

import java.util.List;

/**
 * An instance of a bag with its contents.
 * Bags ARE items themselves, so they can be picked up and placed in inventory.
 * Bags can contain other bags, but only if those bags are empty.
 */
public class BagInstance {
    private final BagDefinition definition;
    private final InventoryContainer container;

    public BagInstance(BagDefinition definition) {
        this.definition = definition;
        this.container = new InventoryContainer(definition.getSlotCount(), definition.getFilter());
    }

    /**
     * Attempts to add an item to this bag.
     * Special handling: if item is a bag, it must be empty.
     * @param stack The item stack to add
     * @return The remaining items that couldn't be added
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        // TODO: When we create bag items, check if item is a bag and if so, verify it's empty
        // For now, just use container's addItem
        return container.addItem(stack);
    }

    /**
     * Checks if a bag can be placed in this bag.
     * Bags can only contain other bags if they are empty.
     * @param otherBag The bag to check
     * @return true if the bag can be placed
     */
    public boolean canAcceptBag(BagInstance otherBag) {
        if (otherBag == null) {
            return false;
        }
        // Can only accept empty bags
        return otherBag.isEmpty();
    }

    /**
     * Gets all items from this bag (for unequipping/dropping).
     * @return List of all items in the bag
     */
    public List<ItemStack> getAllItems() {
        return container.getAllItems();
    }

    /**
     * Clears all items from the bag.
     * @return List of all items that were in the bag
     */
    public List<ItemStack> clearContents() {
        return container.clear();
    }

    /**
     * Checks if the bag is empty.
     * @return true if no items in bag
     */
    public boolean isEmpty() {
        return container.isEmpty();
    }

    public ItemStack removeItem(int slotIndex) {
        return container.removeItem(slotIndex);
    }

    public ItemStack removeItem(int slotIndex, int quantity) {
        return container.removeItem(slotIndex, quantity);
    }

    public ItemStack getItem(int slotIndex) {
        return container.getItem(slotIndex);
    }

    public boolean setItem(int slotIndex, ItemStack stack) {
        return container.setItem(slotIndex, stack);
    }

    public boolean hasSpace(ItemStack stack) {
        return container.hasSpace(stack);
    }

    public int getSlotCount() {
        return container.getSize();
    }

    public BagDefinition getDefinition() {
        return definition;
    }

    public InventoryContainer getContainer() {
        return container;
    }

    @Override
    public String toString() {
        return definition.getName() + " (" +
               (container.getSize() - getAllItems().stream().filter(s -> s != null).count()) +
               " empty slots)";
    }
}
