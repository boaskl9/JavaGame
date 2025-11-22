package com.game.integration;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.pathfinding.GridPathfinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration layer that manages the game world and all its systems.
 * This is the glue that connects all standalone systems together.
 */
public class WorldManager {
    private final int TILE_SIZE = 16;
    private final int worldWidth;
    private final int worldHeight;

    private List<GameObject> gameObjects;
    private SpatialQuery collisionSystem;
    private GridPathfinder gridPathfinder;
    private com.badlogic.gdx.maps.tiled.TiledMap tiledMap;

    public WorldManager(int width, int height) {
        this.worldWidth = width;
        this.worldHeight = height;
        this.gameObjects = new ArrayList<>();
        this.collisionSystem = new SpatialQuery();
    }

    /**
     * Builds the grid pathfinder for pathfinding.
     * Should be called after collision system is set up and TiledMap is loaded.
     */
    public void buildGridPathfinder(com.badlogic.gdx.maps.tiled.TiledMap tiledMap) {
        this.tiledMap = tiledMap;
        float worldWidthPixels = worldWidth * TILE_SIZE;
        float worldHeightPixels = worldHeight * TILE_SIZE;

        // Grid cell size - 8x8 pixels for very fine-grained pathfinding
        int cellSize = 8;

        this.gridPathfinder = new GridPathfinder(worldWidthPixels, worldHeightPixels, cellSize);
        this.gridPathfinder.buildFromTiledMap(tiledMap, collisionSystem);
    }


    public void addGameObject(GameObject obj) {
        gameObjects.add(obj);
    }

    public void removeGameObject(GameObject obj) {
        gameObjects.remove(obj);
    }

    /**
     * Update all game objects.
     */
    public void update(float delta) {
        for (GameObject obj : gameObjects) {
            obj.update(delta);
        }
    }

    /**
     * Render all game objects.
     */
    public void render(SpriteBatch batch) {
        for (GameObject obj : gameObjects) {
            RenderComponent renderComp = obj.getComponent(RenderComponent.class);
            if (renderComp != null) {
                renderComp.render(batch, obj);
            }
        }
    }

    /**
     * Check if a position is walkable (no collision).
     * Checks both static collision (walls from TiledMap) and dynamic objects (breakables).
     */
    public boolean isPositionWalkable(float x, float y, float width, float height) {
        // Check static collision (walls, obstacles)
        if (collisionSystem.testArea(x, y, width, height)) {
            return false;
        }

        // Check dynamic collision (breakable objects and furniture)
        Rectangle testRect = new Rectangle(x, y, width, height);
        for (GameObject obj : gameObjects) {
            // Check if it's a breakable object
            if (obj instanceof com.game.systems.entity.entities.BreakableEntity) {
                com.game.systems.entity.entities.BreakableEntity breakable =
                    (com.game.systems.entity.entities.BreakableEntity) obj;

                // Skip inactive (destroyed) breakables
                if (!breakable.isActive()) continue;

                // Check collision with environment collider (feet area)
                ColliderComponent envCollider = breakable.getEnvironmentCollider();
                if (envCollider != null) {
                    Rectangle breakableBounds = envCollider.getBounds(breakable);
                    if (breakableBounds.overlaps(testRect)) {
                        return false; // Collision detected
                    }
                }
            }
            // Check if it's furniture
            else if (obj instanceof com.game.systems.furniture.FurnitureEntity) {
                com.game.systems.furniture.FurnitureEntity furniture =
                    (com.game.systems.furniture.FurnitureEntity) obj;

                // Check collision with furniture collider
                ColliderComponent furnitureCollider = furniture.getCollider();
                if (furnitureCollider != null) {
                    Rectangle furnitureBounds = furnitureCollider.getBounds(furniture);
                    if (furnitureBounds.overlaps(testRect)) {
                        return false; // Collision detected
                    }
                }
            }
        }

        return true; // No collision
    }

    /**
     * Check if a rectangle is walkable.
     */
    public boolean isRectangleWalkable(Rectangle rect) {
        return !collisionSystem.testRectangle(rect);
    }

    /**
     * Find a game object at the given position with a specific component type.
     */
    @SuppressWarnings("unchecked")
    public <T extends com.game.systems.entity.Component> GameObject findObjectAt(float x, float y, float width, float height, Class<T> componentType) {
        Rectangle testRect = new Rectangle(x, y, width, height);

        for (GameObject obj : gameObjects) {
            if (!obj.hasComponent(componentType)) continue;

            ColliderComponent collider = obj.getComponent(ColliderComponent.class);
            if (collider != null) {
                Rectangle bounds = collider.getBounds(obj);
                if (bounds.overlaps(testRect)) {
                    return obj;
                }
            }
        }

        return null;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public SpatialQuery getCollisionSystem() {
        return collisionSystem;
    }

    public void setCollisionSystem(SpatialQuery collisionSystem) {
        this.collisionSystem = collisionSystem;
    }

    public List<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }

    public GridPathfinder getGridPathfinder() {
        return gridPathfinder;
    }
}
