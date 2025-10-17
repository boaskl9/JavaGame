package com.game.systems.item;

import com.game.systems.ui.ItemSlotUI;

/**
 * Helper class for determining item actions based on item type and context.
 * This is where you define what actions are available for each item type.
 */
public class ItemActionHelper {

    /**
     * Gets the primary action for an item.
     * This is the action that will be performed on double-click.
     *
     * @param type The item type
     * @param itemDef The item definition
     * @param slotType The type of slot the item is in
     * @return The primary action string, or null if no primary action
     */
    public static String getPrimaryAction(ItemType type, ItemDefinition itemDef, ItemSlotUI.SlotType slotType) {
        // Check if item is in an equipment slot - if so, primary action is to unequip
        if (slotType == ItemSlotUI.SlotType.BAG_EQUIPMENT) {
            return "Unequip";
        }

        // Otherwise, determine primary action based on item type
        switch (type) {
            case CONSUMABLE:
                return "Consume";

            case WEAPON:
            case ARMOR:
            case TOOL:
                return "Equip";

            case BAG:
                return "Equip";

            case MATERIAL:
            case RESOURCE:
            case QUEST:
            case MISC:
            default:
                return null; // No primary action for these types
        }
    }

    /**
     * Gets all available actions for an item in order (primary action first).
     *
     * @param type The item type
     * @param itemDef The item definition
     * @param slotType The type of slot the item is in
     * @return Array of action strings
     */
    public static String[] getAvailableActions(ItemType type, ItemDefinition itemDef, ItemSlotUI.SlotType slotType) {
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();

        // Add primary action first
        String primaryAction = getPrimaryAction(type, itemDef, slotType);
        if (primaryAction != null) {
            actions.add(primaryAction);
        }

        // Add secondary actions based on type
        addSecondaryActions(actions, type, itemDef, slotType);

        // Always allow dropping as last option
        actions.add("Drop");

        return actions.toArray(new String[0]);
    }

    /**
     * Adds secondary actions to the list based on item type.
     * Override this behavior by adding cases for specific item types.
     *
     * @param actions The list to add actions to
     * @param type The item type
     * @param itemDef The item definition
     * @param slotType The type of slot the item is in
     */
    private static void addSecondaryActions(java.util.ArrayList<String> actions, ItemType type, ItemDefinition itemDef, ItemSlotUI.SlotType slotType) {
        // Note: We can't access the actual ItemStack here, so we check if the item CAN stack
        // The actual quantity check happens in the UI layer

        switch (type) {
            case CONSUMABLE:
                // Consumables are stackable - add split option
                if (canItemStack(itemDef)) {
                    actions.add("Split");
                }
                break;

            case WEAPON:
            case ARMOR:
            case TOOL:
                // Future: Add "Examine", "Repair", etc.
                break;

            case BAG:
                // Future: Add "Open" when not equipped
                break;

            case MATERIAL:
            case RESOURCE:
                // Materials and resources are stackable - add split option
                if (canItemStack(itemDef)) {
                    actions.add("Split");
                }
                break;

            case QUEST:
                // Future: Add "Read" or "Examine"
                break;

            case MISC:
            default:
                // Misc items might be stackable
                if (canItemStack(itemDef)) {
                    actions.add("Split");
                }
                break;
        }
    }

    /**
     * Checks if an item can stack (has max stack size > 1).
     *
     * @param itemDef The item definition
     * @return True if the item can stack
     */
    private static boolean canItemStack(ItemDefinition itemDef) {
        return itemDef.getMaxStackSize() > 1;
    }

    /**
     * Checks if an item has a primary action.
     *
     * @param type The item type
     * @param itemDef The item definition
     * @param slotType The type of slot the item is in
     * @return True if the item has a primary action
     */
    public static boolean hasPrimaryAction(ItemType type, ItemDefinition itemDef, ItemSlotUI.SlotType slotType) {
        return getPrimaryAction(type, itemDef, slotType) != null;
    }
}
