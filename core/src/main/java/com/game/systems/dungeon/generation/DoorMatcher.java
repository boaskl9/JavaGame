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

        // Calculate offset needed to align doors
        // New room position = existing door world pos - new room door offset
        float newRoomX = existingDoor.getWorldX() - newRoomDoor.getX();
        float newRoomY = existingDoor.getWorldY() - newRoomDoor.getY();

        return new Vector2(newRoomX, newRoomY);
    }

    /**
     * Check if two world doors are close enough to be considered connected.
     * @param door1 First door
     * @param door2 Second door
     * @return true if doors are aligned within tolerance
     */
    public static boolean areDoorsAligned(WorldDoor door1, WorldDoor door2) {
        float dx = door1.getWorldX() - door2.getWorldX();
        float dy = door1.getWorldY() - door2.getWorldY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= DOOR_ALIGNMENT_TOLERANCE;
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
