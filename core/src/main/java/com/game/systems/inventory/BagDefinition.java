package com.game.systems.inventory;

/**
 * Definition of a bag type.
 * Describes bag properties like size and item restrictions.
 */
public class BagDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final int slotCount;
    private final ItemFilter filter;
    private final String iconPath;

    public BagDefinition(String id, String name, String description, int slotCount,
                        ItemFilter filter, String iconPath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slotCount = slotCount;
        this.filter = filter != null ? filter : ItemFilter.allowAll();
        this.iconPath = iconPath;
    }

    public BagDefinition(String id, String name, String description, int slotCount) {
        this(id, name, description, slotCount, ItemFilter.allowAll(), null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public ItemFilter getFilter() {
        return filter;
    }

    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String toString() {
        return "BagDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", slots=" + slotCount +
                '}';
    }
}
