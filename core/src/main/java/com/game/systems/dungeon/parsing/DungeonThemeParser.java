package com.game.systems.dungeon.parsing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.game.systems.dungeon.DungeonTheme;
import com.game.systems.dungeon.RoomBounds;
import com.game.systems.dungeon.RoomTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Main parser for dungeon theme files.
 * Loads a Tiled .tmx file and extracts all room templates.
 */
public class DungeonThemeParser {

    /**
     * Parse a dungeon theme from a Tiled map file.
     * @param themeName Name of the theme (used for identification)
     * @param themePath Path to the .tmx file (e.g., "levels/dungeons/forest_rooms.tmx")
     * @return DungeonTheme containing all extracted room templates
     */
    public static DungeonTheme parse(String themeName, String themePath) {
        System.out.println("DungeonThemeParser: Loading theme '" + themeName + "' from " + themePath);

        // Load Tiled map
        TmxMapLoader mapLoader = new TmxMapLoader();
        TiledMap map;
        try {
            map = mapLoader.load(themePath);
        } catch (Exception e) {
            System.err.println("DungeonThemeParser: Failed to load theme file: " + e.getMessage());
            e.printStackTrace();
            return new DungeonTheme(themeName, new ArrayList<>(), null);
        }

        // Extract room boundaries
        List<RoomBounds> roomBounds = RoomExtractor.extractRooms(map);
        System.out.println("DungeonThemeParser: Found " + roomBounds.size() + " rooms");

        // Extract data for each room
        List<RoomTemplate> rooms = new ArrayList<>();
        int roomIndex = 0;
        for (RoomBounds bounds : roomBounds) {
            String roomId = themeName + "_room_" + roomIndex;
            RoomTemplate room = RoomDataExtractor.extractRoom(map, bounds, roomId);
            rooms.add(room);
            roomIndex++;

            System.out.println("DungeonThemeParser: Extracted " + room);
        }

        // Keep map alive - we need it for tilesets when assembling dungeons!
        System.out.println("DungeonThemeParser: Theme '" + themeName + "' loaded successfully with " + rooms.size() + " rooms");
        return new DungeonTheme(themeName, rooms, map);
    }

    /**
     * Validate that a theme file exists.
     * @param themePath Path to the .tmx file
     * @return true if file exists
     */
    public static boolean themeExists(String themePath) {
        return Gdx.files.internal(themePath).exists();
    }
}
