package com.game.main;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.game.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the grid-based game world and entities within it
 */
public class GameWorld {
    private final int TILE_SIZE = 32; // pixels per tile
    private final int worldWidth;  // in tiles
    private final int worldHeight; // in tiles

    private int[][] tiles; // 0 = walkable, 1 = blocked
    private List<Entity> entities;

    public GameWorld(int width, int height) {
        this.worldWidth = width;
        this.worldHeight = height;
        this.tiles = new int[width][height];
        this.entities = new ArrayList<>();

        // Initialize with all walkable tiles
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = 0;
            }
        }

        // Add some walls around the edges as an example
        for (int x = 0; x < width; x++) {
            tiles[x][0] = 1;
            tiles[x][height - 1] = 1;
        }
        for (int y = 0; y < height; y++) {
            tiles[0][y] = 1;
            tiles[width - 1][y] = 1;
        }
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public boolean isWalkable(int gridX, int gridY) {
        if (gridX < 0 || gridX >= worldWidth || gridY < 0 || gridY >= worldHeight) {
            return false;
        }
        return tiles[gridX][gridY] == 0;
    }

    public void setTile(int gridX, int gridY, int value) {
        if (gridX >= 0 && gridX < worldWidth && gridY >= 0 && gridY < worldHeight) {
            tiles[gridX][gridY] = value;
        }
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

    public void render(SpriteBatch batch) {
        // Render tiles (you'd add actual tile textures here)
        // For now, entities handle their own rendering

        for (Entity entity : entities) {
            entity.render(batch);
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
}
