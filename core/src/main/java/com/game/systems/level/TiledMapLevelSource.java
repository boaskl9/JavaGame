package com.game.systems.level;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.collision.TiledMapCollisionLoader;

/**
 * LevelSource implementation for regular Tiled (.tmx) map files.
 * Loads maps from the file system and extracts level data.
 */
public class TiledMapLevelSource extends LevelSource {
    private final String levelPath;
    private TiledMap tiledMap;
    private LevelData levelData;

    /**
     * Create a level source from a Tiled map file.
     * @param levelPath Path to the .tmx file (e.g., "maps/village.tmx")
     */
    public TiledMapLevelSource(String levelPath) {
        this.levelPath = levelPath;
        load();
    }

    /**
     * Load the Tiled map and parse level data.
     */
    private void load() {
        System.out.println("TiledMapLevelSource: Loading " + levelPath);

        // Load Tiled map from file
        tiledMap = new TmxMapLoader().load(levelPath);

        // Parse level data (spawn points, objects, etc.)
        levelData = TiledMapParser.parse(tiledMap);

        System.out.println("TiledMapLevelSource: Loaded " + levelPath +
            " (" + levelData.getWidth() + "x" + levelData.getHeight() + " tiles)");
    }

    @Override
    public TiledMap getTiledMap() {
        return tiledMap;
    }

    @Override
    public LevelData getLevelData() {
        return levelData;
    }

    @Override
    public void loadCollision(SpatialQuery collisionSystem) {
        TiledMapCollisionLoader.loadFromTiledMap(tiledMap, collisionSystem);
        System.out.println("TiledMapLevelSource: Loaded " + collisionSystem.getShapeCount() + " collision shapes");
    }

    @Override
    public void dispose() {
        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }
    }

    @Override
    public String getLevelName() {
        return levelPath;
    }

    @Override
    public boolean isDungeon() {
        return false;
    }
}
