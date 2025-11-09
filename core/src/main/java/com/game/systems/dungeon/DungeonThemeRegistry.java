package com.game.systems.dungeon;

import com.game.systems.dungeon.parsing.DungeonThemeParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing dungeon themes.
 * Uses singleton pattern with lazy loading and caching.
 * Themes are parsed once and cached for reuse.
 */
public class DungeonThemeRegistry {
    private static DungeonThemeRegistry instance;

    private final Map<String, DungeonTheme> loadedThemes;
    private final Map<String, String> themePathMap;

    private DungeonThemeRegistry() {
        this.loadedThemes = new HashMap<>();
        this.themePathMap = new HashMap<>();

        // Register theme paths
        // Add more themes here as they are created
        registerThemePath("forest", "Maps/dungeons/forest_rooms.tmx");
        registerThemePath("cave", "Maps/dungeons/cave_rooms.tmx");
    }

    /**
     * Get the singleton instance (lazy initialization).
     */
    public static DungeonThemeRegistry getInstance() {
        if (instance == null) {
            instance = new DungeonThemeRegistry();
            System.out.println("DungeonThemeRegistry initialized");
        }
        return instance;
    }

    /**
     * Check if the registry has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Register a theme path.
     * @param themeName Name identifier for the theme
     * @param themePath Path to the .tmx file
     */
    public void registerThemePath(String themeName, String themePath) {
        themePathMap.put(themeName, themePath);
    }

    /**
     * Load a theme by name (with caching).
     * @param themeName Name of the theme to load
     * @return DungeonTheme or null if not found
     */
    public DungeonTheme loadTheme(String themeName) {
        // Check cache first
        if (loadedThemes.containsKey(themeName)) {
            System.out.println("DungeonThemeRegistry: Theme '" + themeName + "' loaded from cache");
            return loadedThemes.get(themeName);
        }

        // Get theme path
        String themePath = themePathMap.get(themeName);
        if (themePath == null) {
            System.err.println("DungeonThemeRegistry: Theme '" + themeName + "' not registered");
            return null;
        }

        // Check if file exists
        if (!DungeonThemeParser.themeExists(themePath)) {
            System.err.println("DungeonThemeRegistry: Theme file not found: " + themePath);
            return null;
        }

        // Parse theme
        DungeonTheme theme = DungeonThemeParser.parse(themeName, themePath);
        if (theme == null || theme.getRoomCount() == 0) {
            System.err.println("DungeonThemeRegistry: Failed to parse theme or no rooms found: " + themeName);
            return null;
        }

        // Cache it
        loadedThemes.put(themeName, theme);
        System.out.println("DungeonThemeRegistry: Theme '" + themeName + "' loaded and cached (" + theme.getRoomCount() + " rooms)");
        return theme;
    }

    /**
     * Get a theme (assumes it's already loaded).
     * @param themeName Name of the theme
     * @return DungeonTheme or null if not loaded
     */
    public DungeonTheme getTheme(String themeName) {
        return loadedThemes.get(themeName);
    }

    /**
     * Check if a theme is loaded.
     */
    public boolean isThemeLoaded(String themeName) {
        return loadedThemes.containsKey(themeName);
    }

    /**
     * Preload a theme (load and cache it).
     */
    public void preloadTheme(String themeName) {
        if (!isThemeLoaded(themeName)) {
            loadTheme(themeName);
        }
    }

    /**
     * Unload a theme from cache.
     */
    public void unloadTheme(String themeName) {
        if (loadedThemes.remove(themeName) != null) {
            System.out.println("DungeonThemeRegistry: Theme '" + themeName + "' unloaded from cache");
        }
    }

    /**
     * Clear all cached themes.
     */
    public void clearCache() {
        loadedThemes.clear();
        System.out.println("DungeonThemeRegistry: All themes unloaded");
    }

    /**
     * Get all registered theme names.
     */
    public String[] getRegisteredThemes() {
        return themePathMap.keySet().toArray(new String[0]);
    }

    /**
     * Get count of loaded themes.
     */
    public int getLoadedThemeCount() {
        return loadedThemes.size();
    }
}
