package com.game.systems.dungeon.assembly;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.level.LevelData;

import java.util.List;

/**
 * Result of dungeon assembly.
 * Contains the assembled TiledMap, collision data, and spawn information.
 */
public class AssembledDungeon {
    private final TiledMap tiledMap;
    private final LevelData levelData;
    private final List<Rectangle> collisionShapes;
    private final String themeName;
    private final int targetBudget;
    private final long seed;

    public AssembledDungeon(TiledMap tiledMap, LevelData levelData, List<Rectangle> collisionShapes,
                            String themeName, int targetBudget, long seed) {
        this.tiledMap = tiledMap;
        this.levelData = levelData;
        this.collisionShapes = collisionShapes;
        this.themeName = themeName;
        this.targetBudget = targetBudget;
        this.seed = seed;
    }

    /**
     * Get the assembled TiledMap ready for rendering.
     */
    public TiledMap getTiledMap() {
        return tiledMap;
    }

    /**
     * Get the level data containing objects and spawn points.
     */
    public LevelData getLevelData() {
        return levelData;
    }

    /**
     * Get collision shapes for the collision system.
     */
    public List<Rectangle> getCollisionShapes() {
        return collisionShapes;
    }

    public String getThemeName() {
        return themeName;
    }

    public int getTargetBudget() {
        return targetBudget;
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Dispose of resources when done.
     */
    public void dispose() {
        if (tiledMap != null) {
            tiledMap.dispose();
        }
    }

    @Override
    public String toString() {
        return String.format("AssembledDungeon[theme=%s, budget=%d, seed=%d, objects=%d, collision=%d]",
            themeName, targetBudget, seed,
            levelData != null ? levelData.getObjects().size() : 0,
            collisionShapes != null ? collisionShapes.size() : 0);
    }
}
