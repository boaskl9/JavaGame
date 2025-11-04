package com.game.systems.dungeon.assembly;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.dungeon.generation.PlacedRoom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges room tile layers into final dungeon tile layers.
 * Handles offsetting tiles to their world positions.
 */
public class TileLayerMerger {
    private static final int TILE_SIZE = 16;

    /**
     * Merge room tiles into final tile layers.
     * @param placedRooms List of placed rooms
     * @param bounds Bounding box of the dungeon in pixels
     * @return Map of layer name to TiledMapTileLayer
     */
    public static Map<String, TiledMapTileLayer> mergeLayers(List<PlacedRoom> placedRooms, Rectangle bounds) {
        System.out.println("TileLayerMerger: Merging layers for " + placedRooms.size() + " rooms");
        System.out.println("TileLayerMerger: Bounds = " + bounds);

        // Calculate map size in tiles
        int mapWidth = (int) Math.ceil(bounds.width / TILE_SIZE) + 2;  // +2 for padding
        int mapHeight = (int) Math.ceil(bounds.height / TILE_SIZE) + 2;

        System.out.println("TileLayerMerger: Final map size = " + mapWidth + "x" + mapHeight + " tiles");

        // Create layers (common layer names)
        Map<String, TiledMapTileLayer> layers = new HashMap<>();
        String[] layerNames = {"Background", "Features", "Top"};

        for (String layerName : layerNames) {
            TiledMapTileLayer layer = new TiledMapTileLayer(mapWidth, mapHeight, TILE_SIZE, TILE_SIZE);
            layer.setName(layerName);  // Set the layer name!

            // Set rendering properties for YSortRenderer
            if (layerName.equals("Features")) {
                layer.getProperties().put("foregroundRender", true);  // Y-sorted with entities
            } else if (layerName.equals("Top")) {
                layer.getProperties().put("topLayer", true);  // Always on top
            }
            // Background has no special properties (default)

            layers.put(layerName, layer);
        }

        // Calculate offset to make all coordinates positive
        int offsetX = (int) Math.floor(bounds.x / TILE_SIZE) - 1;  // -1 for padding
        int offsetY = (int) Math.floor(bounds.y / TILE_SIZE) - 1;

        System.out.println("TileLayerMerger: Offset = (" + offsetX + ", " + offsetY + ")");

        // Merge each room
        for (PlacedRoom room : placedRooms) {
            mergeRoom(room, layers, offsetX, offsetY);
        }

        System.out.println("TileLayerMerger: Layer merging complete");
        return layers;
    }

    /**
     * Merge a single room's tiles into the final layers.
     */
    private static void mergeRoom(PlacedRoom room, Map<String, TiledMapTileLayer> finalLayers, int offsetX, int offsetY) {
        RoomTemplate template = room.getTemplate();

        // Calculate room position in tiles (world position in pixels / tile size)
        int roomTileX = (int) Math.floor(room.getWorldX() / TILE_SIZE);
        int roomTileY = (int) Math.floor(room.getWorldY() / TILE_SIZE);

        // Apply offset to make coordinates positive in final map
        int finalRoomX = roomTileX - offsetX;
        int finalRoomY = roomTileY - offsetY;

        System.out.println("TileLayerMerger: Merging room " + room.getId() + " at tile position (" + finalRoomX + ", " + finalRoomY + ")");

        // Merge each layer from the room template
        for (RoomTemplate.LayerData layerData : template.getTileLayers()) {
            TiledMapTileLayer finalLayer = finalLayers.get(layerData.name);

            if (finalLayer == null) {
                System.err.println("TileLayerMerger: Warning - layer '" + layerData.name + "' not found in final layers, skipping");
                continue;
            }

            // Copy cells from room to final layer
            int cellsCopied = 0;
            int nullTiles = 0;  // DEBUG
            for (int x = 0; x < layerData.cells.length; x++) {
                for (int y = 0; y < layerData.cells[x].length; y++) {
                    TiledMapTileLayer.Cell sourceCell = layerData.cells[x][y];
                    if (sourceCell != null) {
                        if (sourceCell.getTile() == null) {
                            nullTiles++;  // DEBUG: Count cells with null tiles
                        }
                    }
                    if (sourceCell != null && sourceCell.getTile() != null) {
                        // Calculate final position
                        int finalX = finalRoomX + x;
                        int finalY = finalRoomY + y;

                        // Validate bounds
                        if (finalX >= 0 && finalX < finalLayer.getWidth() &&
                            finalY >= 0 && finalY < finalLayer.getHeight()) {

                            // Create new cell (don't reuse the original)
                            TiledMapTileLayer.Cell newCell = new TiledMapTileLayer.Cell();
                            newCell.setTile(sourceCell.getTile());
                            newCell.setFlipHorizontally(sourceCell.getFlipHorizontally());
                            newCell.setFlipVertically(sourceCell.getFlipVertically());
                            newCell.setRotation(sourceCell.getRotation());

                            finalLayer.setCell(finalX, finalY, newCell);
                            cellsCopied++;
                        }
                    }
                }
            }

            if (cellsCopied > 0) {
                System.out.println("TileLayerMerger:   Layer '" + layerData.name + "': copied " + cellsCopied + " cells");
            }
            if (nullTiles > 0) {
                System.out.println("TileLayerMerger:   WARNING - Found " + nullTiles + " cells with null tiles in layer '" + layerData.name + "'");
            }
        }
    }
}
