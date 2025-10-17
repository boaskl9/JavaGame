package com.game.systems.inventory;

import com.game.systems.item.ItemStack;
import com.game.systems.item.ItemDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Base container for holding items in slots.
 * Used for both default inventory and bags.
 */
public class InventoryContainer {
    private final List<ItemStack> slots;
    private final int size;
    private final ItemFilter filter;

    public InventoryContainer(int size, ItemFilter filter) {
        this.size = size;
        this.filter = filter != null ? filter : ItemFilter.allowAll();
        this.slots = new ArrayList<>(size);

        // Initialize empty slots
        for (int i = 0; i < size; i++) {
            slots.add(null);
        }
    }

    public InventoryContainer(int size) {
        this(size, ItemFilter.allowAll());
    }

    /**
     * Attempts to add an item stack to the container.
     * Will try to merge with existing stacks first, then use empty slots.
     * @param stack The item stack to add
     * @return The remaining items that couldn't be added, or null if all added
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        // Check filter
        if (!filter.allows(stack.getDefinition())) {
            return stack; // Return unchanged if not allowed
        }

        ItemStack remaining = stack.copy();

        // First pass: try to merge with existing stacks
        for (int i = 0; i < size; i++) {
            ItemStack existing = slots.get(i);
            if (existing != null && existing.canMergeWith(remaining) && !existing.isFull()) {
                int overflow = existing.add(remaining.getQuantity());
                remaining.setQuantity(overflow);

                if (remaining.isEmpty()) {
                    return null; // All items added
                }
            }
        }

        // Second pass: fill empty slots
        for (int i = 0; i < size; i++) {
            if (slots.get(i) == null) {
                int toAdd = Math.min(remaining.getQuantity(), remaining.getDefinition().getMaxStackSize());
                slots.set(i, new ItemStack(remaining.getDefinition(), toAdd));
                remaining.remove(toAdd);

                if (remaining.isEmpty()) {
                    return null; // All items added
                }
            }
        }

        // Return whatever couldn't be added
        return remaining.isEmpty() ? null : remaining;
    }

    /**
     * Removes an item from a specific slot.
     * @param slotIndex The slot index
     * @return The removed item stack, or null if slot was empty
     */
    public ItemStack removeItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= size) {
            return null;
        }
        ItemStack removed = slots.get(slotIndex);
        slots.set(slotIndex, null);
        return removed;
    }

    /**
     * Removes a specific quantity from a slot.
     * @param slotIndex The slot index
     * @param quantity The quantity to remove
     * @return The removed item stack, or null if failed
     */
    public ItemStack removeItem(int slotIndex, int quantity) {
        if (slotIndex < 0 || slotIndex >= size) {
            return null;
        }

        ItemStack stack = slots.get(slotIndex);
        if (stack == null) {
            return null;
        }

        if (quantity >= stack.getQuantity()) {
            // Remove entire stack
            return removeItem(slotIndex);
        } else {
            // Split stack
            ItemStack removed = stack.split(quantity);
            return removed;
        }
    }

    /**
     * Gets the item stack at a specific slot.
     * @param slotIndex The slot index
     * @return The item stack, or null if empty
     */
    public ItemStack getItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= size) {
            return null;
        }
        return slots.get(slotIndex);
    }

    /**
     * Sets an item in a specific slot.
     * @param slotIndex The slot index
     * @param stack The item stack to set (can be null to clear)
     * @return true if successful
     */
    public boolean setItem(int slotIndex, ItemStack stack) {
        if (slotIndex < 0 || slotIndex >= size) {
            return false;
        }

        // Check filter
        if (stack != null && !filter.allows(stack.getDefinition())) {
            return false;
        }

        slots.set(slotIndex, stack);
        return true;
    }

    /**
     * Checks if a slot is empty.
     * @param slotIndex The slot index
     * @return true if the slot is empty
     */
    public boolean isSlotEmpty(int slotIndex) {
        return getItem(slotIndex) == null;
    }

    /**
     * Gets the first empty slot index.
     * @return The slot index, or -1 if no empty slots
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < size; i++) {
            if (slots.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if the container has space for an item.
     * @param stack The item stack to check
     * @return true if there's space (either empty slot or stackable)
     */
    public boolean hasSpace(ItemStack stack) {
        if (stack == null) {
            return true;
        }

        // Check filter first
        if (!filter.allows(stack.getDefinition())) {
            return false;
        }

        // Check for stackable space
        for (ItemStack existing : slots) {
            if (existing != null && existing.canMergeWith(stack) && !existing.isFull()) {
                return true;
            }
        }

        // Check for empty slot
        return getFirstEmptySlot() != -1;
    }

    /**
     * Checks if the container is completely empty.
     * @return true if all slots are empty
     */
    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the container is completely full.
     * @return true if all slots are occupied
     */
    public boolean isFull() {
        for (ItemStack stack : slots) {
            if (stack == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts the total quantity of a specific item type.
     * @param itemId The item ID to count
     * @return The total quantity
     */
    public int countItem(String itemId) {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getDefinition().getId().equals(itemId)) {
                count += stack.getQuantity();
            }
        }
        return count;
    }

    /**
     * Gets all items in the container.
     * @return List of all item stacks (includes nulls for empty slots)
     */
    public List<ItemStack> getAllItems() {
        return new ArrayList<>(slots);
    }

    /**
     * Clears all items from the container.
     * @return List of all items that were in the container
     */
    public List<ItemStack> clear() {
        List<ItemStack> removed = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = slots.get(i);
            if (stack != null) {
                removed.add(stack);
                slots.set(i, null);
            }
        }
        return removed;
    }

    public int getSize() {
        return size;
    }

    public ItemFilter getFilter() {
        return filter;
    }
}
