package com.game.main;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.game.world.*;

public class LevelLoader {
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    public OrthogonalTiledMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    float[] playerSpawn;

    public void loadLevel(String levelPath) {
        map = new TmxMapLoader().load(levelPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        // Parse entity spawns (only if the layer exists)
        MapLayer objectLayer = map.getLayers().get("Entities");
        if (objectLayer != null) {
            System.out.println("found entities");
            for (MapObject object : objectLayer.getObjects()) {
                if (object.getName() != null && object.getName().equals("player_spawn")) {
                    // Spawn player at object position
                    playerSpawn = new float[]{object.getProperties().get("x", float.class), object.getProperties().get("y", float.class)};
                }
            }
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
}
