package com.game.systems.inventory;

import com.game.systems.item.ItemStack;
import com.game.systems.item.ItemType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages player's equipped items.
 * Stores one item per equipment slot (head, body, weapon, etc.).
 */
public class PlayerEquipment {
    private final Map<EquipmentSlot, ItemStack> equipped;

    public PlayerEquipment() {
        this.equipped = new EnumMap<>(EquipmentSlot.class);
    }

    /**
     * Equip an item in the specified slot.
     * Returns the previously equipped item (or null if slot was empty).
     *
     * @param slot The equipment slot
     * @param item The item to equip (can be null to clear slot)
     * @return The previously equipped item, or null
     */
    public ItemStack equipItem(EquipmentSlot slot, ItemStack item) {
        if (item != null && !canEquip(item, slot)) {
            throw new IllegalArgumentException(
                "Cannot equip " + item.getDefinition().getName() +
                " in slot " + slot.getDisplayName()
            );
        }

        ItemStack previous = equipped.put(slot, item);
        return previous;
    }

    /**
     * Unequip an item from the specified slot.
     * Returns the item that was removed (or null if slot was empty).
     *
     * @param slot The equipment slot
     * @return The unequipped item, or null
     */
    public ItemStack unequipItem(EquipmentSlot slot) {
        return equipped.remove(slot);
    }

    /**
     * Get the item currently equipped in a slot.
     *
     * @param slot The equipment slot
     * @return The equipped item, or null if slot is empty
     */
    public ItemStack getEquipped(EquipmentSlot slot) {
        return equipped.get(slot);
    }

    /**
     * Check if a slot has an item equipped.
     *
     * @param slot The equipment slot
     * @return true if the slot has an item
     */
    public boolean isSlotOccupied(EquipmentSlot slot) {
        return equipped.containsKey(slot) && equipped.get(slot) != null;
    }

    /**
     * Check if an item can be equipped in the given slot.
     * Validates that the item type matches the slot's accepted type.
     *
     * @param item The item to check
     * @param slot The target equipment slot
     * @return true if the item can be equipped
     */
    public boolean canEquip(ItemStack item, EquipmentSlot slot) {
        if (item == null) {
            return false;
        }

        ItemType itemType = item.getDefinition().getType();
        return slot.acceptsItemType(itemType);
    }

    /**
     * Find the appropriate slot for an item (for auto-equip).
     * First checks if item has a specific slot assigned, otherwise finds
     * the first compatible empty slot, or returns first compatible slot for swap.
     *
     * @param item The item to find a slot for
     * @return The appropriate slot, or null
     */
    public EquipmentSlot findSlotFor(ItemStack item) {
        if (item == null) {
            return null;
        }

        // If item has a specific equipment slot defined, prefer that slot
        EquipmentSlot preferredSlot = item.getDefinition().getEquipmentSlot();
        if (preferredSlot != null) {
            return preferredSlot;
        }

        // Otherwise, fall back to finding by item type
        ItemType itemType = item.getDefinition().getType();

        // Try to find an empty slot that accepts this item type
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.acceptsItemType(itemType) && !isSlotOccupied(slot)) {
                return slot;
            }
        }

        // If no empty slot, return first compatible slot (will swap)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.acceptsItemType(itemType)) {
                return slot;
            }
        }

        return null;
    }

    /**
     * Clear all equipment slots.
     * Returns all equipped items.
     *
     * @return Array of previously equipped items
     */
    public ItemStack[] clearAll() {
        ItemStack[] items = new ItemStack[EquipmentSlot.values().length];
        int index = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            items[index++] = unequipItem(slot);
        }

        return items;
    }

    /**
     * Get a copy of all equipped items.
     *
     * @return Map of slot → item
     */
    public Map<EquipmentSlot, ItemStack> getAllEquipped() {
        return new EnumMap<>(equipped);
    }

    /**
     * Alias for equipItem() to match standard API naming.
     */
    public ItemStack equip(EquipmentSlot slot, ItemStack item) {
        return equipItem(slot, item);
    }

    /**
     * Get equipment for a slot (alias for getEquipped).
     */
    public ItemStack getEquipment(EquipmentSlot slot) {
        return getEquipped(slot);
    }

    /**
     * Clear all equipment (alias for clearAll but returns void).
     */
    public void clear() {
        equipped.clear();
    }
}
