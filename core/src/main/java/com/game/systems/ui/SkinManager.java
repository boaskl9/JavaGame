package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Manages UI skins for the game.
 * Provides easy access to different UI themes.
 */
public class SkinManager {
    private static Skin currentSkin;
    private static SkinTheme currentTheme = SkinTheme.DEFAULT;

    public enum SkinTheme {
        DEFAULT("ui/uiskin.json", "ui/uiskin.atlas"),
        WOOD("ui/wood-theme.json", "ui/wood-theme.atlas");

        private final String skinPath;
        private final String atlasPath;

        SkinTheme(String skinPath, String atlasPath) {
            this.skinPath = skinPath;
            this.atlasPath = atlasPath;
        }

        public String getSkinPath() {
            return skinPath;
        }

        public String getAtlasPath() {
            return atlasPath;
        }
    }

    /**
     * Initializes the skin manager with the default theme.
     */
    public static void initialize() {
        loadTheme(SkinTheme.DEFAULT);
    }

    /**
     * Loads a specific skin theme.
     * @param theme The theme to load
     */
    public static void loadTheme(SkinTheme theme) {
        if (currentSkin != null) {
            currentSkin.dispose();
        }

        try {
            // Load the texture atlas
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(theme.getAtlasPath()));

            // Create the skin with the atlas and JSON file
            currentSkin = new Skin(Gdx.files.internal(theme.getSkinPath()), atlas);
            currentTheme = theme;

            Gdx.app.log("SkinManager", "Loaded theme: " + theme.name());
        } catch (Exception e) {
            Gdx.app.error("SkinManager", "Failed to load theme: " + theme.name(), e);

            // Fallback to default if wood theme fails
            if (theme != SkinTheme.DEFAULT) {
                Gdx.app.log("SkinManager", "Falling back to DEFAULT theme");
                loadTheme(SkinTheme.DEFAULT);
            }
        }
    }

    /**
     * Gets the current skin.
     * @return The current skin instance
     */
    public static Skin getSkin() {
        if (currentSkin == null) {
            initialize();
        }
        return currentSkin;
    }

    /**
     * Gets the current theme.
     * @return The current theme
     */
    public static SkinTheme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Disposes of the current skin resources.
     * Should be called when the game is shutting down.
     */
    public static void dispose() {
        if (currentSkin != null) {
            currentSkin.dispose();
            currentSkin = null;
        }
    }
}
