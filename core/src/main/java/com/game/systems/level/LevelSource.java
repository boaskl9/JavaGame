package com.game.systems.level;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.game.systems.collision.SpatialQuery;

/**
 * Abstract base class for all level sources (regular Tiled maps, procedural dungeons, etc.).
 * Provides a common interface for loading levels into GameScreen.
 */
public abstract class LevelSource {

    /**
     * Get the TiledMap for rendering.
     * @return The loaded or generated TiledMap
     */
    public abstract TiledMap getTiledMap();

    /**
     * Get the level data containing spawn points and objects.
     * @return LevelData for this level
     */
    public abstract LevelData getLevelData();

    /**
     * Load collision shapes into the provided collision system.
     * @param collisionSystem The SpatialQuery to populate with collision shapes
     */
    public abstract void loadCollision(SpatialQuery collisionSystem);

    /**
     * Dispose of resources when done with this level.
     * Should clean up TiledMap and any other resources.
     */
    public abstract void dispose();

    /**
     * Get a descriptive name for this level (for debugging/logging).
     * @return Level name or identifier
     */
    public abstract String getLevelName();

    /**
     * Check if this level source is for a dungeon (affects debug rendering).
     * @return true if this is a dungeon, false for regular levels
     */
    public abstract boolean isDungeon();
}
