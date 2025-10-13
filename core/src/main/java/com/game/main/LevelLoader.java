package com.game.main;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.game.entity.Entity;
import com.game.entity.Gateway;
import com.game.world.*;

import java.util.ArrayList;
import java.util.List;

public class LevelLoader {
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    public OrthogonalTiledMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    float[] playerSpawn;
    private java.util.Map<String, float[]> spawnPoints = new java.util.HashMap<>();

    public List<Entity> loadLevel(String levelPath) {
        map = new TmxMapLoader().load(levelPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        // Clear previous spawn points
        spawnPoints.clear();
        playerSpawn = null;

        // Parse entity spawns (only if the layer exists)
        MapLayer objectLayer = map.getLayers().get("Entities");
        if (objectLayer != null) {
            System.out.println("found entities");

            List<Entity> entityList = new ArrayList<>();

            for (MapObject object : objectLayer.getObjects()) {
                if (object.getName() == null) {continue;}

                String objectName = object.getName();
                float x = object.getProperties().get("x", float.class);
                float y = object.getProperties().get("y", float.class);

                if (objectName.equals("player_spawn")) {
                    // Store default spawn point
                    playerSpawn = new float[]{x, y};
                }
                else if (objectName.startsWith("spawn_")) {
                    // Store named spawn points (e.g., spawn_north, spawn_south)
                    spawnPoints.put(objectName, new float[]{x, y});
                }
                else if (objectName.equals("gateway")) {
                    // Get gateway properties
                    float width = object.getProperties().get("width", float.class);
                    float height = object.getProperties().get("height", float.class);

                    // Target level property (required)
                    String targetLevel = object.getProperties().get("targetLevel", String.class);

                    // Target spawn point property (optional)
                    String targetSpawn = object.getProperties().get("targetSpawn", String.class);

                    if (targetLevel != null) {
                        Gateway gateway = new Gateway(x, y, width, height, targetLevel, targetSpawn);
                        entityList.add(gateway);
                        System.out.println("Loaded gateway to: " + targetLevel + " at spawn: " + targetSpawn);
                    } else {
                        System.err.println("Gateway at (" + x + ", " + y + ") missing targetLevel property!");
                    }
                }
            }

            return entityList;
        }
        else {
            return new ArrayList<>();
        }
    }

    /**
     * Load collision shapes from tile collision editor into the collision system
     */
    public void loadCollisionData(GameWorld world) {
        CollisionSystem collisionSystem = new CollisionSystem();
        collisionSystem.loadCollisionShapes(map);
        world.setCollisionSystem(collisionSystem);
    }

    public void render(OrthographicCamera camera) {
        mapRenderer.setView(camera);
        mapRenderer.render();
    }

    public void dispose() {
        if (map != null) {
            map.dispose();
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
    }

    public int getMapWidth() {
        if (map != null) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
            return layer.getWidth();
        }
        return 80;
    }

    public int getMapHeight() {
        if (map != null) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
            return layer.getHeight();
        }
        return 60;
    }

    public float[] getPlayerSpawnPosition() {
        if (playerSpawn != null) {
            return playerSpawn;
        }
        return new float[]{50, 750};
    }

    /**
     * Get a specific named spawn point
     * @param spawnName Name of the spawn point (e.g., "spawn_north")
     * @return The spawn position, or default player spawn if not found
     */
    public float[] getSpawnPosition(String spawnName) {
        if (spawnName != null && spawnPoints.containsKey(spawnName)) {
            return spawnPoints.get(spawnName);
        }
        return getPlayerSpawnPosition();
    }
}
