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

    /**
     * Collect all objects from placed rooms, offset to world coordinates.
     * @param placedRooms List of placed rooms
     * @return List of objects in world coordinates, ready for spawning
     */
    public static List<LevelData.LevelObject> collectObjects(List<PlacedRoom> placedRooms) {
        System.out.println("ObjectSpawner: Collecting objects from " + placedRooms.size() + " rooms");

        List<LevelData.LevelObject> worldObjects = new ArrayList<>();

        for (PlacedRoom room : placedRooms) {
            List<LevelData.LevelObject> roomObjects = collectRoomObjects(room);
            worldObjects.addAll(roomObjects);
        }

        System.out.println("ObjectSpawner: Total objects = " + worldObjects.size());
        return worldObjects;
    }

    /**
     * Collect objects from a single room, offset to world coordinates.
     */
    private static List<LevelData.LevelObject> collectRoomObjects(PlacedRoom room) {
        List<LevelData.LevelObject> worldObjects = new ArrayList<>();
        RoomTemplate template = room.getTemplate();

        // Get room's world position
        float worldX = room.getWorldX();
        float worldY = room.getWorldY();

        // Offset each object to world coordinates
        for (LevelData.LevelObject roomObj : template.getObjects()) {
            // Create new object with world coordinates (type, name, x, y)
            LevelData.LevelObject worldObj = new LevelData.LevelObject(
                roomObj.getType(),
                roomObj.getName(),
                roomObj.getX() + worldX,
                roomObj.getY() + worldY
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
