package com.game.systems.dungeon;

/**
 * Cardinal directions for door connections.
 * Used to match doors when connecting rooms.
 */
public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    /**
     * Get the opposite direction.
     * Used for matching doors (north door connects to south door).
     */
    public Direction opposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
            default: return this;
        }
    }

    /**
     * Parse direction from string (case-insensitive).
     */
    public static Direction fromString(String str) {
        if (str == null) return null;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
