package com.game.systems.dungeon.generation;

import com.game.systems.dungeon.DungeonTheme;
import com.game.systems.dungeon.DungeonThemeRegistry;
import com.game.systems.dungeon.RoomTemplate;

import java.util.List;

/**
 * Main dungeon generation orchestrator.
 * Coordinates budget allocation, room placement, and final assembly.
 */
public class DungeonGenerator {
    private static final float DEFAULT_BUDGET_TOLERANCE = 0.15f;  // ±15%

    /**
     * Generate a dungeon layout from a theme.
     * @param themeName Name of the theme to use
     * @param targetBudget Target cost budget for room selection
     * @param seed Random seed for deterministic generation
     * @return Generation result containing placed rooms
     */
    public static DungeonGenerationResult generate(String themeName, int targetBudget, long seed) {
        System.out.println("==============================================");
        System.out.println("DungeonGenerator: Starting generation");
        System.out.println("  Theme: " + themeName);
        System.out.println("  Budget: " + targetBudget);
        System.out.println("  Seed: " + seed);
        System.out.println("==============================================");

        // Load theme
        DungeonTheme theme = DungeonThemeRegistry.getInstance().loadTheme(themeName);
        if (theme == null) {
            System.err.println("DungeonGenerator: Failed to load theme '" + themeName + "'");
            return null;
        }

        if (theme.getRoomCount() == 0) {
            System.err.println("DungeonGenerator: Theme has no rooms");
            return null;
        }

        // Phase 1: Budget Allocation
        System.out.println("\n[Phase 1: Budget Allocation]");
        BudgetAllocator allocator = new BudgetAllocator(theme, targetBudget, DEFAULT_BUDGET_TOLERANCE, seed);
        List<RoomTemplate> selectedRooms = allocator.allocateRooms();

        if (selectedRooms.isEmpty()) {
            System.err.println("DungeonGenerator: No rooms selected by budget allocator");
            return null;
        }

        // Phase 2: Room Placement
        System.out.println("\n[Phase 2: Room Placement]");
        RoomPlacer placer = new RoomPlacer(seed);
        List<PlacedRoom> placedRooms = placer.placeRooms(selectedRooms);

        if (placedRooms.isEmpty()) {
            System.err.println("DungeonGenerator: No rooms placed");
            return null;
        }

        // Create result
        DungeonGenerationResult result = new DungeonGenerationResult(
            themeName,
            targetBudget,
            seed,
            placedRooms,
            placer.getUnconnectedDoors()
        );

        System.out.println("\n==============================================");
        System.out.println("DungeonGenerator: Generation complete!");
        System.out.println("  Rooms placed: " + placedRooms.size() + "/" + selectedRooms.size());
        System.out.println("  Unconnected doors: " + placer.getUnconnectedDoors().size());
        System.out.println("  Bounds: " + result.getBounds());
        System.out.println("==============================================");

        return result;
    }

    /**
     * Generate a dungeon with default parameters.
     * @param themeName Theme to use
     * @param floorNumber Floor number (affects budget)
     * @return Generation result
     */
    public static DungeonGenerationResult generateFloor(String themeName, int floorNumber) {
        int targetBudget = 50 + (floorNumber * 25);  // Budget increases per floor
        long seed = System.currentTimeMillis() + floorNumber;
        return generate(themeName, targetBudget, seed);
    }

    /**
     * Generate a dungeon with a specific seed for testing/debugging.
     * @param themeName Theme to use
     * @param targetBudget Target cost budget
     * @param seed Random seed
     * @return Generation result
     */
    public static DungeonGenerationResult generateWithSeed(String themeName, int targetBudget, long seed) {
        return generate(themeName, targetBudget, seed);
    }
}
