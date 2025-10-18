package com.game.systems.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Manages game settings with persistence via LibGDX Preferences.
 * Settings are automatically saved and loaded.
 */
public class GameSettings {
    private static final String PREFS_NAME = "game_settings";

    // Setting keys
    private static final String KEY_CAMERA_SCALE = "camera_scale";
    private static final String KEY_UI_SCALE = "ui_scale";

    // Default values
    private static final float DEFAULT_CAMERA_SCALE = 1.0f;
    private static final float DEFAULT_UI_SCALE = 1.0f;

    // Min/max values
    public static final float MIN_CAMERA_SCALE = 0.5f;
    public static final float MAX_CAMERA_SCALE = 2.0f;
    public static final float MIN_UI_SCALE = 0.75f;
    public static final float MAX_UI_SCALE = 1.5f;

    private Preferences prefs;

    // Current settings
    private float cameraScale;
    private float uiScale;

    // Listener for settings changes
    private SettingsChangeListener listener;

    public interface SettingsChangeListener {
        void onCameraScaleChanged(float newScale);
        void onUIScaleChanged(float newScale);
    }

    public GameSettings() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        load();
    }

    /**
     * Loads settings from preferences.
     */
    public void load() {
        cameraScale = prefs.getFloat(KEY_CAMERA_SCALE, DEFAULT_CAMERA_SCALE);
        uiScale = prefs.getFloat(KEY_UI_SCALE, DEFAULT_UI_SCALE);

        // Clamp to valid ranges
        cameraScale = clamp(cameraScale, MIN_CAMERA_SCALE, MAX_CAMERA_SCALE);
        uiScale = clamp(uiScale, MIN_UI_SCALE, MAX_UI_SCALE);

        System.out.println("GameSettings: Loaded - Camera scale: " + cameraScale + ", UI scale: " + uiScale);
    }

    /**
     * Saves settings to preferences.
     */
    public void save() {
        prefs.putFloat(KEY_CAMERA_SCALE, cameraScale);
        prefs.putFloat(KEY_UI_SCALE, uiScale);
        prefs.flush();

        System.out.println("GameSettings: Saved - Camera scale: " + cameraScale + ", UI scale: " + uiScale);
    }

    /**
     * Resets all settings to defaults.
     */
    public void resetToDefaults() {
        setCameraScale(DEFAULT_CAMERA_SCALE);
        setUIScale(DEFAULT_UI_SCALE);
        save();
        System.out.println("GameSettings: Reset to defaults");
    }

    // Getters
    public float getCameraScale() {
        return cameraScale;
    }

    public float getUIScale() {
        return uiScale;
    }

    // Setters with validation and notification
    public void setCameraScale(float scale) {
        float oldScale = this.cameraScale;
        this.cameraScale = clamp(scale, MIN_CAMERA_SCALE, MAX_CAMERA_SCALE);

        if (oldScale != this.cameraScale && listener != null) {
            listener.onCameraScaleChanged(this.cameraScale);
        }
    }

    public void setUIScale(float scale) {
        float oldScale = this.uiScale;
        this.uiScale = clamp(scale, MIN_UI_SCALE, MAX_UI_SCALE);

        if (oldScale != this.uiScale && listener != null) {
            listener.onUIScaleChanged(this.uiScale);
        }
    }

    public void setListener(SettingsChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Helper method to clamp a value between min and max.
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
