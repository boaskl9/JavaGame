package com.game.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.entity.Entity;
import com.game.entity.Gateway;

import java.util.ArrayList;
import java.util.List;

public class GameWorld {
    private final int TILE_SIZE = 16; // Changed to 16 to match Tiled
    private final int worldWidth;
    private final int worldHeight;

    private List<Entity> entities;
    private CollisionSystem collisionSystem;

    public GameWorld(int width, int height) {
        this.worldWidth = width;
        this.worldHeight = height;
        this.entities = new ArrayList<>();
        this.collisionSystem = new CollisionSystem();
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    /**
     * Check if a position is walkable using pixel-perfect collision
     */
    public boolean isPositionWalkable(float worldX, float worldY, float width, float height) {
        return !collisionSystem.isAreaBlocked(worldX, worldY, width, height);
    }

    /**
     * Check if a position overlaps with any gateway and return it
     */
    public Gateway getGatewayAtPosition(float worldX, float worldY, float width, float height) {
        Rectangle playerRect = new Rectangle(worldX, worldY, width, height);

        for (Entity entity : entities) {
            if (entity instanceof Gateway) {
                Gateway gateway = (Gateway) entity;
                if (gateway.overlaps(playerRect)) {
                    return gateway;
                }
            }
        }

        return null;
    }

    /**
     * Check if a rectangle collides with anything
     */
    public boolean isRectangleWalkable(Rectangle rect) {
        return !collisionSystem.isRectangleBlocked(rect);
    }

    public Vector2 gridToWorld(int gridX, int gridY) {
        return new Vector2(gridX * TILE_SIZE, gridY * TILE_SIZE);
    }

    public Vector2 worldToGrid(float worldX, float worldY) {
        return new Vector2((int)(worldX / TILE_SIZE), (int)(worldY / TILE_SIZE));
    }

    public void update(float delta) {
        for (Entity entity : entities) {
            entity.update(delta);
        }
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

    public CollisionSystem getCollisionSystem() {
        return collisionSystem;
    }

    public void setCollisionSystem(CollisionSystem collisionSystem) {
        this.collisionSystem = collisionSystem;
    }

    public List<Entity> getEntities() {
        return entities;
    }
}
