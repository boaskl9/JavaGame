package com.game.systems.dungeon;

/**
 * Represents a door connection point in a room template.
 * Doors are used to connect rooms during dungeon generation.
 *
 * Doors now have dimensions (width/height) to enable size-based matching.
 * Direction can be auto-calculated based on door position relative to room bounds.
 */
public class DoorConnection {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final Direction direction;
    private final String doorType;

        /**
     * Create a door connection with auto-calculated direction.
     * Direction is determined by door position relative to room bounds.
     *
     * @param x X position in room coordinates (pixels)
     * @param y Y position in room coordinates (pixels)
     * @param width Door width in pixels
     * @param height Door height in pixels
     * @param roomBounds Room boundaries to calculate direction from
     * @param doorType Type of door ("normal", "locked", "boss", "secret")
     */
    public DoorConnection(float x, float y, float width, float height, RoomBounds roomBounds, String doorType) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = calculateDirection(x, y, width, height, roomBounds);
        this.doorType = doorType != null ? doorType : "normal";
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getDoorType() {
        return doorType;
    }

    /**
     * Auto-calculate door direction based on position relative to room bounds.
     * Doors at room edges get assigned the appropriate direction.
     *
     * Logic:
     * - If door is at top edge (y near room top) → NORTH
     * - If door is at bottom edge (y near 0) → SOUTH
     * - If door is at right edge (x near room right) → EAST
     * - If door is at left edge (x near 0) → WEST
     *
     * @param doorX Door X position in room coordinates
     * @param doorY Door Y position in room coordinates
     * @param doorWidth Door width
     * @param doorHeight Door height
     * @param roomBounds Room boundaries
     * @return Calculated direction
     */
    private static Direction calculateDirection(float doorX, float doorY, float doorWidth, float doorHeight, RoomBounds roomBounds) {
        // Convert room bounds from tiles to pixels
        float roomWidthPx = roomBounds.getWidth() * 16;
        float roomHeightPx = roomBounds.getHeight() * 16;

        // Tolerance for edge detection (8 pixels)
        float tolerance = 8.0f;

        // Calculate door center
        float doorCenterX = doorX + doorWidth / 2;
        float doorCenterY = doorY + doorHeight / 2;

        // Check which edge the door is closest to
        float distToLeft = doorCenterX;
        float distToRight = roomWidthPx - doorCenterX;
        float distToBottom = doorCenterY;
        float distToTop = roomHeightPx - doorCenterY;

        // Find minimum distance to determine which edge
        float minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToBottom, distToTop));

        if (minDist == distToLeft && distToLeft <= tolerance) {
            return Direction.WEST;
        } else if (minDist == distToRight && distToRight <= tolerance) {
            return Direction.EAST;
        } else if (minDist == distToBottom && distToBottom <= tolerance) {
            return Direction.SOUTH;
        } else if (minDist == distToTop && distToTop <= tolerance) {
            return Direction.NORTH;
        }

        // Fallback: if not near any edge, determine by closest edge
        if (minDist == distToLeft) return Direction.WEST;
        if (minDist == distToRight) return Direction.EAST;
        if (minDist == distToBottom) return Direction.SOUTH;
        return Direction.NORTH;
    }

    /**
     * Check if this door can connect with another door.
     * Doors match if they:
     * 1. Face opposite directions
     * 2. Have compatible types
     * 3. Have exact size match (width and height must match)
     */
    public boolean canConnectWith(DoorConnection other) {
        if (other == null || direction == null || other.direction == null) {
            return false;
        }

        // Directions must be opposite
        if (direction != other.direction.opposite()) {
            return false;
        }

        // Sizes must match (with tolerance for manual placement in Tiled)
        // Increased tolerance for irregular/cave rooms where doors might vary slightly
        float tolerance = 2.0f;  // 2 pixels tolerance (was 0.1)
        if (Math.abs(width - other.width) > tolerance || Math.abs(height - other.height) > tolerance) {
            return false;
        }

        // Door types must be compatible
        // For now, all types can connect (can add restrictions later)
        return true;
    }

    @Override
    public String toString() {
        return String.format("Door[%s, type=%s, size=%.0fx%.0f, pos=(%.0f,%.0f)]",
            direction, doorType, width, height, x, y);
    }
}
