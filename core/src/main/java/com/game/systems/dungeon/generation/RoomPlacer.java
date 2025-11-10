package com.game.systems.dungeon.generation;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.dungeon.DoorConnection;
import com.game.systems.dungeon.RoomBounds;
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
    private static final int OVERLAP_TOLERANCE_TILES = 2;  // Allow small overlaps for irregular rooms

    private final List<PlacedRoom> placedRooms;
    private List<RoomTemplate> rooms;
    private final int targetBudget;
    private final List<WorldDoor> unconnectedDoors;
    private final Random random;
    private int nextRoomId = 0;
    private int budgetUsed;
    private final com.game.systems.dungeon.DungeonTheme theme;
    private final RoomSelector roomSelector;

    public RoomPlacer(int targetBudget, long seed, List<RoomTemplate> inputRooms, com.game.systems.dungeon.DungeonTheme theme) {
        this.placedRooms = new ArrayList<>();
        this.unconnectedDoors = new ArrayList<>();
        this.random = new Random(seed);
        this.targetBudget = targetBudget;
        this.rooms = inputRooms;
        this.theme = theme;
        this.roomSelector = new RoomSelector(inputRooms, random);
    }

    /**
     * Place rooms from the allocated list.
     * @return List of successfully placed rooms
     */
    public List<PlacedRoom> placeRooms() {
        if (rooms.isEmpty()) {
            System.err.println("RoomPlacer: No rooms to place");
            return placedRooms;
        }



        // Place first room at origin (aligned to tile grid)
        RoomTemplate firstRoom = pickRoom();
        float startX = 0;
        float startY = 0;
        PlacedRoom firstPlaced = new PlacedRoom(firstRoom, startX, startY, nextRoomId++);
        addPlacedRoom(firstPlaced);
        System.out.println("RoomPlacer: Placed first room " + firstPlaced);
        System.out.println("RoomPlacer: First room has " + firstPlaced.getWorldDoors().size() + " doors:");
        for (WorldDoor door : firstPlaced.getWorldDoors()) {
            System.out.println("  - " + door.getDoor().getDirection() + " at (" + door.getWorldX() + "," + door.getWorldY() + ")");
        }

        int attempts = 0;

        // Place remaining rooms
        while (getRemainingBudget() > 5 && !getUnconnectedDoors().isEmpty() && attempts < 50){

            for (PlacedRoom.WorldDoor door : getUnconnectedDoors()) {
                System.out.println("door: "+ door.toString());
            }


            if (canPickRoom()) {
                RoomTemplate room = pickRoom();
                PlacedRoom placed = tryPlaceRoom(room);

                if (placed != null) {
                    System.out.println("RoomPlacer: Placed room " + placed);
                    budgetUsed += placed.getTemplate().getCost();
                } else {
                    System.out.println("RoomPlacer: Failed to place room " + room.getName() + " after " + MAX_PLACEMENT_ATTEMPTS + " attempts");
                    attempts++;
                }
            }
            else {
                break;
            }
        }

        // Fill out dead ends with closer rooms
        System.out.println("\n[Phase 3: Door Closure]");
        closeUnconnectedDoors();

        System.out.println("RoomPlacer: failed attempts: " + attempts);


        System.out.println("RoomPlacer: Placed " + placedRooms.size() + "/" + rooms.size() + " rooms");
        System.out.println("RoomPlacer: " + unconnectedDoors.size() + " unconnected doors remaining");

        return new ArrayList<>(placedRooms);
    }

    private boolean canPickRoom() {

        for (RoomTemplate r : rooms) {
            if (r.getCost() < getRemainingBudget()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Pick the next room using intelligent selection strategy.
     * @return Selected room
     */
    private RoomTemplate pickRoom() {
        RoomTemplate selected = roomSelector.selectRoom(getRemainingBudget(), targetBudget);

        if (selected == null) {
            throw new IllegalStateException("ERROR: no available rooms within budget");
        }

        System.out.println("picked room named " + selected.getName() + " (" + selected.getDoors().size() + " doors)");
        return selected;
    }

    private int getRemainingBudget()
    {
        return targetBudget - budgetUsed;
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
            System.out.println("  " + door);  // Use toString() to show sizes
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

                    // Mark door1 as connected
                    existingDoor.setConnectedRoom(candidate);
                    unconnectedDoors.remove(existingDoor);

                    // Find the matching door in candidate that corresponds to pair.templateDoor
                    for (WorldDoor candidateDoor : candidate.getWorldDoors()) {
                        // Match by DoorConnection reference - the WorldDoor wraps the same templateDoor
                        if (candidateDoor.getDoor() == pair.templateDoor) {
                            candidateDoor.setConnectedRoom(existingDoor.getParentRoom());
                            unconnectedDoors.remove(candidateDoor);
                            System.out.println("      Connected door pair: " + existingDoor.getDoor().getDirection() + " <-> " + candidateDoor.getDoor().getDirection());
                            break;
                        }
                    }

                    System.out.println("    SUCCESS - Placed at (" + position.x + "," + position.y + ")");
                    return candidate;
                } else {
                    System.out.println("    Pair " + pair + ": failed overlap check at (" + position.x + "," + position.y + ")");
                }
            }
        }

        // Failed to place via door connections
        // DO NOT use fallback random placement - it creates disconnected islands
        // Rooms MUST connect via doors or not be placed at all
        System.out.println("    FAILED - No valid door connections found for " + template.getName());
        return null;  // Failed to place
    }

    /**
     * Check if a room placement is valid (no overlaps except at connection).
     */
    private boolean isValidPlacement(PlacedRoom candidate, WorldDoor connectionDoor) {
        for (PlacedRoom existing : placedRooms) {
            // Check if rooms overlap (using rectangular bounds)
            if (candidate.overlaps(existing, TILE_SIZE)) {
                // For irregular rooms, rectangular bounds might overlap even when connecting properly
                // Allow overlap if this is the connection room OR if overlap is small
                if (connectionDoor != null && connectionDoor.getParentRoom() == existing) {
                    System.out.println("      OVERLAP ALLOWED: Connecting to this room");
                    continue;  // Allow overlap with connection target
                }

                // Calculate overlap area to see if it's significant
                RoomBounds candidateBounds = candidate.getWorldBounds(TILE_SIZE);
                RoomBounds existingBounds = existing.getWorldBounds(TILE_SIZE);

                int overlapX1 = Math.max(candidateBounds.getX(), existingBounds.getX());
                int overlapY1 = Math.max(candidateBounds.getY(), existingBounds.getY());
                int overlapX2 = Math.min(candidateBounds.getRight(), existingBounds.getRight());
                int overlapY2 = Math.min(candidateBounds.getBottom(), existingBounds.getBottom());

                int overlapWidth = overlapX2 - overlapX1;
                int overlapHeight = overlapY2 - overlapY1;
                int overlapArea = overlapWidth * overlapHeight;

                // Allow small overlaps for irregular rooms
                // This accounts for rectangular bounds being larger than actual room shape
                if (overlapArea <= OVERLAP_TOLERANCE_TILES) {
                    System.out.println("      OVERLAP ALLOWED: Small overlap (" + overlapArea + " tiles, tolerance=" + OVERLAP_TOLERANCE_TILES + ") with " + existing.getTemplate().getName());
                    continue;
                }

                System.out.println("      OVERLAP REJECTED: Candidate at (" + candidate.getWorldX() + "," + candidate.getWorldY() + ") overlaps " + overlapArea + " tiles with " + existing.getTemplate().getName() + " at (" + existing.getWorldX() + "," + existing.getWorldY() + ")");
                return false;  // Significant overlap
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

    /**
     * Close all unconnected doors using closer rooms from the theme.
     * Closer rooms are free (don't consume budget) and are specifically designed as dead-ends.
     */
    private void closeUnconnectedDoors() {
        // Make a copy to avoid concurrent modification
        List<WorldDoor> doorsToClose = new ArrayList<>(unconnectedDoors);
        int closedCount = 0;

        System.out.println("RoomPlacer: Attempting to close " + doorsToClose.size() + " unconnected doors");

        for (WorldDoor openDoor : doorsToClose) {
            // Get a compatible closer room for this door direction
            RoomTemplate closer = theme.getCompatibleCloser(openDoor.getDoor().getDirection());

            if (closer == null) {
                System.out.println("  Door " + openDoor.getDoor().getDirection() + " at (" + openDoor.getWorldX() + "," + openDoor.getWorldY() + "): no compatible closer found");
                continue;
            }

            // Try to place the closer room
            PlacedRoom placedCloser = tryPlaceCloser(closer, openDoor);

            if (placedCloser != null) {
                closedCount++;
                System.out.println("  Door " + openDoor.getDoor().getDirection() + " at (" + openDoor.getWorldX() + "," + openDoor.getWorldY() + "): CLOSED with " + closer.getName());
            } else {
                System.out.println("  Door " + openDoor.getDoor().getDirection() + " at (" + openDoor.getWorldX() + "," + openDoor.getWorldY() + "): failed to place closer");
            }
        }

        System.out.println("RoomPlacer: Closed " + closedCount + "/" + doorsToClose.size() + " doors");
    }

    /**
     * Try to place a closer room to seal a specific door.
     * @param closerTemplate The closer room template
     * @param doorToClose The door to close
     * @return Placed room or null if placement failed
     */
    private PlacedRoom tryPlaceCloser(RoomTemplate closerTemplate, WorldDoor doorToClose) {
        // Find all compatible doors in the closer template
        List<DoorMatcher.DoorPair> pairs = new ArrayList<>();
        for (com.game.systems.dungeon.DoorConnection closerDoor : closerTemplate.getDoors()) {
            if (DoorMatcher.canDoorsConnect(doorToClose.getDoor(), closerDoor)) {
                pairs.add(new DoorMatcher.DoorPair(doorToClose, closerDoor));
            }
        }

        if (pairs.isEmpty()) {
            return null;
        }

        // Try each compatible door pair
        for (DoorMatcher.DoorPair pair : pairs) {
            Vector2 position = pair.calculatePlacementPosition();
            if (position == null) {
                continue;
            }

            // Create tentative placed room
            PlacedRoom candidate = new PlacedRoom(closerTemplate, position.x, position.y, nextRoomId);

            // Validate placement (closers can overlap slightly with connection room)
            if (isValidPlacement(candidate, doorToClose)) {
                // Accept placement
                nextRoomId++;
                addPlacedRoom(candidate);

                // Connect the doors
                doorToClose.setConnectedRoom(candidate);
                unconnectedDoors.remove(doorToClose);

                // Find and connect the matching door in the closer
                for (WorldDoor closerDoor : candidate.getWorldDoors()) {
                    if (closerDoor.getDoor() == pair.templateDoor) {
                        closerDoor.setConnectedRoom(doorToClose.getParentRoom());
                        unconnectedDoors.remove(closerDoor);
                        break;
                    }
                }

                return candidate;
            }
        }

        return null;
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
