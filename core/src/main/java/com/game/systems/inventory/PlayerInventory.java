package com.game.systems.inventory;

import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the player's complete inventory system.
 * Includes default inventory slots and bag slots.
 * Handles bag equipping/unequipping with proper item redistribution.
 */
public class PlayerInventory {
    private final InventoryContainer defaultInventory;
    private final List<BagInstance> bagSlots;
    private final int maxBagSlots;

    public PlayerInventory() {
        this(InventoryConfig.DEFAULT_INVENTORY_SIZE, InventoryConfig.MAX_BAG_SLOTS);
    }

    public PlayerInventory(int defaultSlots, int maxBagSlots) {
        this.defaultInventory = new InventoryContainer(defaultSlots);
        this.maxBagSlots = maxBagSlots;
        this.bagSlots = new ArrayList<>(maxBagSlots);

        // Initialize bag slots as empty
        for (int i = 0; i < maxBagSlots; i++) {
            bagSlots.add(null);
        }
    }

    /**
     * Attempts to add an item to the inventory.
     * Tries default inventory first, then bags.
     * @param stack The item stack to add
     * @return The remaining items that couldn't be added, or null if all added
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ItemStack remaining = stack.copy();

        // Try default inventory first
        remaining = defaultInventory.addItem(remaining);
        if (remaining == null) {
            return null;
        }

        // Try each equipped bag
        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                remaining = bag.addItem(remaining);
                if (remaining == null) {
                    return null;
                }
            }
        }

        return remaining;
    }

    /**
     * Equips a bag to a bag slot.
     * @param bag The bag to equip
     * @param slotIndex The bag slot index
     * @return true if successful
     */
    public boolean equipBag(BagInstance bag, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= maxBagSlots || bag == null) {
            return false;
        }

        // If slot already has a bag, unequip it first
        if (bagSlots.get(slotIndex) != null) {
            return false; // Require manual unequip first
        }

        bagSlots.set(slotIndex, bag);
        return true;
    }

    /**
     * Unequips a bag from a bag slot.
     * Items in the bag are redistributed to other bags/default inventory.
     * Items that don't fit are returned to be dropped.
     * @param slotIndex The bag slot index
     * @return List of items that need to be dropped (couldn't fit elsewhere)
     */
    public List<ItemStack> unequipBag(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= maxBagSlots) {
            return new ArrayList<>();
        }

        BagInstance bag = bagSlots.get(slotIndex);
        if (bag == null) {
            return new ArrayList<>();
        }

        // Remove the bag from the slot
        bagSlots.set(slotIndex, null);

        // Get all items from the bag
        List<ItemStack> items = bag.clearContents();
        List<ItemStack> itemsToDrop = new ArrayList<>();

        // Try to redistribute items to default inventory and other bags
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                ItemStack remaining = addItem(item);
                if (remaining != null) {
                    itemsToDrop.add(remaining);
                }
            }
        }

        // Try to add the empty bag itself to inventory
        // TODO: This will need ItemStack support for bags (when we make bags as items)
        // For now, just return the bag in itemsToDrop list (will need special handling)

        return itemsToDrop;
    }

    /**
     * Gets a bag from a specific slot.
     * @param slotIndex The bag slot index
     * @return The bag instance, or null if empty
     */
    public BagInstance getBag(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= maxBagSlots) {
            return null;
        }
        return bagSlots.get(slotIndex);
    }

    /**
     * Finds the first empty bag slot.
     * @return The slot index, or -1 if all slots full
     */
    public int getFirstEmptyBagSlot() {
        for (int i = 0; i < maxBagSlots; i++) {
            if (bagSlots.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the total number of inventory slots (default + all bags).
     * @return Total slot count
     */
    public int getTotalSlotCount() {
        int total = defaultInventory.getSize();
        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                total += bag.getSlotCount();
            }
        }
        return total;
    }

    /**
     * Gets the total number of empty slots across all containers.
     * @return Empty slot count
     */
    public int getEmptySlotCount() {
        int count = 0;

        // Count default inventory
        for (int i = 0; i < defaultInventory.getSize(); i++) {
            if (defaultInventory.isSlotEmpty(i)) {
                count++;
            }
        }

        // Count bag slots
        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                for (int i = 0; i < bag.getSlotCount(); i++) {
                    if (bag.getItem(i) == null) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Counts the total quantity of a specific item across all containers.
     * @param itemId The item ID
     * @return Total quantity
     */
    public int countItem(String itemId) {
        int count = defaultInventory.countItem(itemId);

        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                count += bag.getContainer().countItem(itemId);
            }
        }

        return count;
    }

    /**
     * Checks if the inventory has space for an item.
     * @param stack The item stack to check
     * @return true if there's space somewhere
     */
    public boolean hasSpace(ItemStack stack) {
        if (defaultInventory.hasSpace(stack)) {
            return true;
        }

        for (BagInstance bag : bagSlots) {
            if (bag != null && bag.hasSpace(stack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the inventory is completely full.
     * @return true if no space anywhere
     */
    public boolean isFull() {
        return getEmptySlotCount() == 0;
    }

    /**
     * Gets all items across all containers.
     * Useful for debugging or displaying all items.
     * @return List of all item stacks
     */
    public List<ItemStack> getAllItems() {
        List<ItemStack> allItems = new ArrayList<>();

        // Add default inventory items
        allItems.addAll(defaultInventory.getAllItems());

        // Add bag items
        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                allItems.addAll(bag.getAllItems());
            }
        }

        return allItems;
    }

    public InventoryContainer getDefaultInventory() {
        return defaultInventory;
    }

    public List<BagInstance> getBagSlots() {
        return new ArrayList<>(bagSlots);
    }

    public int getMaxBagSlots() {
        return maxBagSlots;
    }

    /**
     * Gets the number of bags currently equipped.
     * @return Equipped bag count
     */
    public int getEquippedBagCount() {
        int count = 0;
        for (BagInstance bag : bagSlots) {
            if (bag != null) {
                count++;
            }
        }
        return count;
    }
}
