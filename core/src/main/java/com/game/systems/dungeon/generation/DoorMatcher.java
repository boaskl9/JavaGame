package com.game.systems.dungeon.generation;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.dungeon.DoorConnection;
import com.game.systems.dungeon.Direction;
import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.dungeon.generation.PlacedRoom.WorldDoor;

/**
 * Utilities for matching and connecting doors between rooms.
 * Handles door alignment and position calculation.
 */
public class DoorMatcher {
    private static final float DOOR_ALIGNMENT_TOLERANCE = 2.0f;  // Pixels

    /**
     * Calculate the world position where a new room should be placed to align with an existing door.
     * @param existingDoor Door from an already-placed room
     * @param newRoomDoor Door from the room to be placed
     * @return World position for the new room, or null if doors can't connect
     */
    public static Vector2 calculateRoomPositionForDoorAlignment(WorldDoor existingDoor, DoorConnection newRoomDoor) {
        // Check if doors can connect (opposite directions, compatible types)
        if (!existingDoor.getDoor().canConnectWith(newRoomDoor)) {
            return null;
        }

        DoorConnection existingDoorConn = existingDoor.getDoor();
        Direction existingDir = existingDoorConn.getDirection();

        // Calculate offset needed to align doors
        // New room position = existing door world pos - new room door offset
        float newRoomX = existingDoor.getWorldX() - newRoomDoor.getX();
        float newRoomY = existingDoor.getWorldY() - newRoomDoor.getY();

        // Apply directional offset so doors MEET at boundary instead of overlapping
        // For NORTH/SOUTH: offset by door height
        // For EAST/WEST: offset by door width
        switch (existingDir) {
            case NORTH:
                // Existing door faces up, new door faces down
                // New room should be ABOVE existing door
                newRoomY += existingDoorConn.getHeight();
                break;
            case SOUTH:
                // Existing door faces down, new door faces up
                // New room should be BELOW existing door
                newRoomY -= newRoomDoor.getHeight();
                break;
            case EAST:
                // Existing door faces right, new door faces left
                // New room should be to the RIGHT of existing door
                newRoomX += existingDoorConn.getWidth();
                break;
            case WEST:
                // Existing door faces left, new door faces right
                // New room should be to the LEFT of existing door
                newRoomX -= newRoomDoor.getWidth();
                break;
        }

        return new Vector2(newRoomX, newRoomY);
    }

    /**
     * Check if two world doors are close enough to be considered connected.
     * @param door1 First door
     * @param door2 Second door
     * @return true if doors are aligned within tolerance
     */
    public static boolean areDoorsAligned(WorldDoor door1, WorldDoor door2) {

        Direction door1Direction = door1.getDoor().getDirection();
        Direction door2Direction = door2.getDoor().getDirection();

        System.out.println("--------------" + (door1Direction.opposite() == door2Direction) + "-----------");

        return door1Direction.opposite() == door2Direction;
    }

    /**
     * Check if a door from a placed room can connect with a door from a room template.
     * @param placedDoor Door from an already-placed room
     * @param templateDoor Door from a room template (relative coords)
     * @return true if doors can potentially connect
     */
    public static boolean canDoorsConnect(DoorConnection placedDoor, DoorConnection templateDoor) {
        return placedDoor.canConnectWith(templateDoor);
    }

    /**
     * Find all compatible doors between a placed room and a template.
     * @param placedRoom Already-placed room
     * @param template Room template to check
     * @return List of door pairs that could connect
     */
    public static java.util.List<DoorPair> findCompatibleDoors(PlacedRoom placedRoom, RoomTemplate template) {
        java.util.List<DoorPair> pairs = new java.util.ArrayList<>();

        for (WorldDoor worldDoor : placedRoom.getWorldDoors()) {
            // Skip already connected doors
            if (worldDoor.isConnected()) {
                continue;
            }

            for (DoorConnection templateDoor : template.getDoors()) {
                if (canDoorsConnect(worldDoor.getDoor(), templateDoor)) {
                    pairs.add(new DoorPair(worldDoor, templateDoor));
                }
            }
        }

        return pairs;
    }

    /**
     * Represents a potential door connection between a placed room and a template.
     */
    public static class DoorPair {
        public final WorldDoor placedDoor;
        public final DoorConnection templateDoor;

        public DoorPair(WorldDoor placedDoor, DoorConnection templateDoor) {
            this.placedDoor = placedDoor;
            this.templateDoor = templateDoor;
        }

        /**
         * Calculate where the template room should be placed to align this door pair.
         */
        public Vector2 calculatePlacementPosition() {
            return calculateRoomPositionForDoorAlignment(placedDoor, templateDoor);
        }

        @Override
        public String toString() {
            return String.format("DoorPair[placed=%s, template=%s]",
                placedDoor.getDoor().getDirection(),
                templateDoor.getDirection());
        }
    }

    /**
     * Get the direction vector for a door direction.
     * Used for positioning rooms relative to each other.
     */
    public static Vector2 getDirectionVector(Direction direction) {
        switch (direction) {
            case NORTH: return new Vector2(0, 1);
            case SOUTH: return new Vector2(0, -1);
            case EAST: return new Vector2(1, 0);
            case WEST: return new Vector2(-1, 0);
            default: return new Vector2(0, 0);
        }
    }
}
