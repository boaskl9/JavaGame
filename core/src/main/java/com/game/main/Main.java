package com.game.main;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Game;
import com.game.main.GameScreen;

import com.badlogic.gdx.Game;
import com.game.main.GameScreen;

/**
 * Main game class - entry point for libGDX
 * Extends Game to manage screens
 */
public class Main extends Game {

    @Override
    public void create() {
        // This is called when the application is created
        // Set the initial screen to the game screen
        setScreen(new GameScreen());
    }

    @Override
    public void render() {
        // Game class handles calling the current screen's render method
        super.render();
    }

    @Override
    public void dispose() {
        // Clean up resources when the game closes
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
