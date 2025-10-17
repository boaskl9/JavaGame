package com.game.systems.item;

/**
 * Immutable definition of an item type.
 * This is pure data with no game-specific logic, making it fully reusable.
 */
public class ItemDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final ItemType type;
    private final int maxStackSize;
    private final String iconPath;
    private final boolean consumable;

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.maxStackSize = maxStackSize;
        this.iconPath = iconPath;
        this.consumable = consumable;
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

    public ItemType getType() {
        return type;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public String getIconPath() {
        return iconPath;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public boolean isStackable() {
        return maxStackSize > 1;
    }

    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", maxStackSize=" + maxStackSize +
                '}';
    }
}
