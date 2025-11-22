package com.game.systems.furniture;

import com.game.systems.entity.Entity;
import com.game.systems.inventory.InventoryContainer;
import com.game.systems.inventory.ItemFilter;
import com.game.systems.item.ItemDefinition;

/**
 * A chest furniture entity that contains an inventory.
 * Can be placed in the world and interacted with to access storage.
 */
public class ChestEntity extends FurnitureEntity {
    private final InventoryContainer container;
    private boolean isOpen = false;

    /**
     * Create a new chest entity.
     * @param itemId The item ID this chest was created from
     * @param definition The item definition (must have bagSize set)
     * @param x World X position
     * @param y World Y position
     */
    public ChestEntity(String itemId, ItemDefinition definition, float x, float y) {
        super(itemId, definition, x, y);

        // Get chest capacity from item definition's bagSize
        Integer bagSize = definition.getBagSize();
        if (bagSize == null || bagSize <= 0) {
            throw new IllegalArgumentException("Chest item must have bagSize defined: " + itemId);
        }

        // Create inventory container
        this.container = new InventoryContainer(bagSize, ItemFilter.allowAll());
    }

    @Override
    public void onInteract(Entity entity) {
        // TODO: Open chest UI in future phase
        // For now, just toggle open state for testing
        isOpen = !isOpen;
        System.out.println("Chest " + (isOpen ? "opened" : "closed") + " - " + container.getSize() + " slots");
    }

    @Override
    public boolean canPickup() {
        // Can only pick up if chest is empty
        return container.isEmpty();
    }

    /**
     * Get the chest's inventory container.
     * @return The inventory container
     */
    public InventoryContainer getContainer() {
        return container;
    }

    /**
     * Check if the chest is currently open in UI.
     * @return true if chest is open
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Set whether the chest is open in UI.
     * @param open true to mark as open
     */
    public void setOpen(boolean open) {
        this.isOpen = open;
    }
}
