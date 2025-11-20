package com.game.systems.dungeon;

import com.game.systems.dungeon.assembly.AssembledDungeon;
import com.game.systems.dungeon.assembly.DungeonAssembler;
import com.game.systems.dungeon.generation.DungeonGenerationResult;
import com.game.systems.dungeon.generation.DungeonGenerator;
import com.game.systems.dungeon.generation.PlacedRoom;
import com.game.systems.level.LevelSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central controller for dungeon generation and management.
 * Handles dungeon generation, caching, and provides access to dungeon data for rendering and gameplay.
 */
public class DungeonController {
    private AssembledDungeon currentDungeon;
    private DungeonGenerationResult currentGenerationResult;
    private final Map<Long, AssembledDungeon> dungeonCache;
    private static final int MAX_CACHE_SIZE = 5;

    public DungeonController() {
        this.dungeonCache = new HashMap<>();
    }

    /**
     * Generate a new dungeon with specified parameters.
     * @param themeName Theme name (e.g., "forest", "cave")
     * @param budget Budget for room placement
     * @param seed Random seed for generation
     * @param maxRooms Maximum number of rooms
     * @return The assembled dungeon
     */
    public AssembledDungeon generateDungeon(String themeName, int budget, long seed, int maxRooms) {
        System.out.println("=== Generating Dungeon ===");
        System.out.println("Theme: " + themeName + ", Budget: " + budget + ", Seed: " + seed);

        // Check cache first
        if (dungeonCache.containsKey(seed)) {
            System.out.println("DungeonController: Using cached dungeon for seed " + seed);
            currentDungeon = dungeonCache.get(seed);
            return currentDungeon;
        }

        // Load theme (will load from file if not cached)
        DungeonTheme theme = DungeonThemeRegistry.getInstance().loadTheme(themeName);
        if (theme == null) {
            throw new IllegalArgumentException("Unknown theme: " + themeName);
        }

        // Generate dungeon
        currentGenerationResult = DungeonGenerator.generateWithSeed(themeName, budget, seed);

        if (currentGenerationResult == null) {
            throw new RuntimeException("Dungeon generation failed!");
        }

        // Assemble dungeon
        currentDungeon = DungeonAssembler.assemble(currentGenerationResult, theme);

        // Cache the dungeon
        cacheDungeon(seed, currentDungeon);

        System.out.println("=== Dungeon Generation Complete ===");
        return currentDungeon;
    }

    /**
     * Generate a dungeon with auto-generated seed.
     * @param themeName Theme name
     * @param budget Budget for room placement
     * @param maxRooms Maximum number of rooms
     * @return The assembled dungeon
     */
    public AssembledDungeon generateDungeon(String themeName, int budget, int maxRooms) {
        long seed = System.currentTimeMillis();
        return generateDungeon(themeName, budget, seed, maxRooms);
    }

    /**
     * Create a LevelSource for the current dungeon.
     * Used to integrate with GameScreen's unified level loading.
     * @return DungeonLevelSource wrapping the current dungeon
     */
    public LevelSource createLevelSource() {
        if (currentDungeon == null) {
            throw new IllegalStateException("No dungeon generated! Call generateDungeon() first.");
        }
        return new DungeonLevelSource(currentDungeon, currentGenerationResult);
    }

    /**
     * Get the current assembled dungeon (for direct access).
     * @return Current dungeon or null if none generated
     */
    public AssembledDungeon getCurrentDungeon() {
        return currentDungeon;
    }

    /**
     * Get the generation result for debug visualization.
     * @return Generation result with placed rooms, or null if none
     */
    public DungeonGenerationResult getGenerationResult() {
        return currentGenerationResult;
    }

    /**
     * Get placed rooms for debug rendering.
     * @return List of placed rooms, or null if none
     */
    public List<PlacedRoom> getPlacedRooms() {
        return currentGenerationResult != null ? currentGenerationResult.getPlacedRooms() : null;
    }

    /**
     * Get the offset used for positioning (for debug rendering).
     * @return Offset in pixels [offsetX, offsetY], or [0, 0] if no dungeon
     */
    public float[] getOffset() {
        if (currentDungeon != null) {
            return new float[] {
                currentDungeon.getOffsetX() * 16f,  // Convert tiles to pixels
                currentDungeon.getOffsetY() * 16f
            };
        }
        return new float[] {0, 0};
    }

    /**
     * Cache a generated dungeon by its seed.
     */
    private void cacheDungeon(long seed, AssembledDungeon dungeon) {
        // Simple cache eviction: if over limit, clear oldest (not LRU, but simple)
        if (dungeonCache.size() >= MAX_CACHE_SIZE) {
            // Remove first entry (arbitrary eviction)
            Long firstKey = dungeonCache.keySet().iterator().next();
            AssembledDungeon old = dungeonCache.remove(firstKey);
            if (old != null) {
                old.dispose();
            }
        }

        dungeonCache.put(seed, dungeon);
    }

    /**
     * Clear the dungeon cache and dispose of all cached dungeons.
     */
    public void clearCache() {
        for (AssembledDungeon dungeon : dungeonCache.values()) {
            if (dungeon != null) {
                dungeon.dispose();
            }
        }
        dungeonCache.clear();
        System.out.println("DungeonController: Cache cleared");
    }

    /**
     * Dispose of all resources.
     */
    public void dispose() {
        clearCache();
        if (currentDungeon != null && !dungeonCache.containsValue(currentDungeon)) {
            currentDungeon.dispose();
        }
        currentDungeon = null;
        currentGenerationResult = null;
    }
}
