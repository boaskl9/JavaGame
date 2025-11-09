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

    private static final int TILE_SIZE = 16;

    /**
     * Merge collision shapes with offset (to match tile positioning).
     * @param placedRooms List of placed rooms
     * @param offsetX Tile offset X (in tiles, same as TileLayerMerger)
     * @param offsetY Tile offset Y (in tiles, same as TileLayerMerger)
     * @return List of collision rectangles in final map coordinates
     */
    public static List<Rectangle> mergeCollisionWithOffset(List<PlacedRoom> placedRooms, int offsetX, int offsetY) {
        System.out.println("CollisionMerger: Merging collision for " + placedRooms.size() + " rooms");
        System.out.println("CollisionMerger: Applying offset (" + offsetX + ", " + offsetY + ") tiles = (" + (offsetX * TILE_SIZE) + ", " + (offsetY * TILE_SIZE) + ") pixels");

        List<Rectangle> worldCollision = new ArrayList<>();
        float pixelOffsetX = offsetX * TILE_SIZE;
        float pixelOffsetY = offsetY * TILE_SIZE;

        for (PlacedRoom room : placedRooms) {
            System.out.println("CollisionMerger:   Room " + room.getId() + " world position: (" + room.getWorldX() + ", " + room.getWorldY() + ")");
            List<Rectangle> roomCollision = mergeRoomCollision(room, pixelOffsetX, pixelOffsetY);
            worldCollision.addAll(roomCollision);
        }

        System.out.println("CollisionMerger: Total collision shapes = " + worldCollision.size());
        return worldCollision;
    }

    /**
     * Legacy method without offset (deprecated).
     * @deprecated Use mergeCollisionWithOffset() instead
     */
    @Deprecated
    public static List<Rectangle> mergeCollision(List<PlacedRoom> placedRooms) {
        return mergeCollisionWithOffset(placedRooms, 0, 0);
    }

    /**
     * Merge collision shapes from a single room.
     * Offsets shapes from room-relative to final map coordinates.
     */
    private static List<Rectangle> mergeRoomCollision(PlacedRoom room, float pixelOffsetX, float pixelOffsetY) {
        List<Rectangle> worldShapes = new ArrayList<>();
        RoomTemplate template = room.getTemplate();

        // Get room's world position
        float worldX = room.getWorldX();
        float worldY = room.getWorldY();

        // Offset each collision shape to final map coordinates
        // (same offset as tiles to keep everything aligned)
        for (Rectangle roomShape : template.getCollisionShapes()) {
            Rectangle worldShape = new Rectangle(
                roomShape.x + worldX - pixelOffsetX,
                roomShape.y + worldY - pixelOffsetY,
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
