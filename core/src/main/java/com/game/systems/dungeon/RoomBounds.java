package com.game.systems.dungeon;

import com.badlogic.gdx.math.Rectangle;

/**
 * Represents the bounding box of a room in tile coordinates.
 * Used for extracting rooms from theme files and placing them during generation.
 */
public class RoomBounds {
    private final int x;      // Left edge in tiles
    private final int y;      // Top edge in tiles
    private final int width;  // Width in tiles
    private final int height; // Height in tiles

    /**
     * Create room bounds.
     * @param x Left edge in tile coordinates
     * @param y Top edge in tile coordinates
     * @param width Width in tiles
     * @param height Height in tiles
     */
    public RoomBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Get the right edge (exclusive).
     */
    public int getRight() {
        return x + width;
    }

    /**
     * Get the bottom edge (exclusive).
     */
    public int getBottom() {
        return y + height;
    }

    /**
     * Get center X coordinate.
     */
    public float getCenterX() {
        return x + width / 2f;
    }

    /**
     * Get center Y coordinate.
     */
    public float getCenterY() {
        return y + height / 2f;
    }

    /**
     * Check if a tile coordinate is within this room.
     */
    public boolean contains(int tileX, int tileY) {
        return tileX >= x && tileX < x + width &&
               tileY >= y && tileY < y + height;
    }

    /**
     * Check if this room intersects with another.
     */
    public boolean intersects(RoomBounds other) {
        return !(other.x >= x + width || other.x + other.width <= x ||
                 other.y >= y + height || other.y + other.height <= y);
    }

    /**
     * Convert to libGDX Rectangle (in pixel coordinates).
     */
    public Rectangle toPixelRectangle(int tileSize) {
        return new Rectangle(x * tileSize, y * tileSize, width * tileSize, height * tileSize);
    }

    /**
     * Get area in tiles.
     */
    public int getArea() {
        return width * height;
    }

    @Override
    public String toString() {
        return String.format("RoomBounds[x=%d, y=%d, w=%d, h=%d]", x, y, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RoomBounds)) return false;
        RoomBounds other = (RoomBounds) obj;
        return x == other.x && y == other.y && width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }
}
