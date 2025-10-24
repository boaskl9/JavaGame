package com.game.systems.inventory;

import com.game.systems.item.ItemType;

/**
 * Equipment slot types for player character.
 * Defines which slots are available and what items can be equipped in each.
 */
public enum EquipmentSlot {
    HEAD("Head", ItemType.ARMOR),
    BODY("Body", ItemType.ARMOR),
    AMULET("Amulet", ItemType.ARMOR),
    RING_1("Ring 1", ItemType.ARMOR),
    RING_2("Ring 2", ItemType.ARMOR),
    WEAPON("Weapon", ItemType.WEAPON);

    private final String displayName;
    private final ItemType acceptedType;

    EquipmentSlot(String displayName, ItemType acceptedType) {
        this.displayName = displayName;
        this.acceptedType = acceptedType;
    }

    /**
     * Get the display name for UI labels.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the item type this slot accepts.
     */
    public ItemType getAcceptedType() {
        return acceptedType;
    }

    /**
     * Check if this slot can accept the given item type.
     */
    public boolean acceptsItemType(ItemType type) {
        return this.acceptedType == type;
    }

    /**
     * Get all equipment slots in display order (top to bottom).
     */
    public static EquipmentSlot[] getDisplayOrder() {
        return values(); // Already in correct order
    }
}
