package com.game.entity;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public interface Entity {

    float posX = 0;
    float posY = 0;

    int health = 50;
    int maxHealth = 50;

    void update(float delta);
    void render(SpriteBatch batch);
    Vector2 getPosition();
    void setPosition(float x, float y);
}
