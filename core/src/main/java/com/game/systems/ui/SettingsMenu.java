package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.game.systems.settings.GameSettings;

/**
 * Settings menu window with various game settings.
 * Currently supports camera scale and UI scale.
 */
public class SettingsMenu extends Window {
    private GameSettings settings;

    private Slider cameraScaleSlider;
    private Label cameraScaleValueLabel;

    private Slider uiScaleSlider;
    private Label uiScaleValueLabel;

    private TextButton applyButton;
    private TextButton resetButton;
    private TextButton closeButton;

    public SettingsMenu(GameSettings settings, Skin skin) {
        super("Settings", skin);
        this.settings = settings;

        // Create UI
        buildUI(skin);

        // Load current settings
        loadCurrentSettings();

        // Configure window
        setMovable(true);
        setModal(true);
        setVisible(false);

        pack();
        centerOnScreen();
    }

    private void buildUI(Skin skin) {
        Table contentTable = new Table(skin);
        contentTable.defaults().pad(5);

        // Title
        Label titleLabel = new Label("Game Settings", skin, "default");
        contentTable.add(titleLabel).colspan(3).center().padBottom(10).row();

        // Camera Scale Setting
        Label cameraScaleLabel = new Label("Camera Scale:", skin);
        cameraScaleSlider = new Slider(
            GameSettings.MIN_CAMERA_SCALE,
            GameSettings.MAX_CAMERA_SCALE,
            0.05f,  // Step size
            false,  // Vertical
            skin,
            "default-horizontal"
        );
        cameraScaleValueLabel = new Label("1.00x", skin);
        cameraScaleValueLabel.setWidth(60);

        cameraScaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("SettingsMenu: Camera slider changed to " + cameraScaleSlider.getValue());
                updateCameraScaleLabel();
            }
        });

        contentTable.add(cameraScaleLabel).left();
        contentTable.add(cameraScaleSlider).width(200).padLeft(10);
        contentTable.add(cameraScaleValueLabel).width(60).left().padLeft(10).row();

        // Description for camera scale
        Label cameraDesc = new Label("(Zoom level of game world)", skin);
        cameraDesc.setFontScale(0.8f);
        contentTable.add().width(1); // Empty cell
        contentTable.add(cameraDesc).colspan(2).left().padBottom(10).row();

        // UI Scale Setting
        Label uiScaleLabel = new Label("UI Scale:", skin);
        uiScaleSlider = new Slider(
            GameSettings.MIN_UI_SCALE,
            GameSettings.MAX_UI_SCALE,
            0.05f,  // Step size
            false,  // Vertical
            skin,
            "default-horizontal"
        );
        uiScaleValueLabel = new Label("1.00x", skin);
        uiScaleValueLabel.setWidth(60);

        uiScaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateUIScaleLabel();
            }
        });

        contentTable.add(uiScaleLabel).left();
        contentTable.add(uiScaleSlider).width(200).padLeft(10);
        contentTable.add(uiScaleValueLabel).width(60).left().padLeft(10).row();

        // Description for UI scale
        Label uiDesc = new Label("(Size of UI elements)", skin);
        uiDesc.setFontScale(0.8f);
        contentTable.add().width(1); // Empty cell
        contentTable.add(uiDesc).colspan(2).left().padBottom(15).row();

        // Buttons
        Table buttonTable = new Table();
        buttonTable.defaults().pad(5).width(100);

        applyButton = new TextButton("Apply", skin);
        applyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("SettingsMenu: Apply button clicked");
                applySettings();
            }
        });

        resetButton = new TextButton("Reset", skin);
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resetSettings();
            }
        });

        closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("SettingsMenu: Close button clicked");
                hide();
            }
        });

        buttonTable.add(applyButton);
        buttonTable.add(resetButton);
        buttonTable.add(closeButton);

        contentTable.add(buttonTable).colspan(3).center().padTop(10).row();

        // Add content to window
        add(contentTable).pad(10);
    }

    /**
     * Loads current settings into the UI.
     */
    private void loadCurrentSettings() {
        cameraScaleSlider.setValue(settings.getCameraScale());
        uiScaleSlider.setValue(settings.getUIScale());
        updateCameraScaleLabel();
        updateUIScaleLabel();
    }

    /**
     * Applies the current slider values to settings.
     */
    private void applySettings() {
        settings.setCameraScale(cameraScaleSlider.getValue());
        settings.setUIScale(uiScaleSlider.getValue());
        settings.save();

        System.out.println("SettingsMenu: Applied settings - Camera: " + settings.getCameraScale() +
                         ", UI: " + settings.getUIScale());
    }

    /**
     * Resets settings to defaults.
     */
    private void resetSettings() {
        settings.resetToDefaults();
        loadCurrentSettings();
        System.out.println("SettingsMenu: Reset to defaults");
    }

    /**
     * Shows the settings menu.
     */
    public void show() {
        setVisible(true);
        toFront();
        centerOnScreen();

        System.out.println("SettingsMenu: Shown at position (" + getX() + ", " + getY() + ")");
        System.out.println("SettingsMenu: Size (" + getWidth() + " x " + getHeight() + ")");
        System.out.println("SettingsMenu: Modal = " + isModal());
        System.out.println("SettingsMenu: Touchable = " + getTouchable());
    }

    /**
     * Hides the settings menu.
     */
    public void hide() {
        setVisible(false);
    }

    /**
     * Toggles the settings menu visibility.
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Centers the window on screen.
     */
    private void centerOnScreen() {
        setPosition(
            (Gdx.graphics.getWidth() - getWidth()) / 2,
            (Gdx.graphics.getHeight() - getHeight()) / 2
        );
    }

    /**
     * Updates the camera scale value label.
     */
    private void updateCameraScaleLabel() {
        cameraScaleValueLabel.setText(String.format("%.2fx", cameraScaleSlider.getValue()));
    }

    /**
     * Updates the UI scale value label.
     */
    private void updateUIScaleLabel() {
        uiScaleValueLabel.setText(String.format("%.2fx", uiScaleSlider.getValue()));
    }

    /**
     * Called when window is resized.
     */
    public void onResize() {
        centerOnScreen();
    }
}
