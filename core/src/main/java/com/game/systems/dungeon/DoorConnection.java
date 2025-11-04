package com.game.systems.dungeon;

/**
 * Represents a door connection point in a room template.
 * Doors are used to connect rooms during dungeon generation.
 */
public class DoorConnection {
    private final float x;
    private final float y;
    private final Direction direction;
    private final String doorType;

    /**
     * Create a door connection.
     * @param x X position in room coordinates (pixels)
     * @param y Y position in room coordinates (pixels)
     * @param direction Which direction this door faces
     * @param doorType Type of door ("normal", "locked", "boss", "secret")
     */
    public DoorConnection(float x, float y, Direction direction, String doorType) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.doorType = doorType != null ? doorType : "normal";
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getDoorType() {
        return doorType;
    }

    /**
     * Check if this door can connect with another door.
     * Doors match if they face opposite directions and have compatible types.
     */
    public boolean canConnectWith(DoorConnection other) {
        if (other == null || direction == null || other.direction == null) {
            return false;
        }

        // Directions must be opposite
        if (direction != other.direction.opposite()) {
            return false;
        }

        // Door types must be compatible
        // For now, all types can connect (can add restrictions later)
        return true;
    }

    @Override
    public String toString() {
        return String.format("Door[%s, type=%s, pos=(%.0f,%.0f)]",
            direction, doorType, x, y);
    }
}
