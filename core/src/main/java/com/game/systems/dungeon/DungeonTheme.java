package com.game.systems.dungeon;

import java.util.*;

/**
 * Collection of room templates for a dungeon theme.
 * A theme represents a visual/gameplay style (e.g., "forest", "cave", "castle").
 */
public class DungeonTheme {
    private final String themeName;
    private final List<RoomTemplate> rooms;
    private final Map<String, List<RoomTemplate>> roomsByCategory;
    private final Random random;
    private final com.badlogic.gdx.maps.tiled.TiledMap sourceMap;  // Keep for tilesets

    public DungeonTheme(String themeName, List<RoomTemplate> rooms, com.badlogic.gdx.maps.tiled.TiledMap sourceMap) {
        this.themeName = themeName;
        this.rooms = new ArrayList<>(rooms);
        this.sourceMap = sourceMap;
        this.roomsByCategory = new HashMap<>();
        this.random = new Random();

        // Index rooms by category for quick lookup
        for (RoomTemplate room : rooms) {
            String category = room.getCategory();
            roomsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(room);
        }
    }

    public com.badlogic.gdx.maps.tiled.TiledMap getSourceMap() {
        return sourceMap;
    }

    public String getThemeName() {
        return themeName;
    }

    public List<RoomTemplate> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * Get rooms by category (small, medium, large).
     */
    public List<RoomTemplate> getRoomsByCategory(String category) {
        return new ArrayList<>(roomsByCategory.getOrDefault(category, Collections.emptyList()));
    }

    /**
     * Get rooms that have a specific tag.
     */
    public List<RoomTemplate> getRoomsByTag(String tag) {
        List<RoomTemplate> result = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            if (room.hasTag(tag)) {
                result.add(room);
            }
        }
        return result;
    }

    /**
     * Get rooms that have ALL specified tags.
     */
    public List<RoomTemplate> getRoomsByTags(Set<String> tags) {
        List<RoomTemplate> result = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            boolean hasAllTags = true;
            for (String tag : tags) {
                if (!room.hasTag(tag)) {
                    hasAllTags = false;
                    break;
                }
            }
            if (hasAllTags) {
                result.add(room);
            }
        }
        return result;
    }

    /**
     * Get a random room from the theme, weighted by room weight.
     */
    public RoomTemplate getRandomRoom() {
        if (rooms.isEmpty()) {
            return null;
        }

        // Calculate total weight
        int totalWeight = 0;
        for (RoomTemplate room : rooms) {
            totalWeight += room.getWeight();
        }

        // Select weighted random
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (RoomTemplate room : rooms) {
            currentWeight += room.getWeight();
            if (randomValue < currentWeight) {
                return room;
            }
        }

        return rooms.get(rooms.size() - 1);  // Fallback
    }

    /**
     * Get a random room from a specific category.
     */
    public RoomTemplate getRandomRoomByCategory(String category) {
        List<RoomTemplate> categoryRooms = roomsByCategory.get(category);
        if (categoryRooms == null || categoryRooms.isEmpty()) {
            return null;
        }

        // Calculate total weight for this category
        int totalWeight = 0;
        for (RoomTemplate room : categoryRooms) {
            totalWeight += room.getWeight();
        }

        // Select weighted random
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (RoomTemplate room : categoryRooms) {
            currentWeight += room.getWeight();
            if (randomValue < currentWeight) {
                return room;
            }
        }

        return categoryRooms.get(categoryRooms.size() - 1);  // Fallback
    }

    /**
     * Get a random room with a specific tag.
     */
    public RoomTemplate getRandomRoomByTag(String tag) {
        List<RoomTemplate> taggedRooms = getRoomsByTag(tag);
        if (taggedRooms.isEmpty()) {
            return null;
        }

        // Calculate total weight
        int totalWeight = 0;
        for (RoomTemplate room : taggedRooms) {
            totalWeight += room.getWeight();
        }

        // Select weighted random
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (RoomTemplate room : taggedRooms) {
            currentWeight += room.getWeight();
            if (randomValue < currentWeight) {
                return room;
            }
        }

        return taggedRooms.get(taggedRooms.size() - 1);  // Fallback
    }

    /**
     * Get all rooms marked as closers (dead-ends for final cleanup).
     */
    public List<RoomTemplate> getCloserRooms() {
        List<RoomTemplate> closers = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            if (room.isCloser()) {
                closers.add(room);
            }
        }
        return closers;
    }

    /**
     * Get a compatible closer room for a specific door direction.
     * Returns a random closer that has a door facing the opposite direction.
     *
     * @param direction The direction of the open door that needs closing
     * @return Compatible closer room, or null if none found
     */
    public RoomTemplate getCompatibleCloser(Direction direction) {
        // Find closers with doors in the opposite direction
        Direction oppositeDir = direction.opposite();
        List<RoomTemplate> compatibleClosers = new ArrayList<>();

        for (RoomTemplate closer : getCloserRooms()) {
            // Check if closer has a door facing the opposite direction
            for (DoorConnection door : closer.getDoors()) {
                if (door.getDirection() == oppositeDir) {
                    compatibleClosers.add(closer);
                    break;  // Only add each closer once
                }
            }
        }

        if (compatibleClosers.isEmpty()) {
            return null;
        }

        // Return random compatible closer
        return compatibleClosers.get(random.nextInt(compatibleClosers.size()));
    }

    /**
     * Set the random seed for deterministic room selection.
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    @Override
    public String toString() {
        return String.format("DungeonTheme[name=%s, rooms=%d, categories=%s]",
            themeName, rooms.size(), roomsByCategory.keySet());
    }
}
