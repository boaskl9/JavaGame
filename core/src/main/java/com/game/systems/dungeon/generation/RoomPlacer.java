package com.game.systems.dungeon.generation;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.dungeon.RoomTemplate;
import com.game.systems.dungeon.generation.PlacedRoom.WorldDoor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places rooms organically in world space, connecting them via doors.
 * Uses a greedy algorithm: place rooms to connect with existing unconnected doors.
 */
public class RoomPlacer {
    private static final int TILE_SIZE = 16;
    private static final int MAX_PLACEMENT_ATTEMPTS = 50;

    private final List<PlacedRoom> placedRooms;
    private final List<WorldDoor> unconnectedDoors;
    private final Random random;
    private int nextRoomId = 0;

    public RoomPlacer(long seed) {
        this.placedRooms = new ArrayList<>();
        this.unconnectedDoors = new ArrayList<>();
        this.random = new Random(seed);
    }

    /**
     * Place rooms from the allocated list.
     * @param rooms List of room templates to place
     * @return List of successfully placed rooms
     */
    public List<PlacedRoom> placeRooms(List<RoomTemplate> rooms) {
        if (rooms.isEmpty()) {
            System.err.println("RoomPlacer: No rooms to place");
            return placedRooms;
        }

        // Place first room at origin (or offset slightly for variety)
        RoomTemplate firstRoom = rooms.get(0);
        float startX = random.nextFloat() * 32 - 16;  // Small random offset
        float startY = random.nextFloat() * 32 - 16;
        PlacedRoom firstPlaced = new PlacedRoom(firstRoom, startX, startY, nextRoomId++);
        addPlacedRoom(firstPlaced);
        System.out.println("RoomPlacer: Placed first room " + firstPlaced);
        System.out.println("RoomPlacer: First room has " + firstPlaced.getWorldDoors().size() + " doors:");
        for (WorldDoor door : firstPlaced.getWorldDoors()) {
            System.out.println("  - " + door.getDoor().getDirection() + " at (" + door.getWorldX() + "," + door.getWorldY() + ")");
        }

        // Place remaining rooms
        for (int i = 1; i < rooms.size(); i++) {
            RoomTemplate room = rooms.get(i);
            PlacedRoom placed = tryPlaceRoom(room);

            if (placed != null) {
                System.out.println("RoomPlacer: Placed room " + placed);
            } else {
                System.out.println("RoomPlacer: Failed to place room " + room.getName() + " after " + MAX_PLACEMENT_ATTEMPTS + " attempts");
            }
        }

        System.out.println("RoomPlacer: Placed " + placedRooms.size() + "/" + rooms.size() + " rooms");
        System.out.println("RoomPlacer: " + unconnectedDoors.size() + " unconnected doors remaining");

        return new ArrayList<>(placedRooms);
    }

