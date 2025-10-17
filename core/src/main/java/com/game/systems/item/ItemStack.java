package com.game.systems.item;

/**
 * Represents a stack of items (definition + quantity).
 * Mutable class for inventory management.
 */
public class ItemStack {
    private final ItemDefinition definition;
    private int quantity;

    public ItemStack(ItemDefinition definition, int quantity) {
        if (definition == null) {
            throw new IllegalArgumentException("ItemDefinition cannot be null");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.definition = definition;
        this.quantity = Math.min(quantity, definition.getMaxStackSize());
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, Math.min(quantity, definition.getMaxStackSize()));
    }

    /**
     * Adds to the stack quantity, respecting max stack size.
     * @param amount Amount to add
     * @return Amount that couldn't be added (overflow)
     */
    public int add(int amount) {
        int maxCanAdd = definition.getMaxStackSize() - quantity;
        int toAdd = Math.min(amount, maxCanAdd);
        quantity += toAdd;
        return amount - toAdd; // Return overflow
    }

    /**
     * Removes from the stack quantity.
     * @param amount Amount to remove
     * @return Actual amount removed
     */
    public int remove(int amount) {
        int toRemove = Math.min(amount, quantity);
        quantity -= toRemove;
        return toRemove;
    }

    /**
     * Checks if this stack can merge with another.
     * @param other The other stack
     * @return true if they have the same item definition
     */
    public boolean canMergeWith(ItemStack other) {
        return other != null && this.definition.getId().equals(other.definition.getId());
    }

    /**
     * Checks if the stack is full.
     * @return true if at max stack size
     */
    public boolean isFull() {
        return quantity >= definition.getMaxStackSize();
    }

    /**
     * Checks if the stack is empty.
     * @return true if quantity is 0
     */
    public boolean isEmpty() {
        return quantity == 0;
    }

    /**
     * Gets remaining space in this stack.
     * @return Number of items that can be added
     */
    public int getRemainingSpace() {
        return definition.getMaxStackSize() - quantity;
    }

    /**
     * Creates a copy of this stack.
     * @return A new ItemStack with the same definition and quantity
     */
    public ItemStack copy() {
        return new ItemStack(definition, quantity);
    }

    /**
     * Splits this stack into two.
     * @param amount Amount to split off
     * @return A new stack with the split amount, or null if can't split
     */
    public ItemStack split(int amount) {
        if (amount <= 0 || amount >= quantity) {
            return null;
        }
        int splitAmount = Math.min(amount, quantity);
        quantity -= splitAmount;
        return new ItemStack(definition, splitAmount);
    }

    @Override
    public String toString() {
        return definition.getName() + " x" + quantity;
    }
}
