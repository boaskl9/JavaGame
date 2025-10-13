package com.game.systems.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure data structure representing a loaded level.
 * No dependencies on rendering, entities, or game logic.
 * Can be serialized/deserialized from various formats.
 */
public class LevelData {
    private int width;
    private int height;
    private int tileSize;

    private Map<String, SpawnPoint> spawnPoints;
    private List<LevelObject> objects;

    public LevelData(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.spawnPoints = new HashMap<>();
        this.objects = new ArrayList<>();
    }

    public void addSpawnPoint(String name, float x, float y) {
        spawnPoints.put(name, new SpawnPoint(name, x, y));
    }

    public void addObject(LevelObject object) {
        objects.add(object);
    }

    public SpawnPoint getSpawnPoint(String name) {
        return spawnPoints.get(name);
    }

    public SpawnPoint getDefaultSpawnPoint() {
        return spawnPoints.get("player_spawn");
    }

    public List<LevelObject> getObjects() {
        return new ArrayList<>(objects);
    }

    public List<LevelObject> getObjectsByType(String type) {
        List<LevelObject> filtered = new ArrayList<>();
        for (LevelObject obj : objects) {
            if (obj.getType().equals(type)) {
                filtered.add(obj);
            }
        }
        return filtered;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileSize() {
        return tileSize;
    }

    /**
     * Represents a spawn point in the level.
     */
    public static class SpawnPoint {
        private String name;
        private float x;
        private float y;

        public SpawnPoint(String name, float x, float y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public String getName() { return name; }
        public float getX() { return x; }
        public float getY() { return y; }
    }

    /**
     * Represents a generic object in the level (entity spawn, gateway, item, etc.)
     */
    public static class LevelObject {
        private String type;
        private String name;
        private float x;
        private float y;
        private float width;
        private float height;
        private Map<String, Object> properties;

        public LevelObject(String type, String name, float x, float y) {
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = 0;
            this.height = 0;
            this.properties = new HashMap<>();
        }

        public void setSize(float width, float height) {
            this.width = width;
            this.height = height;
        }

        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }

        public Object getProperty(String key) {
            return properties.get(key);
        }

        public String getPropertyString(String key, String defaultValue) {
            Object value = properties.get(key);
            return value != null ? value.toString() : defaultValue;
        }

        public float getPropertyFloat(String key, float defaultValue) {
            Object value = properties.get(key);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return defaultValue;
        }
    }
}
