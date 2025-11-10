package com.game.systems.dungeon.generation;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.dungeon.DoorConnection;
import com.game.systems.dungeon.RoomBounds;
import com.game.systems.dungeon.RoomTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a room template placed at a specific position in the dungeon.
 * Tracks world position offset for stitching rooms together.
 */
public class PlacedRoom {
    private final RoomTemplate template;
    private final Vector2 worldPosition;  // Position in world coordinates (pixels)
    private final int id;  // Unique ID for this placed instance
    private final List<WorldDoor> cachedWorldDoors;  // Cache to avoid recreating doors

    /**
     * Create a placed room.
     * @param template The room template
     * @param worldX World X position in pixels
     * @param worldY World Y position in pixels
     * @param id Unique placement ID
     */
    public PlacedRoom(RoomTemplate template, float worldX, float worldY, int id) {
        this.template = template;
        this.worldPosition = new Vector2(worldX, worldY);
        this.id = id;

        // Create and cache world doors once
        this.cachedWorldDoors = new ArrayList<>();
        for (DoorConnection door : template.getDoors()) {
            float worldDoorX = worldPosition.x + door.getX();
            float worldDoorY = worldPosition.y + door.getY();
            cachedWorldDoors.add(new WorldDoor(door, worldDoorX, worldDoorY, this));
        }
    }

    public RoomTemplate getTemplate() {
        return template;
    }

    public Vector2 getWorldPosition() {
        return new Vector2(worldPosition);
    }

    public float getWorldX() {
        return worldPosition.x;
    }

    public float getWorldY() {
        return worldPosition.y;
    }

    public int getId() {
        return id;
    }

    /**
     * Get room bounds in world coordinates (pixels).
     */
    public RoomBounds getWorldBounds(int tileSize) {
        RoomBounds templateBounds = template.getBounds();
        int worldX = (int) (worldPosition.x / tileSize);
        int worldY = (int) (worldPosition.y / tileSize);
        return new RoomBounds(worldX, worldY, templateBounds.getWidth(), templateBounds.getHeight());
    }

    /**
     * Get all doors in world coordinates.
     * Returns the same cached list to allow reference-based removal.
     */
    public List<WorldDoor> getWorldDoors() {
        return cachedWorldDoors;
    }

    /**
     * Check if this placed room overlaps with another (excluding doors).
     * @param other Another placed room
     * @param tileSize Tile size in pixels
     * @return true if rooms overlap
     */
    public boolean overlaps(PlacedRoom other, int tileSize) {
        RoomBounds thisBounds = getWorldBounds(tileSize);
        RoomBounds otherBounds = other.getWorldBounds(tileSize);
        return thisBounds.intersects(otherBounds);
    }

    @Override
    public String toString() {
        return String.format("PlacedRoom[id=%d, template=%s, pos=(%.0f,%.0f)]",
            id, template.getName(), worldPosition.x, worldPosition.y);
    }

    /**
     * Represents a door in world coordinates.
     */
    public static class WorldDoor {
        private final DoorConnection door;
        private final float worldX;
        private final float worldY;
        private final PlacedRoom parentRoom;
        private PlacedRoom connectedRoom;  // Room this door connects to (if any)

        public WorldDoor(DoorConnection door, float worldX, float worldY, PlacedRoom parentRoom) {
            this.door = door;
            this.worldX = worldX;
            this.worldY = worldY;
            this.parentRoom = parentRoom;
            this.connectedRoom = null;
        }

        public DoorConnection getDoor() {
            return door;
        }

        public float getWorldX() {
            return worldX;
        }

        public float getWorldY() {
            return worldY;
        }

        public PlacedRoom getParentRoom() {
            return parentRoom;
        }

        public PlacedRoom getConnectedRoom() {
            return connectedRoom;
        }

        public void setConnectedRoom(PlacedRoom room) {
            this.connectedRoom = room;
        }

        public boolean isConnected() {
            return connectedRoom != null;
        }

        @Override
        public String toString() {
            return String.format("WorldDoor[%s, pos=(%.0f,%.0f), connected=%s]",
                door.getDirection(), worldX, worldY, isConnected());
        }
    }
}
