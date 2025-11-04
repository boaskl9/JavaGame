package com.game.systems.dungeon.assembly;

import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.dungeon.generation.PlacedRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges collision shapes from room templates into world-space collision shapes.
 * Offsets collision rectangles to their world positions.
 */
public class CollisionMerger {

    /**
     * Merge collision shapes from all placed rooms.
     * @param placedRooms List of placed rooms
     * @return List of collision rectangles in world coordinates
     */
    public static List<Rectangle> mergeCollision(List<PlacedRoom> placedRooms) {
        System.out.println("CollisionMerger: Merging collision for " + placedRooms.size() + " rooms");

        List<Rectangle> worldCollision = new ArrayList<>();

        for (PlacedRoom room : placedRooms) {
            List<Rectangle> roomCollision = mergeRoomCollision(room);
            worldCollision.addAll(roomCollision);
        }

        System.out.println("CollisionMerger: Total collision shapes = " + worldCollision.size());
        return worldCollision;
    }

    /**
     * Merge collision shapes from a single room.
     * Offsets shapes from room-relative to world coordinates.
     */
    private static List<Rectangle> mergeRoomCollision(PlacedRoom room) {
        List<Rectangle> worldShapes = new ArrayList<>();
        RoomTemplate template = room.getTemplate();

        // Get room's world position
        float worldX = room.getWorldX();
        float worldY = room.getWorldY();

        // Offset each collision shape to world coordinates
        for (Rectangle roomShape : template.getCollisionShapes()) {
            Rectangle worldShape = new Rectangle(
                roomShape.x + worldX,
                roomShape.y + worldY,
                roomShape.width,
                roomShape.height
            );
            worldShapes.add(worldShape);
        }

        if (!worldShapes.isEmpty()) {
            System.out.println("CollisionMerger:   Room " + room.getId() + ": added " + worldShapes.size() + " collision shapes");
        }

        return worldShapes;
    }

    /**
     * Get the bounding box of all collision shapes.
     * Useful for debug visualization.
     */
    public static Rectangle getCollisionBounds(List<Rectangle> collisionShapes) {
        if (collisionShapes.isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Rectangle shape : collisionShapes) {
            minX = Math.min(minX, shape.x);
            minY = Math.min(minY, shape.y);
            maxX = Math.max(maxX, shape.x + shape.width);
            maxY = Math.max(maxY, shape.y + shape.height);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
}
