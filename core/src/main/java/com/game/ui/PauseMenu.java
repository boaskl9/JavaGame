package com.game.ui;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Pause menu overlay with Resume, Settings, and Return to Main Menu options.
 */
public class PauseMenu extends Window {
    private boolean visible = false;
    private PauseMenuCallback callback;

    public interface PauseMenuCallback {
        void onResume();
        void onSettings();
        void onOpenToLAN();
        void onReturnToMainMenu();
    }

    public PauseMenu(Skin skin) {
        super("Paused", skin);

        this.getTitleLabel().setAlignment(0);

        this.padTop(20);

        setModal(true);
        setMovable(false);

        Table content = new Table();
        content.pad(20);
        content.defaults().width(250).height(50).padBottom(15);

        // Resume button
        TextButton resumeButton = new TextButton("Resume", skin);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onResume();
                }
            }
        });
        content.add(resumeButton).row();

        // Settings button
        TextButton settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onSettings();
                }
            }
        });
        content.add(settingsButton).row();

        // Open to LAN button
        TextButton lanButton = new TextButton("Open to LAN", skin);
        lanButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onOpenToLAN();
                }
            }
        });
        content.add(lanButton).row();

        // Return to Main Menu button
        TextButton mainMenuButton = new TextButton("Return to Main Menu", skin);
        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (callback != null) {
                    callback.onReturnToMainMenu();
                }
            }
        });
        content.add(mainMenuButton);

        add(content);
        pack();

        setVisible(false);
    }

    public void setCallback(PauseMenuCallback callback) {
        this.callback = callback;
    }

    /**
     * Toggle pause menu visibility.
     */
    public void toggle() {
        visible = !visible;
        setVisible(visible);

        if (visible) {
            // Center the pause menu
            if (getStage() != null) {
                setPosition(
                    (getStage().getWidth() - getWidth()) / 2,
                    (getStage().getHeight() - getHeight()) / 2
                );
                toFront();
            }
        }
    }

    /**
     * Show the pause menu.
     */
    public void show() {
        visible = true;
        setVisible(true);

        if (getStage() != null) {
            setPosition(
                (getStage().getWidth() - getWidth()) / 2,
                (getStage().getHeight() - getHeight()) / 2
            );
            toFront();
        }
    }

    /**
     * Hide the pause menu.
     */
    public void hide() {
        visible = false;
        setVisible(false);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
