package com.game.main;

import com.badlogic.gdx.Game;


/**
 * Main game class - entry point for libGDX
 * Extends Game to manage screens
 */
public class Main extends Game {

    @Override
    public void create() {
        // This is called when the application is created
        // Set the initial screen to the game screen
        // Using GameScreenNew with refactored decoupled architecture
        setScreen(new GameScreenNew());
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
