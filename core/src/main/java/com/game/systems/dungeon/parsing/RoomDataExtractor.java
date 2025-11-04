package com.game.systems.dungeon.parsing;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.Direction;
import com.game.systems.dungeon.DoorConnection;
import com.game.systems.dungeon.RoomBounds;
import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.level.LevelData;

import java.util.*;

/**
 * Extracts tile data, doors, collision, and objects from a room region in a theme map.
 */
public class RoomDataExtractor {
    private static final int TILE_SIZE = 16;
    private static final String DOORS_LAYER = "Doors";
    private static final String COLLISION_LAYER = "Collision";
    private static final String ENTITIES_LAYER = "Entities";

    /**
     * Extract complete room data and build a RoomTemplate.
     * @param map The theme map
     * @param bounds The room boundaries
     * @param roomId Unique ID for this room
     * @return Fully populated RoomTemplate
     */
    public static RoomTemplate extractRoom(TiledMap map, RoomBounds bounds, String roomId) {
        String roomName = RoomExtractor.getRoomName(map, bounds);
        RoomTemplate.Builder builder = new RoomTemplate.Builder(roomId, bounds);
        builder.name(roomName);

        // Extract tile layers
        extractTileLayers(map, bounds, builder);

        // Extract doors
        extractDoors(map, bounds, builder);

        // Extract collision shapes
        extractCollision(map, bounds, builder);

        // Extract objects/entities
        extractObjects(map, bounds, builder);

        // Extract metadata from room marker
        extractMetadata(map, bounds, builder);

        return builder.build();
    }

