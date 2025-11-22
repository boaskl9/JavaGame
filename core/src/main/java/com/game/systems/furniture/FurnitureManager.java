package com.game.systems.furniture;

import com.game.integration.WorldManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all placed furniture across different levels.
 * Persists furniture when changing levels (similar to WorldItemManager).
 * Singleton pattern to maintain state across level transitions.
 */
public class FurnitureManager {
    private static FurnitureManager instance;

    // Store furniture by level ID
    private final Map<String, List<FurnitureEntity>> furnitureByLevel;

    private FurnitureManager() {
        this.furnitureByLevel = new HashMap<>();
    }

    /**
     * Get the singleton instance of FurnitureManager.
     * @return The furniture manager instance
     */
    public static FurnitureManager getInstance() {
        if (instance == null) {
            instance = new FurnitureManager();
        }
        return instance;
    }

    /**
     * Place furniture in a level.
     * Adds the furniture to the manager's registry for the specified level.
     * @param levelId The level ID where furniture is placed
     * @param furniture The furniture entity to place
     */
    public void placeFurniture(String levelId, FurnitureEntity furniture) {
        furnitureByLevel.computeIfAbsent(levelId, k -> new ArrayList<>()).add(furniture);
        System.out.println("Placed furniture " + furniture.getItemId() + " in level " + levelId);
    }

    /**
     * Remove furniture from a level.
     * Call this when furniture is picked up.
     * @param levelId The level ID
     * @param furniture The furniture entity to remove
     * @return true if furniture was found and removed
     */
    public boolean removeFurniture(String levelId, FurnitureEntity furniture) {
        List<FurnitureEntity> levelFurniture = furnitureByLevel.get(levelId);
        if (levelFurniture != null) {
            boolean removed = levelFurniture.remove(furniture);
            if (removed) {
                System.out.println("Removed furniture " + furniture.getItemId() + " from level " + levelId);
            }
            return removed;
        }
        return false;
    }

    /**
     * Get all furniture for a specific level.
     * @param levelId The level ID
     * @return List of furniture entities (may be empty, never null)
     */
    public List<FurnitureEntity> getFurnitureForLevel(String levelId) {
        return furnitureByLevel.getOrDefault(levelId, new ArrayList<>());
    }

    /**
     * Load all furniture for a level into the world.
     * Call this when a level is loaded to restore placed furniture.
     * @param levelId The level ID
     * @param world The world manager to add furniture to
     */
    public void loadFurnitureIntoWorld(String levelId, WorldManager world) {
        List<FurnitureEntity> levelFurniture = getFurnitureForLevel(levelId);
        if (levelFurniture.isEmpty()) {
            System.out.println("No furniture to load for level: " + levelId);
            return;
        }

        System.out.println("Loading " + levelFurniture.size() + " furniture items for level: " + levelId);
        for (FurnitureEntity furniture : levelFurniture) {
            world.addGameObject(furniture);
        }
    }

    /**
     * Clear all furniture for a specific level.
     * Useful for cleaning up or resetting levels.
     * @param levelId The level ID to clear
     */
    public void clearLevel(String levelId) {
        List<FurnitureEntity> removed = furnitureByLevel.remove(levelId);
        if (removed != null) {
            System.out.println("Cleared " + removed.size() + " furniture items from level: " + levelId);
        }
    }

    /**
     * Clear all furniture from all levels.
     * Use with caution - typically only for new game or testing.
     */
    public void clearAll() {
        int total = furnitureByLevel.values().stream().mapToInt(List::size).sum();
        furnitureByLevel.clear();
        System.out.println("Cleared all furniture (" + total + " items)");
    }

    /**
     * Get total count of furniture across all levels.
     * @return Total furniture count
     */
    public int getTotalFurnitureCount() {
        return furnitureByLevel.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Get count of furniture in a specific level.
     * @param levelId The level ID
     * @return Furniture count for that level
     */
    public int getLevelFurnitureCount(String levelId) {
        List<FurnitureEntity> levelFurniture = furnitureByLevel.get(levelId);
        return levelFurniture != null ? levelFurniture.size() : 0;
    }

    /**
     * Debug: Print all furniture locations.
     */
    public void debugPrint() {
        System.out.println("=== Furniture Manager Debug ===");
        System.out.println("Total furniture: " + getTotalFurnitureCount());
        for (Map.Entry<String, List<FurnitureEntity>> entry : furnitureByLevel.entrySet()) {
            String levelId = entry.getKey();
            List<FurnitureEntity> furniture = entry.getValue();
            System.out.println("  Level '" + levelId + "': " + furniture.size() + " items");
            for (FurnitureEntity f : furniture) {
                System.out.println("    - " + f.getItemId() + " at (" +
                        f.getTransform().getX() + ", " + f.getTransform().getY() + ")");
            }
        }
        System.out.println("==============================");
    }
}
