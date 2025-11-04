package com.game.systems.dungeon.parsing;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.RoomBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts room boundaries from a dungeon theme Tiled map.
 * Rooms are defined by rectangle objects in the "RoomMarkers" layer.
 */
public class RoomExtractor {
    private static final String ROOM_MARKERS_LAYER = "RoomMarkers";
    private static final int TILE_SIZE = 16;

    /**
     * Scan the theme map for rooms defined by marker rectangles.
     * @param map The Tiled map containing room markers
     * @return List of room boundaries
     */
    public static List<RoomBounds> extractRooms(TiledMap map) {
        List<RoomBounds> rooms = new ArrayList<>();

        // Get RoomMarkers layer
        MapLayer markersLayer = map.getLayers().get(ROOM_MARKERS_LAYER);
        if (markersLayer == null) {
            System.err.println("RoomExtractor: No 'RoomMarkers' layer found in theme map!");
            return rooms;
        }

        // Extract each room marker
        for (MapObject obj : markersLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();

                // Convert pixel coordinates to tile coordinates
                int tileX = (int) (rect.x / TILE_SIZE);
                int tileY = (int) (rect.y / TILE_SIZE);
                int tileW = (int) (rect.width / TILE_SIZE);
                int tileH = (int) (rect.height / TILE_SIZE);

                // Create room bounds
                RoomBounds bounds = new RoomBounds(tileX, tileY, tileW, tileH);
                rooms.add(bounds);

                System.out.println("RoomExtractor: Found room - " + obj.getName() + " at " + bounds);
            }
        }

        System.out.println("RoomExtractor: Extracted " + rooms.size() + " rooms");
        return rooms;
    }

    /**
     * Get the name of the room marker at the given bounds.
     * @param map The Tiled map
     * @param bounds The room bounds
     * @return Room name or null if not found
     */
    public static String getRoomName(TiledMap map, RoomBounds bounds) {
        MapLayer markersLayer = map.getLayers().get(ROOM_MARKERS_LAYER);
        if (markersLayer == null) {
            return null;
        }

        // Find the marker object that matches these bounds
        for (MapObject obj : markersLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();

                // Convert to tile coordinates and compare
                int tileX = (int) (rect.x / TILE_SIZE);
                int tileY = (int) (rect.y / TILE_SIZE);
                int tileW = (int) (rect.width / TILE_SIZE);
                int tileH = (int) (rect.height / TILE_SIZE);

                if (bounds.getX() == tileX && bounds.getY() == tileY &&
                    bounds.getWidth() == tileW && bounds.getHeight() == tileH) {
                    return obj.getName() != null ? obj.getName() : "unnamed_room";
                }
            }
        }

        return "unnamed_room";
    }

    /**
     * Get the marker object for a room.
     * @param map The Tiled map
     * @param bounds The room bounds
     * @return The marker object or null if not found
     */
    public static MapObject getRoomMarker(TiledMap map, RoomBounds bounds) {
        MapLayer markersLayer = map.getLayers().get(ROOM_MARKERS_LAYER);
        if (markersLayer == null) {
            return null;
        }

        // Find the marker object that matches these bounds
        for (MapObject obj : markersLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();

                // Convert to tile coordinates and compare
                int tileX = (int) (rect.x / TILE_SIZE);
                int tileY = (int) (rect.y / TILE_SIZE);
                int tileW = (int) (rect.width / TILE_SIZE);
                int tileH = (int) (rect.height / TILE_SIZE);

                if (bounds.getX() == tileX && bounds.getY() == tileY &&
                    bounds.getWidth() == tileW && bounds.getHeight() == tileH) {
                    return obj;
                }
            }
        }

        return null;
    }
}