    /**
     * Extract tile data from all tile layers within the room bounds.
     */
    private static void extractTileLayers(TiledMap map, RoomBounds bounds, RoomTemplate.Builder builder) {
        int width = bounds.getWidth();
        int height = bounds.getHeight();

        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                String layerName = layer.getName();

                // Skip marker layers
                if (layerName.equals("RoomMarkers") || layerName.equals(DOORS_LAYER)) {
                    continue;
                }

                // Extract cells from this region
                TiledMapTileLayer.Cell[][] cells = new TiledMapTileLayer.Cell[width][height];
                boolean hasContent = false;

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        TiledMapTileLayer.Cell cell = tileLayer.getCell(bounds.getX() + x, bounds.getY() + y);
                        cells[x][y] = cell;
                        if (cell != null) {
                            hasContent = true;
                        }
                    }
                }

                // Only add layer if it has content
                if (hasContent) {
                    builder.addTileLayer(layerName, cells);
                }
            }
        }
    }

    /**
     * Extract door markers from the Doors layer.
     */
    private static void extractDoors(TiledMap map, RoomBounds bounds, RoomTemplate.Builder builder) {
        MapLayer doorsLayer = map.getLayers().get(DOORS_LAYER);
        if (doorsLayer == null) {
            return;
        }

        int roomX = bounds.getX() * TILE_SIZE;
        int roomY = bounds.getY() * TILE_SIZE;

        for (MapObject obj : doorsLayer.getObjects()) {
            float objX = obj.getProperties().get("x", Float.class);
            float objY = obj.getProperties().get("y", Float.class);

            // Check if door is within room bounds (pixel coordinates)
            if (objX >= roomX && objX < roomX + bounds.getWidth() * TILE_SIZE &&
                objY >= roomY && objY < roomY + bounds.getHeight() * TILE_SIZE) {

                // Convert to room-relative coordinates
                float relativeX = objX - roomX;
                float relativeY = objY - roomY;

                // Get door properties
                String directionStr = obj.getProperties().get("direction", String.class);
                String doorType = obj.getProperties().get("doorType", String.class);

                if (directionStr == null) {
                    directionStr = "NORTH";  // Default
                }
                if (doorType == null) {
                    doorType = "normal";
                }

                Direction direction = Direction.fromString(directionStr);
                if (direction != null) {
                    DoorConnection door = new DoorConnection(relativeX, relativeY, direction, doorType);
                    builder.addDoor(door);
                }
            }
        }
    }

    /**
     * Extract collision shapes from the Collision layer.
     */
    private static void extractCollision(TiledMap map, RoomBounds bounds, RoomTemplate.Builder builder) {
        MapLayer collisionLayer = map.getLayers().get(COLLISION_LAYER);
        if (collisionLayer == null) {
            return;
        }

        int roomX = bounds.getX() * TILE_SIZE;
        int roomY = bounds.getY() * TILE_SIZE;

        for (MapObject obj : collisionLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();

                // Check if collision is within room bounds
                if (rect.x >= roomX && rect.x < roomX + bounds.getWidth() * TILE_SIZE &&
                    rect.y >= roomY && rect.y < roomY + bounds.getHeight() * TILE_SIZE) {

                    // Convert to room-relative coordinates
                    Rectangle relativeRect = new Rectangle(
                        rect.x - roomX,
                        rect.y - roomY,
                        rect.width,
                        rect.height
                    );
                    builder.addCollisionShape(relativeRect);
                }
            }
            // TODO: Support polygon collision shapes if needed
        }
    }

    /**
     * Extract entity/object spawn points from the Entities layer.
     */
    private static void extractObjects(TiledMap map, RoomBounds bounds, RoomTemplate.Builder builder) {
        MapLayer entitiesLayer = map.getLayers().get(ENTITIES_LAYER);
        if (entitiesLayer == null) {
            return;
        }

        int roomX = bounds.getX() * TILE_SIZE;
        int roomY = bounds.getY() * TILE_SIZE;

        for (MapObject obj : entitiesLayer.getObjects()) {
            float objX = obj.getProperties().get("x", Float.class);
            float objY = obj.getProperties().get("y", Float.class);

            // Check if object is within room bounds
            if (objX >= roomX && objX < roomX + bounds.getWidth() * TILE_SIZE &&
                objY >= roomY && objY < roomY + bounds.getHeight() * TILE_SIZE) {

                // Convert to room-relative coordinates
                float relativeX = objX - roomX;
                float relativeY = objY - roomY;

                // Create LevelObject with relative coordinates
                String objectName = obj.getName() != null ? obj.getName() : "unnamed";
                String objectType = obj.getName() != null ? obj.getName() : "unknown";
                LevelData.LevelObject levelObj = new LevelData.LevelObject(objectName, objectType, relativeX, relativeY);

                // Copy custom properties
                Iterator<String> keys = obj.getProperties().getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!key.equals("x") && !key.equals("y")) {
                        levelObj.setProperty(key, obj.getProperties().get(key));
                    }
                }

                builder.addObject(levelObj);
            }
        }
    }

    /**
     * Extract metadata (cost, weight, category, tags) from room marker object.
     */
    private static void extractMetadata(TiledMap map, RoomBounds bounds, RoomTemplate.Builder builder) {
        MapObject marker = RoomExtractor.getRoomMarker(map, bounds);
        if (marker == null) {
            return;
        }

        // Extract cost (default: 10)
        Integer cost = marker.getProperties().get("cost", Integer.class);
        if (cost != null) {
            builder.cost(cost);
        }

        // Extract weight (default: 10)
        Integer weight = marker.getProperties().get("weight", Integer.class);
        if (weight != null) {
            builder.weight(weight);
        }

        // Extract category (default: "medium")
        String category = marker.getProperties().get("category", String.class);
        if (category != null) {
            builder.category(category);
        }

        // Extract tags (comma-separated string)
        String tagsStr = marker.getProperties().get("tags", String.class);
        if (tagsStr != null && !tagsStr.isEmpty()) {
            String[] tagArray = tagsStr.split(",");
            for (String tag : tagArray) {
                builder.addTag(tag.trim());
            }
        }
    }
}
