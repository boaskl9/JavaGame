package com.game.systems.item;

import com.game.systems.combat.WeaponStats;
import com.game.systems.inventory.EquipmentSlot;

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
    private final Integer bagSize; // Number of slots if this is a bag item, null otherwise
    private final EquipmentSlot equipmentSlot; // Which equipment slot this item goes in (null if not equippable)
    private final WeaponStats weaponStats; // Combat stats for weapons (null if not a weapon)
    private final String weaponSpritePath; // Path to SpriteInHand.png for weapon rendering (null if not a weapon)

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable) {
        this(id, name, description, type, maxStackSize, iconPath, consumable, null, null, null, null);
    }

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable, Integer bagSize) {
        this(id, name, description, type, maxStackSize, iconPath, consumable, bagSize, null, null, null);
    }

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable,
                         Integer bagSize, EquipmentSlot equipmentSlot) {
        this(id, name, description, type, maxStackSize, iconPath, consumable, bagSize, equipmentSlot, null, null);
    }

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable,
                         Integer bagSize, EquipmentSlot equipmentSlot, WeaponStats weaponStats) {
        this(id, name, description, type, maxStackSize, iconPath, consumable, bagSize, equipmentSlot, weaponStats, null);
    }

    public ItemDefinition(String id, String name, String description, ItemType type,
                         int maxStackSize, String iconPath, boolean consumable,
                         Integer bagSize, EquipmentSlot equipmentSlot, WeaponStats weaponStats,
                         String weaponSpritePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.maxStackSize = maxStackSize;
        this.iconPath = iconPath;
        this.consumable = consumable;
        this.bagSize = bagSize;
        this.equipmentSlot = equipmentSlot;
        this.weaponStats = weaponStats;
        this.weaponSpritePath = weaponSpritePath;
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

    public Integer getBagSize() {
        return bagSize;
    }

    public boolean isBag() {
        return bagSize != null;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public boolean isEquippable() {
        return equipmentSlot != null;
    }

    public WeaponStats getWeaponStats() {
        return weaponStats;
    }

    public String getWeaponSpritePath() {
        return weaponSpritePath;
    }

    public boolean isWeapon() {
        return weaponStats != null;
    }

    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", maxStackSize=" + maxStackSize +
                ", bagSize=" + bagSize +
                '}';
    }
}
