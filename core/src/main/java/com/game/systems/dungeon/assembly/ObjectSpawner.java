package com.game.systems.dungeon.assembly;

import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.dungeon.generation.PlacedRoom;
import com.game.systems.level.LevelData;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes object/entity spawn data from room templates.
 * Offsets objects to world coordinates for spawning.
 */
public class ObjectSpawner {

    private static final int TILE_SIZE = 16;

    /**
     * Collect all objects from placed rooms with offset (to match tile positioning).
     * @param placedRooms List of placed rooms
     * @param offsetX Tile offset X (in tiles, same as TileLayerMerger)
     * @param offsetY Tile offset Y (in tiles, same as TileLayerMerger)
     * @return List of objects in final map coordinates, ready for spawning
     */
    public static List<LevelData.LevelObject> collectObjectsWithOffset(List<PlacedRoom> placedRooms, int offsetX, int offsetY) {
        System.out.println("ObjectSpawner: Collecting objects from " + placedRooms.size() + " rooms");
        System.out.println("ObjectSpawner: Applying offset (" + offsetX + ", " + offsetY + ") tiles = (" + (offsetX * TILE_SIZE) + ", " + (offsetY * TILE_SIZE) + ") pixels");

        List<LevelData.LevelObject> worldObjects = new ArrayList<>();
        float pixelOffsetX = offsetX * TILE_SIZE;
        float pixelOffsetY = offsetY * TILE_SIZE;

        for (PlacedRoom room : placedRooms) {
            List<LevelData.LevelObject> roomObjects = collectRoomObjects(room, pixelOffsetX, pixelOffsetY);
            worldObjects.addAll(roomObjects);
        }

        System.out.println("ObjectSpawner: Total objects = " + worldObjects.size());
        return worldObjects;
    }

    /**
     * Legacy method without offset (deprecated).
     * @deprecated Use collectObjectsWithOffset() instead
     */
    @Deprecated
    public static List<LevelData.LevelObject> collectObjects(List<PlacedRoom> placedRooms) {
        return collectObjectsWithOffset(placedRooms, 0, 0);
    }

    /**
     * Collect objects from a single room, offset to final map coordinates.
     */
    private static List<LevelData.LevelObject> collectRoomObjects(PlacedRoom room, float pixelOffsetX, float pixelOffsetY) {
        List<LevelData.LevelObject> worldObjects = new ArrayList<>();
        RoomTemplate template = room.getTemplate();

        // Get room's world position
        float worldX = room.getWorldX();
        float worldY = room.getWorldY();

        // Offset each object to final map coordinates
        // (same offset as tiles and collision to keep everything aligned)
        for (LevelData.LevelObject roomObj : template.getObjects()) {
            // Create new object with final map coordinates (type, name, x, y)
            LevelData.LevelObject worldObj = new LevelData.LevelObject(
                roomObj.getType(),
                roomObj.getName(),
                roomObj.getX() + worldX - pixelOffsetX,
                roomObj.getY() + worldY - pixelOffsetY
            );

            // Copy all custom properties
            for (String propertyKey : roomObj.getPropertyKeys()) {
                worldObj.setProperty(propertyKey, roomObj.getProperty(propertyKey));
            }

            worldObjects.add(worldObj);
        }

        if (!worldObjects.isEmpty()) {
            System.out.println("ObjectSpawner:   Room " + room.getId() + ": added " + worldObjects.size() + " objects");
        }

        return worldObjects;
    }

    /**
     * Filter objects by type.
     * Useful for finding specific object categories (spawns, gateways, etc.)
     */
    public static List<LevelData.LevelObject> filterObjectsByType(List<LevelData.LevelObject> objects, String type) {
        List<LevelData.LevelObject> filtered = new ArrayList<>();
        for (LevelData.LevelObject obj : objects) {
            if (type.equals(obj.getType())) {
                filtered.add(obj);
            }
        }
        return filtered;
    }

    /**
     * Find spawn points in the object list.
     */
    public static List<LevelData.LevelObject> findSpawnPoints(List<LevelData.LevelObject> objects) {
        List<LevelData.LevelObject> spawns = new ArrayList<>();
        for (LevelData.LevelObject obj : objects) {
            if (obj.getName() != null && obj.getName().contains("spawn")) {
                spawns.add(obj);
            }
        }
        return spawns;
    }
}
