package com.game.save;

import java.util.List;
import java.util.Map;

/**
 * Serializable representation of world state.
 */
public class WorldData {
    public String currentLevelId; // e.g., "Maps/prototype.tmx"
    public String levelType; // "tiled_map" or "dungeon" (dungeons not saved)
    public Map<String, List<FurnitureData>> furnitureByLevel; // Level ID -> furniture list
    public Map<String, List<DroppedItemData>> droppedItemsByLevel; // Level ID -> dropped items

    // Required for JSON deserialization
    public WorldData() {
    }

    public WorldData(String currentLevelId, String levelType,
                     Map<String, List<FurnitureData>> furnitureByLevel,
                     Map<String, List<DroppedItemData>> droppedItemsByLevel) {
        this.currentLevelId = currentLevelId;
        this.levelType = levelType;
        this.furnitureByLevel = furnitureByLevel;
        this.droppedItemsByLevel = droppedItemsByLevel;
    }
}
