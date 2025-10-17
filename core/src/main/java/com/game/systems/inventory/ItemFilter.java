package com.game.systems.inventory;

import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemType;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter for restricting what items can be placed in a container.
 * Used for specialized bags (e.g., herb bags, tool bags).
 */
public class ItemFilter {
    private final Set<ItemType> allowedTypes;
    private final Set<String> allowedItemIds;
    private final boolean whitelist; // true = allow only listed, false = allow all except listed

    private ItemFilter(Set<ItemType> allowedTypes, Set<String> allowedItemIds, boolean whitelist) {
        this.allowedTypes = allowedTypes != null ? allowedTypes : new HashSet<>();
        this.allowedItemIds = allowedItemIds != null ? allowedItemIds : new HashSet<>();
        this.whitelist = whitelist;
    }

    /**
     * Creates a filter that allows all items.
     * @return A permissive filter
     */
    public static ItemFilter allowAll() {
        return new ItemFilter(new HashSet<>(), new HashSet<>(), false);
    }

    /**
     * Creates a filter that allows only specific item types.
     * @param types The allowed types
     * @return A type-restricted filter
     */
    public static ItemFilter allowTypes(ItemType... types) {
        Set<ItemType> typeSet = new HashSet<>();
        for (ItemType type : types) {
            typeSet.add(type);
        }
        return new ItemFilter(typeSet, new HashSet<>(), true);
    }

    /**
     * Creates a filter that allows only specific item IDs.
     * @param itemIds The allowed item IDs
     * @return An ID-restricted filter
     */
    public static ItemFilter allowItems(String... itemIds) {
        Set<String> idSet = new HashSet<>();
        for (String id : itemIds) {
            idSet.add(id);
        }
        return new ItemFilter(new HashSet<>(), idSet, true);
    }

    /**
     * Checks if an item is allowed by this filter.
     * @param item The item definition to check
     * @return true if the item can be placed in this container
     */
    public boolean allows(ItemDefinition item) {
        if (item == null) {
            return false;
        }

        // If no restrictions, allow all
        if (allowedTypes.isEmpty() && allowedItemIds.isEmpty()) {
            return true;
        }

        if (whitelist) {
            // Whitelist mode: must be in allowed list
            return allowedTypes.contains(item.getType()) || allowedItemIds.contains(item.getId());
        } else {
            // Blacklist mode: must NOT be in blocked list
            return !allowedTypes.contains(item.getType()) && !allowedItemIds.contains(item.getId());
        }
    }

    public Set<ItemType> getAllowedTypes() {
        return new HashSet<>(allowedTypes);
    }

    public Set<String> getAllowedItemIds() {
        return new HashSet<>(allowedItemIds);
    }

    public boolean isWhitelist() {
        return whitelist;
    }
}
