package com.game.systems.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.dungeon.assembly.AssembledDungeon;
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
    private final AssembledDungeon assembledDungeon;
    private final DungeonGenerationResult generationResult;

    /**
     * Create a dungeon level source from an assembled dungeon.
     * @param assembledDungeon The assembled dungeon data
     * @param generationResult The generation result (for debug access)
     */
    public DungeonLevelSource(AssembledDungeon assembledDungeon, DungeonGenerationResult generationResult) {
        this.assembledDungeon = assembledDungeon;
        this.generationResult = generationResult;
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
        // Don't dispose here - DungeonController manages disposal
        // This allows the dungeon to be cached and reused
    }

    @Override
    public String getLevelName() {
        return "Dungeon [" + assembledDungeon.getThemeName() + ", seed=" + assembledDungeon.getSeed() + "]";
    }

    @Override
    public boolean isDungeon() {
        return true;
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
