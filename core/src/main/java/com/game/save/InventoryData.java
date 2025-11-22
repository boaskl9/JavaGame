package com.game.save;

import java.util.List;
import java.util.Map;

/**
 * Serializable representation of player inventory (default inventory + bags + equipment).
 */
public class InventoryData {
    public List<ItemStackData> defaultSlots; // Main inventory (can contain nulls)
    public List<BagData> bags; // Equipped bags (can contain nulls)
    public Map<String, ItemStackData> equipment; // Equipment by slot name (HEAD, BODY, etc.)

    // Required for JSON deserialization
    public InventoryData() {
    }

    public InventoryData(List<ItemStackData> defaultSlots, List<BagData> bags, Map<String, ItemStackData> equipment) {
        this.defaultSlots = defaultSlots;
        this.bags = bags;
        this.equipment = equipment;
    }
}