    /**
     * Try to place a room by finding a valid connection with existing doors.
     * @param template Room template to place
     * @return Placed room or null if placement failed
     */
    private PlacedRoom tryPlaceRoom(RoomTemplate template) {
        // Try to connect with unconnected doors
        List<WorldDoor> doorsToTry = new ArrayList<>(unconnectedDoors);

        // DEBUG
        System.out.println("RoomPlacer: Trying to place " + template.getName() + " (has " + template.getDoors().size() + " doors)");
        for (com.game.systems.dungeon.DoorConnection door : template.getDoors()) {
            System.out.println("  Template door: " + door.getDirection() + " at (" + door.getX() + "," + door.getY() + ")");
        }
        System.out.println("RoomPlacer: Available unconnected doors: " + unconnectedDoors.size());

        // Shuffle for variety
        java.util.Collections.shuffle(doorsToTry, random);

        for (WorldDoor existingDoor : doorsToTry) {
            // Find compatible doors in the template
            List<DoorMatcher.DoorPair> pairs = new ArrayList<>();
            for (com.game.systems.dungeon.DoorConnection templateDoor : template.getDoors()) {
                if (DoorMatcher.canDoorsConnect(existingDoor.getDoor(), templateDoor)) {
                    pairs.add(new DoorMatcher.DoorPair(existingDoor, templateDoor));
                }
            }

            // DEBUG
            if (pairs.isEmpty()) {
                System.out.println("  Door " + existingDoor.getDoor().getDirection() + " at (" + existingDoor.getWorldX() + "," + existingDoor.getWorldY() + "): no compatible pairs");
            } else {
                System.out.println("  Door " + existingDoor.getDoor().getDirection() + " at (" + existingDoor.getWorldX() + "," + existingDoor.getWorldY() + "): found " + pairs.size() + " compatible pairs");
            }

            // Try each compatible door pair
            for (DoorMatcher.DoorPair pair : pairs) {
                Vector2 position = pair.calculatePlacementPosition();
                if (position == null) {
                    System.out.println("    Pair " + pair + ": position calculation returned null");
                    continue;
                }

                // Create tentative placed room
                PlacedRoom candidate = new PlacedRoom(template, position.x, position.y, nextRoomId);

                // Validate placement (no overlaps except at connection point)
                if (isValidPlacement(candidate, existingDoor)) {
                    // Accept placement
                    nextRoomId++;
                    addPlacedRoom(candidate);

                    // Connect the doors
                    connectDoors(existingDoor, candidate);

                    System.out.println("    SUCCESS - Placed at (" + position.x + "," + position.y + ")");
                    return candidate;
                } else {
                    System.out.println("    Pair " + pair + ": failed overlap check at (" + position.x + "," + position.y + ")");
                }
            }
        }

        // Failed to place via door connections
        // Last resort: place near a random existing room with some offset
        if (!placedRooms.isEmpty()) {
            for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
                PlacedRoom nearbyRoom = placedRooms.get(random.nextInt(placedRooms.size()));
                float offsetX = (random.nextFloat() * 200 - 100);  // Random offset within range
                float offsetY = (random.nextFloat() * 200 - 100);

                PlacedRoom candidate = new PlacedRoom(
                    template,
                    nearbyRoom.getWorldX() + offsetX,
                    nearbyRoom.getWorldY() + offsetY,
                    nextRoomId
                );

                if (isValidPlacement(candidate, null)) {
                    nextRoomId++;
                    addPlacedRoom(candidate);
                    return candidate;
                }
            }
        }

        return null;  // Failed to place
    }

    /**
     * Check if a room placement is valid (no overlaps except at connection).
     */
    private boolean isValidPlacement(PlacedRoom candidate, WorldDoor connectionDoor) {
        for (PlacedRoom existing : placedRooms) {
            // Check if rooms overlap
            if (candidate.overlaps(existing, TILE_SIZE)) {
                // Allow overlap only if this is the connection point
                if (connectionDoor == null || connectionDoor.getParentRoom() != existing) {
                    return false;  // Invalid overlap
                }
                // Otherwise, overlap at connection point is expected
            }
        }
        return true;
    }

    /**
     * Add a placed room and register its doors.
     */
    private void addPlacedRoom(PlacedRoom room) {
        placedRooms.add(room);

        // Add all doors from this room to unconnected list
        unconnectedDoors.addAll(room.getWorldDoors());
    }

    /**
     * Connect two doors (mark as connected, remove from unconnected list).
     */
    private void connectDoors(WorldDoor door1, PlacedRoom room2) {
        // Mark door1 as connected
        door1.setConnectedRoom(room2);
        unconnectedDoors.remove(door1);

        // Find matching door in room2 and mark it connected
        for (WorldDoor door2 : room2.getWorldDoors()) {
            if (DoorMatcher.areDoorsAligned(door1, door2)) {
                door2.setConnectedRoom(door1.getParentRoom());
                unconnectedDoors.remove(door2);
                break;
            }
        }
    }

    public List<PlacedRoom> getPlacedRooms() {
        return new ArrayList<>(placedRooms);
    }

    public List<WorldDoor> getUnconnectedDoors() {
        return new ArrayList<>(unconnectedDoors);
    }

    public int getPlacedRoomCount() {
        return placedRooms.size();
    }
}
