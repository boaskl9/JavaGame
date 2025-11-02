package com.game.systems.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Manages game settings with persistence via LibGDX Preferences.
 * Settings are automatically saved and loaded.
 *
 * Singleton pattern - use GameSettings.getInstance()
 */
public class GameSettings {
    private static GameSettings instance;
    private static final String PREFS_NAME = "game_settings";

    // Setting keys
    private static final String KEY_CAMERA_SCALE = "camera_scale";
    private static final String KEY_UI_SCALE = "ui_scale";
    private static final String KEY_MASTER_VOLUME = "master_volume";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";

    // Default values
    private static final float DEFAULT_CAMERA_SCALE = 1.0f;
    private static final float DEFAULT_UI_SCALE = 1.0f;
    private static final float DEFAULT_MASTER_VOLUME = 1.0f;
    private static final float DEFAULT_MUSIC_VOLUME = 0.7f;
    private static final float DEFAULT_SFX_VOLUME = 0.8f;

    // Min/max values
    public static final float MIN_CAMERA_SCALE = 0.5f;
    public static final float MAX_CAMERA_SCALE = 2.0f;
    public static final float MIN_UI_SCALE = 0.75f;
    public static final float MAX_UI_SCALE = 1.5f;

    private Preferences prefs;

    // Current settings
    private float cameraScale;
    private float uiScale;
    private float masterVolume;
    private float musicVolume;
    private float sfxVolume;

    // Listener for settings changes
    private SettingsChangeListener listener;

    public interface SettingsChangeListener {
        void onCameraScaleChanged(float newScale);
        void onUIScaleChanged(float newScale);
    }

    /**
     * Private constructor for singleton pattern.
     */
    private GameSettings() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        load();
    }

    /**
     * Gets the singleton instance, creating it if necessary.
     */
    public static GameSettings getInstance() {
        if (instance == null) {
            instance = new GameSettings();
        }
        return instance;
    }

    /**
     * Loads settings from preferences.
     */
    public void load() {
        cameraScale = prefs.getFloat(KEY_CAMERA_SCALE, DEFAULT_CAMERA_SCALE);
        uiScale = prefs.getFloat(KEY_UI_SCALE, DEFAULT_UI_SCALE);
        masterVolume = prefs.getFloat(KEY_MASTER_VOLUME, DEFAULT_MASTER_VOLUME);
        musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);

        // Clamp to valid ranges
        cameraScale = clamp(cameraScale, MIN_CAMERA_SCALE, MAX_CAMERA_SCALE);
        uiScale = clamp(uiScale, MIN_UI_SCALE, MAX_UI_SCALE);
        masterVolume = clamp(masterVolume, 0f, 1f);
        musicVolume = clamp(musicVolume, 0f, 1f);
        sfxVolume = clamp(sfxVolume, 0f, 1f);

        System.out.println("GameSettings: Loaded - Camera: " + cameraScale +
                           ", UI: " + uiScale +
                           ", Master Vol: " + masterVolume +
                           ", Music Vol: " + musicVolume +
                           ", SFX Vol: " + sfxVolume);
    }

    /**
     * Saves settings to preferences.
     */
    public void save() {
        prefs.putFloat(KEY_CAMERA_SCALE, cameraScale);
        prefs.putFloat(KEY_UI_SCALE, uiScale);
        prefs.putFloat(KEY_MASTER_VOLUME, masterVolume);
        prefs.putFloat(KEY_MUSIC_VOLUME, musicVolume);
        prefs.putFloat(KEY_SFX_VOLUME, sfxVolume);
        prefs.flush();

        System.out.println("GameSettings: Saved - Camera: " + cameraScale +
                           ", UI: " + uiScale +
                           ", Volumes: " + masterVolume + "/" + musicVolume + "/" + sfxVolume);
    }

    /**
     * Resets all settings to defaults.
     */
    public void resetToDefaults() {
        setCameraScale(DEFAULT_CAMERA_SCALE);
        setUIScale(DEFAULT_UI_SCALE);
        setMasterVolume(DEFAULT_MASTER_VOLUME);
        setMusicVolume(DEFAULT_MUSIC_VOLUME);
        setSfxVolume(DEFAULT_SFX_VOLUME);
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

    public float getMasterVolume() {
        return masterVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
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

    public void setMasterVolume(float volume) {
        this.masterVolume = clamp(volume, 0f, 1f);
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = clamp(volume, 0f, 1f);
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = clamp(volume, 0f, 1f);
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
