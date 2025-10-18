package com.game.rendering;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages multi-tile structure grouping for proper Y-sorting.
 *
 * Tiles that belong to the same structure (e.g., a multi-tile building)
 * should all use the same Y coordinate for sorting to ensure the entire
 * structure renders cohesively in front of or behind entities.
 *
 * Usage in Tiled:
 * 1. In the tileset, add a custom property to each tile that's part of a multi-tile object:
 *    - Property name: "groupId"
 *    - Property type: string
 *    - Property value: unique identifier (e.g., "house1", "barn1", etc.)
 *
 * 2. All tiles belonging to the same structure should share the same groupId.
 *
 * 3. The manager will automatically:
 *    - Find all tiles with a groupId property
 *    - Calculate the lowest Y coordinate for each group (the "base Y")
 *    - Store this for runtime lookup during rendering
 *
 * Performance:
 * - Grouping and base Y calculation happens once at map load (O(n) where n = total tiles)
 * - Runtime lookup is O(1) per tile via HashMap
 */
public class TileGroupManager {

    // Maps groupId -> base world Y coordinate for that group
    private final Map<String, Float> groupBaseY;

    // Cache for tile groupId lookups (optional optimization)
    // Key: "layerIndex_x_y", Value: groupId
    private final Map<String, String> tileGroupCache;

    public TileGroupManager() {
        this.groupBaseY = new HashMap<>();
        this.tileGroupCache = new HashMap<>();
    }

    /**
     * Analyze the map and compute base Y coordinates for all tile groups.
     * Call this once when the map is loaded.
     *
     * @param map The TiledMap to analyze
     * @param ySortedLayerIndices The layer indices that will be Y-sorted
     */
    public void buildGroupData(TiledMap map, int[] ySortedLayerIndices) {
        // Clear any existing data
        groupBaseY.clear();
        tileGroupCache.clear();

        // Track minimum Y for each group
        Map<String, Float> groupMinY = new HashMap<>();

        // Iterate through Y-sorted layers only
        for (int layerIndex : ySortedLayerIndices) {
            if (!(map.getLayers().get(layerIndex) instanceof TiledMapTileLayer)) {
                continue;
            }

            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerIndex);
            int tileWidth = (int) layer.getTileWidth();
            int tileHeight = (int) layer.getTileHeight();

            // Check each tile in the layer
            for (int x = 0; x < layer.getWidth(); x++) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                    if (cell == null || cell.getTile() == null) continue;

                    // Check if this tile has a groupId property
                    MapProperties tileProps = cell.getTile().getProperties();
                    String groupId = tileProps.get("groupId", String.class);

                    if (groupId != null && !groupId.isEmpty()) {
                        // Calculate world Y for this tile
                        float worldY = y * tileHeight;
                        float tileBottomY = worldY + tileHeight;

                        // Update minimum Y for this group
                        groupMinY.merge(groupId, tileBottomY, Math::min);

                        // Cache this tile's group assignment
                        String tileKey = layerIndex + "_" + x + "_" + y;
                        tileGroupCache.put(tileKey, groupId);
                    }
                }
            }
        }

        // Store the computed base Y values
        groupBaseY.putAll(groupMinY);

        // Log results for debugging
        if (!groupBaseY.isEmpty()) {
            System.out.println("TileGroupManager: Found " + groupBaseY.size() + " tile groups:");
            for (Map.Entry<String, Float> entry : groupBaseY.entrySet()) {
                System.out.println("  - Group '" + entry.getKey() + "' base Y: " + entry.getValue());
            }
        }
    }

    /**
     * Get the group ID for a specific tile, if it has one.
     *
     * @param layerIndex The layer index
     * @param x Tile X coordinate
     * @param y Tile Y coordinate
     * @return The groupId, or null if this tile is not part of a group
     */
    public String getGroupId(int layerIndex, int x, int y) {
        String tileKey = layerIndex + "_" + x + "_" + y;
        return tileGroupCache.get(tileKey);
    }

    /**
     * Get the base Y coordinate for a group.
     *
     * @param groupId The group identifier
     * @return The base Y coordinate, or null if group doesn't exist
     */
    public Float getGroupBaseY(String groupId) {
        return groupBaseY.get(groupId);
    }

    /**
     * Get the render Y coordinate for a tile.
     * If the tile is part of a group, returns the group's base Y.
     * Otherwise returns the tile's own bottom Y.
     *
     * @param layerIndex The layer index
     * @param x Tile X coordinate
     * @param y Tile Y coordinate
     * @param tileHeight Height of a tile in pixels
     * @return The Y coordinate to use for sorting
     */
    public float getRenderY(int layerIndex, int x, int y, int tileHeight) {
        String groupId = getGroupId(layerIndex, x, y);

        if (groupId != null) {
            Float baseY = getGroupBaseY(groupId);
            if (baseY != null) {
                return baseY;
            }
        }

        // No group - use tile's own Y
        return y * tileHeight + tileHeight;
    }

    /**
     * Check if there are any tile groups configured.
     *
     * @return true if at least one tile group exists
     */
    public boolean hasGroups() {
        return !groupBaseY.isEmpty();
    }
}
