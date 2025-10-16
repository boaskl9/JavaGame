package com.game.systems.level;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.game.systems.level.LevelData.LevelObject;

/**
 * Parser for extracting level data from Tiled maps.
 * Separates the parsing logic from the data structure.
 */
public class TiledMapParser {

    /**
     * Parse a Tiled map into a generic LevelData structure.
     * @param map The tiled map to parse
     * @return Pure data representation of the level
     */
    public static LevelData parse(TiledMap map) {
        // Get dimensions from first tile layer
        TiledMapTileLayer firstLayer = getFirstTileLayer(map);
        if (firstLayer == null) {
            throw new RuntimeException("No tile layer found in map!");
        }

        int width = firstLayer.getWidth();
        int height = firstLayer.getHeight();
        int tileSize = (int) firstLayer.getTileWidth();

        LevelData levelData = new LevelData(width, height, tileSize);

        // Parse entity/object layer
        MapLayer objectLayer = map.getLayers().get("Entities");
        if (objectLayer != null) {
            parseObjects(objectLayer, levelData);
        }

        return levelData;
    }

    /**
     * Parse objects from the Entities layer.
     */
    private static void parseObjects(MapLayer layer, LevelData levelData) {
        for (MapObject object : layer.getObjects()) {
            if (object.getName() == null) continue;

            String objectName = object.getName();
            float x = object.getProperties().get("x", Float.class);
            float y = object.getProperties().get("y", Float.class);

            // Handle spawn points
            if (objectName.equals("player_spawn") || objectName.startsWith("spawn_")) {
                levelData.addSpawnPoint(objectName, x, y);
            }
            // Handle other objects
            else {
                LevelObject levelObject = new LevelObject(objectName, objectName, x, y);

                // Copy width/height if they exist
                if (object.getProperties().containsKey("width")) {
                    float width = object.getProperties().get("width", Float.class);
                    float height = object.getProperties().get("height", Float.class);
                    levelObject.setSize(width, height);
                }

                // Copy all custom properties
                java.util.Iterator<String> keys = object.getProperties().getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!key.equals("x") && !key.equals("y") && !key.equals("width") && !key.equals("height")) {
                        levelObject.setProperty(key, object.getProperties().get(key));
                    }
                }

                levelData.addObject(levelObject);
            }
        }
    }

    /**
     * Get the first tile layer from the map.
     */
    private static TiledMapTileLayer getFirstTileLayer(TiledMap map) {
        for (int i = 0; i < map.getLayers().getCount(); i++) {
            if (map.getLayers().get(i) instanceof TiledMapTileLayer) {
                return (TiledMapTileLayer) map.getLayers().get(i);
            }
        }
        return null;
    }
}
