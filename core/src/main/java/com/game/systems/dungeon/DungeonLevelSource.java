package com.game.systems.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.dungeon.assembly.AssembledDungeon;
import com.game.systems.dungeon.assembly.DungeonAssembler;
import com.game.systems.dungeon.assembly.DungeonPopulator;
import com.game.systems.dungeon.generation.DungeonGenerator;
import com.game.systems.dungeon.generation.DungeonGenerationResult;
import com.game.systems.dungeon.generation.PlacedRoom;
import com.game.systems.level.LevelData;
import com.game.systems.level.LevelSource;

import java.util.List;

/**
 * LevelSource adapter for procedurally generated dungeons.
 * Wraps an AssembledDungeon to provide a common interface with regular Tiled levels.
 */
public class DungeonLevelSource extends LevelSource {
    /** Level IDs of seeded dungeons start with this: "dungeon:<theme>:<day>". */
    public static final String ID_PREFIX = "dungeon:";
    private static final int BUDGET = 50;

    private final AssembledDungeon assembledDungeon;
    private final DungeonGenerationResult generationResult;
    private final String levelId; // Null for one-off dungeons (debug console)
    private final boolean ownsDungeon;

    /**
     * Create a dungeon level source from an assembled dungeon.
     * @param assembledDungeon The assembled dungeon data
     * @param generationResult The generation result (for debug access)
     */
    public DungeonLevelSource(AssembledDungeon assembledDungeon, DungeonGenerationResult generationResult) {
        this(assembledDungeon, generationResult, null, false);
    }

    private DungeonLevelSource(AssembledDungeon assembledDungeon, DungeonGenerationResult generationResult,
                               String levelId, boolean ownsDungeon) {
        this.assembledDungeon = assembledDungeon;
        this.generationResult = generationResult;
        this.levelId = levelId;
        this.ownsDungeon = ownsDungeon;
    }

    /**
     * Build a dungeon that every machine can rebuild identically from its level ID and seed:
     * same rooms, entrance, exit and enemy spots.
     */
    public static DungeonLevelSource generate(String levelId, String themeName, long seed) {
        DungeonGenerationResult result = DungeonGenerator.generateWithSeed(themeName, BUDGET, seed);
        if (result == null) {
            throw new IllegalStateException("Could not generate dungeon " + levelId);
        }
        AssembledDungeon dungeon = DungeonAssembler.assemble(result, DungeonThemeRegistry.getInstance().getTheme(themeName));
        DungeonPopulator.populate(dungeon, result.getPlacedRooms(), seed, enemyTypesFor(themeName));
        return new DungeonLevelSource(dungeon, result, levelId, true);
    }

    /** Which enemies live in a theme's dungeons. */
    private static List<String> enemyTypesFor(String themeName) {
        return switch (themeName) {
            case "cave" -> List.of("enemy:lizard", "enemy:axolot");
            default -> List.of("enemy:lizard", "enemy:axolot", "enemy:cat");
        };
    }

    @Override
    public TiledMap getTiledMap() {
        return assembledDungeon.getTiledMap();
    }

    @Override
    public LevelData getLevelData() {
        return assembledDungeon.getLevelData();
    }

    @Override
    public void loadCollision(SpatialQuery collisionSystem) {
        // Load pre-assembled collision shapes from dungeon
        for (Rectangle shape : assembledDungeon.getCollisionShapes()) {
            collisionSystem.addRectangle(shape);
        }
        System.out.println("DungeonLevelSource: Loaded " + assembledDungeon.getCollisionShapes().size() + " collision shapes");
    }

    @Override
    public void dispose() {
        // One-off dungeons are cached by DungeonController, which disposes them
        if (ownsDungeon) {
            assembledDungeon.dispose();
        }
    }

    @Override
    public String getLevelName() {
        if (levelId != null) return levelId;
        return "Dungeon [" + assembledDungeon.getThemeName() + ", seed=" + assembledDungeon.getSeed() + "]";
    }

    @Override
    public boolean isDungeon() {
        return true;
    }

    @Override
    public boolean isShareable() {
        return levelId != null;
    }

    /**
     * Get the underlying assembled dungeon (for direct access).
     * @return The assembled dungeon
     */
    public AssembledDungeon getAssembledDungeon() {
        return assembledDungeon;
    }

    /**
     * Get placed rooms for debug visualization.
     * @return List of placed rooms
     */
    public List<PlacedRoom> getPlacedRooms() {
        return generationResult != null ? generationResult.getPlacedRooms() : null;
    }

    /**
     * Get the offset for debug rendering.
     * @return [offsetX, offsetY] in pixels
     */
    public float[] getDebugOffset() {
        return new float[] {
            assembledDungeon.getOffsetX() * 16f,  // Convert tiles to pixels
            assembledDungeon.getOffsetY() * 16f
        };
    }
}
