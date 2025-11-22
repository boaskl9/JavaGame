package com.game.save;

import java.util.List;

/**
 * Serializable representation of a bag equipped in a bag slot.
 */
public class BagData {
    public int slotIndex; // Which bag slot (0, 1, 2)
    public String bagItemId; // The bag item definition ID
    public List<ItemStackData> contents; // Items inside the bag (can contain nulls)

    // Required for JSON deserialization
    public BagData() {
    }

    public BagData(int slotIndex, String bagItemId, List<ItemStackData> contents) {
        this.slotIndex = slotIndex;
        this.bagItemId = bagItemId;
        this.contents = contents;
    }
}
