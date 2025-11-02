package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.game.systems.audio.SoundSystem;
import com.game.systems.settings.GameSettings;

import static com.game.systems.audio.SoundRegistry.*;

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

    private Slider masterVolumeSlider;
    private Label masterVolumeValueLabel;

    private Slider musicVolumeSlider;
    private Label musicVolumeValueLabel;

    private Slider sfxVolumeSlider;
    private Label sfxVolumeValueLabel;

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

        // Audio Settings Section
        Label audioHeader = new Label("Audio Settings", skin, "default");
        contentTable.add(audioHeader).colspan(3).center().padTop(10).padBottom(10).row();

        // Master Volume Setting
        Label masterVolumeLabel = new Label("Master Volume:", skin);
        masterVolumeSlider = new Slider(0.0f, 1.0f, 0.01f, false, skin, "default-horizontal");
        masterVolumeValueLabel = new Label("100%", skin);
        masterVolumeValueLabel.setWidth(60);

        masterVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateMasterVolumeLabel();
                // Apply volume immediately for real-time feedback
                SoundSystem.getInstance().setMasterVolume(masterVolumeSlider.getValue());

            }
        });

        contentTable.add(masterVolumeLabel).left();
        contentTable.add(masterVolumeSlider).width(200).padLeft(10);
        contentTable.add(masterVolumeValueLabel).width(60).left().padLeft(10).row();

        // Music Volume Setting
        Label musicVolumeLabel = new Label("Music Volume:", skin);
        musicVolumeSlider = new Slider(0.0f, 1.0f, 0.01f, false, skin, "default-horizontal");
        musicVolumeValueLabel = new Label("100%", skin);
        musicVolumeValueLabel.setWidth(60);

        musicVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateMusicVolumeLabel();
                // Apply volume immediately for real-time feedback
                SoundSystem.getInstance().setMusicVolume(musicVolumeSlider.getValue());
            }
        });

        contentTable.add(musicVolumeLabel).left();
        contentTable.add(musicVolumeSlider).width(200).padLeft(10);
        contentTable.add(musicVolumeValueLabel).width(60).left().padLeft(10).row();

        // SFX Volume Setting
        Label sfxVolumeLabel = new Label("SFX Volume:", skin);
        sfxVolumeSlider = new Slider(0.0f, 1.0f, 0.01f, false, skin, "default-horizontal");
        sfxVolumeValueLabel = new Label("100%", skin);
        sfxVolumeValueLabel.setWidth(60);

        sfxVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateSfxVolumeLabel();
                // Apply volume immediately for real-time feedback
                SoundSystem.getInstance().setSfxVolume(sfxVolumeSlider.getValue());
            }
        });

        contentTable.add(sfxVolumeLabel).left();
        contentTable.add(sfxVolumeSlider).width(200).padLeft(10);
        contentTable.add(sfxVolumeValueLabel).width(60).left().padLeft(10).row();

        // Buttons
        Table buttonTable = new Table();
        buttonTable.defaults().pad(5).width(100);

        applyButton = new TextButton("Apply", skin);
        applyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Play button click sound
                SoundSystem.getInstance().playSound(
                    BUTTON_CLICK,
                    0.5f
                );


                System.out.println("SettingsMenu: Apply button clicked");
                applySettings();
            }
        });

        resetButton = new TextButton("Reset", skin);
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Play button click sound
                SoundSystem.getInstance().playSound(
                    BUTTON_CLICK,
                    0.5f
                );

                resetSettings();
            }
        });

        closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Play button click sound
                SoundSystem.getInstance().playSound(
                    BUTTON_CLICK,
                    0.5f
                );


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
        masterVolumeSlider.setValue(settings.getMasterVolume());
        musicVolumeSlider.setValue(settings.getMusicVolume());
        sfxVolumeSlider.setValue(settings.getSfxVolume());
        updateCameraScaleLabel();
        updateUIScaleLabel();
        updateMasterVolumeLabel();
        updateMusicVolumeLabel();
        updateSfxVolumeLabel();
    }

    /**
     * Applies the current slider values to settings.
     */
    private void applySettings() {
        settings.setCameraScale(cameraScaleSlider.getValue());
        settings.setUIScale(uiScaleSlider.getValue());
        settings.setMasterVolume(masterVolumeSlider.getValue());
        settings.setMusicVolume(musicVolumeSlider.getValue());
        settings.setSfxVolume(sfxVolumeSlider.getValue());
        settings.save();

        System.out.println("SettingsMenu: Applied settings - Camera: " + settings.getCameraScale() +
                         ", UI: " + settings.getUIScale() +
                         ", Master Vol: " + settings.getMasterVolume() +
                         ", Music Vol: " + settings.getMusicVolume() +
                         ", SFX Vol: " + settings.getSfxVolume());
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
     * Updates the master volume value label.
     */
    private void updateMasterVolumeLabel() {
        masterVolumeValueLabel.setText(String.format("%d%%", (int)(masterVolumeSlider.getValue() * 100)));
    }

    /**
     * Updates the music volume value label.
     */
    private void updateMusicVolumeLabel() {
        musicVolumeValueLabel.setText(String.format("%d%%", (int)(musicVolumeSlider.getValue() * 100)));
    }

    /**
     * Updates the SFX volume value label.
     */
    private void updateSfxVolumeLabel() {
        sfxVolumeValueLabel.setText(String.format("%d%%", (int)(sfxVolumeSlider.getValue() * 100)));
    }

    /**
     * Called when window is resized.
     */
    public void onResize() {
        centerOnScreen();
    }
}
