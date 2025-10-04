package com.game.entity;

import com.badlogic.gdx.graphics.g2d.Sprite;

public interface Entity {

    int posX = 0;
    int posY = 0;

    int health = 50;
    int maxHealth = 50;

    Sprite getSprite();


}
