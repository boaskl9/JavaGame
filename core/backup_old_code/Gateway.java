package com.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Gateway entity that triggers level transitions when the player collides with it
 */
public class Gateway implements Entity {
    private Vector2 position;
    private float width;
    private float height;
    private String targetLevel;
    private String spawnPointName; // Optional: specific spawn point in the target level

    public Gateway(float x, float y, float width, float height, String targetLevel, String spawnPointName) {
        this.position = new Vector2(x, y);
        this.width = width;
        this.height = height;
        this.targetLevel = targetLevel;
        this.spawnPointName = spawnPointName;
    }

    public Gateway(float x, float y, float width, float height, String targetLevel) {
        this(x, y, width, height, targetLevel, null);
    }

    @Override
    public void update(float delta) {
        // Gateways don't need to update
    }

    @Override
    public void render(SpriteBatch batch) {
        // Gateways are invisible, so we don't render anything
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    @Override
    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, width, height);
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getSpawnPointName() {
        return spawnPointName;
    }

    public boolean contains(float x, float y) {
        return getBounds().contains(x, y);
    }

    public boolean overlaps(Rectangle rect) {
        return getBounds().overlaps(rect);
    }
}