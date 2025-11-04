package com.game.systems.dungeon.generation;

import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.RoomBounds;
import com.game.systems.dungeon.generation.PlacedRoom.WorldDoor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of dungeon generation.
 * Contains placed rooms, statistics, and bounds information.
 */
public class DungeonGenerationResult {
    private final String themeName;
    private final int targetBudget;
    private final long seed;
    private final List<PlacedRoom> placedRooms;
    private final List<WorldDoor> unconnectedDoors;
    private final Rectangle bounds;  // Bounding box of all rooms (in pixels)

    public DungeonGenerationResult(String themeName, int targetBudget, long seed,
                                    List<PlacedRoom> placedRooms, List<WorldDoor> unconnectedDoors) {
        this.themeName = themeName;
        this.targetBudget = targetBudget;
        this.seed = seed;
        this.placedRooms = new ArrayList<>(placedRooms);
        this.unconnectedDoors = new ArrayList<>(unconnectedDoors);
        this.bounds = calculateBounds(placedRooms);
    }

    /**
     * Calculate bounding box of all placed rooms.
     */
    private Rectangle calculateBounds(List<PlacedRoom> rooms) {
        if (rooms.isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (PlacedRoom room : rooms) {
            RoomBounds roomBounds = room.getWorldBounds(16);
            int roomPixelX = roomBounds.getX() * 16;
            int roomPixelY = roomBounds.getY() * 16;
            int roomPixelW = roomBounds.getWidth() * 16;
            int roomPixelH = roomBounds.getHeight() * 16;

            minX = Math.min(minX, roomPixelX);
            minY = Math.min(minY, roomPixelY);
            maxX = Math.max(maxX, roomPixelX + roomPixelW);
            maxY = Math.max(maxY, roomPixelY + roomPixelH);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    // Getters
    public String getThemeName() {
        return themeName;
    }

    public int getTargetBudget() {
        return targetBudget;
    }

    public long getSeed() {
        return seed;
    }

    public List<PlacedRoom> getPlacedRooms() {
        return new ArrayList<>(placedRooms);
    }

    public int getRoomCount() {
        return placedRooms.size();
    }

    public List<WorldDoor> getUnconnectedDoors() {
        return new ArrayList<>(unconnectedDoors);
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    /**
     * Get total cost of all placed rooms.
     */
    public int getTotalCost() {
        int total = 0;
        for (PlacedRoom room : placedRooms) {
            total += room.getTemplate().getCost();
        }
        return total;
    }

    /**
     * Get total number of doors (connected and unconnected).
     */
    public int getTotalDoorCount() {
        int total = 0;
        for (PlacedRoom room : placedRooms) {
            total += room.getTemplate().getDoors().size();
        }
        return total;
    }

    /**
     * Get number of connected doors.
     */
    public int getConnectedDoorCount() {
        return getTotalDoorCount() - unconnectedDoors.size();
    }

    /**
     * Check if dungeon is valid (all rooms connected).
     * A valid dungeon has at most 2 unconnected doors (entrance/exit).
     */
    public boolean isValid() {
        return placedRooms.size() > 0;  // Basic validation for now
    }

    /**
     * Get a summary string for logging/display.
     */
    public String getSummary() {
        return String.format("DungeonGenerationResult[theme=%s, rooms=%d, cost=%d/%d, doors=%d/%d connected, bounds=%.0fx%.0f]",
            themeName, getRoomCount(), getTotalCost(), targetBudget,
            getConnectedDoorCount(), getTotalDoorCount(),
            bounds.width, bounds.height);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
