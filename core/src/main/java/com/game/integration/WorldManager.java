package com.game.integration;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

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

    public WorldManager(int width, int height) {
        this.worldWidth = width;
        this.worldHeight = height;
        this.gameObjects = new ArrayList<>();
        this.collisionSystem = new SpatialQuery();
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
     */
    public boolean isPositionWalkable(float x, float y, float width, float height) {
        return !collisionSystem.testArea(x, y, width, height);
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
}
