package com.game.systems.furniture;

import com.game.save.ItemStackData;
import com.game.systems.inventory.InventoryContainer;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates furniture entities from item IDs, and converts chest contents to and from plain data.
 * The single place that knows which furniture item becomes which entity.
 */
public final class FurnitureFactory {

    private FurnitureFactory() {
    }

    /**
     * @return the furniture entity for this item, or null if the item isn't placeable furniture
     */
    public static FurnitureEntity create(String itemId, float x, float y) {
        ItemDefinition def = ItemRegistry.get(itemId);
        if (def == null || !def.isFurniture()) {
            System.err.println("FurnitureFactory: Not a furniture item: " + itemId);
            return null;
        }
        if (def.isBag()) {
            // Bag-type furniture items are chests
            return new ChestEntity(itemId, def, x, y);
        }
        System.err.println("FurnitureFactory: Unknown furniture type: " + itemId);
        return null;
    }

    public static boolean isPlaceable(String itemId) {
        ItemDefinition def = ItemRegistry.get(itemId);
        return def != null && def.isFurniture() && def.isBag();
    }

    /** Chest contents as save data (one entry per slot, null for empty slots). */
    public static List<ItemStackData> exportContents(InventoryContainer container) {
        List<ItemStackData> contents = new ArrayList<>();
        for (int i = 0; i < container.getSize(); i++) {
            ItemStack stack = container.getItem(i);
            contents.add(stack != null && !stack.isEmpty()
                ? new ItemStackData(stack.getDefinition().getId(), stack.getQuantity())
                : null);
        }
        return contents;
    }

    /** Replace a chest's contents from save data. */
    public static void importContents(InventoryContainer container, List<ItemStackData> contents) {
        for (int i = 0; i < container.getSize(); i++) {
            ItemStackData data = contents != null && i < contents.size() ? contents.get(i) : null;
            ItemDefinition def = data != null ? ItemRegistry.get(data.itemId) : null;
            container.setItem(i, def != null ? new ItemStack(def, data.quantity) : null);
        }
    }

    /** Chest contents as two parallel arrays (for network packets; null ID = empty slot). */
    public static String[] contentIds(InventoryContainer container) {
        String[] ids = new String[container.getSize()];
        for (int i = 0; i < ids.length; i++) {
            ItemStack stack = container.getItem(i);
            ids[i] = stack != null && !stack.isEmpty() ? stack.getDefinition().getId() : null;
        }
        return ids;
    }

    public static int[] contentQuantities(InventoryContainer container) {
        int[] quantities = new int[container.getSize()];
        for (int i = 0; i < quantities.length; i++) {
            ItemStack stack = container.getItem(i);
            quantities[i] = stack != null ? stack.getQuantity() : 0;
        }
        return quantities;
    }

    public static void setContents(InventoryContainer container, String[] ids, int[] quantities) {
        for (int i = 0; i < container.getSize(); i++) {
            String id = ids != null && i < ids.length ? ids[i] : null;
            ItemDefinition def = id != null ? ItemRegistry.get(id) : null;
            container.setItem(i, def != null && quantities[i] > 0 ? new ItemStack(def, quantities[i]) : null);
        }
    }

    /** A cheap fingerprint of a chest's contents, to notice when it changed. */
    public static String signature(InventoryContainer container) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < container.getSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack != null && !stack.isEmpty()) {
                sb.append(i).append('=').append(stack.getDefinition().getId()).append('x').append(stack.getQuantity()).append(';');
            }
        }
        return sb.toString();
    }
}
