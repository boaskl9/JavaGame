package com.game.world;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class CollisionSystem {
    private List<Rectangle> collisionRectangles = new ArrayList<>();
    private List<Polygon> collisionPolygons = new ArrayList<>();

    /**
     * Load all collision shapes from the tiled map
     */
    public void loadCollisionShapes(TiledMap map) {
        for(int i = 0; i < map.getLayers().getCount(); i++) {
            if (!(map.getLayers().get(i) instanceof TiledMapTileLayer)) {
                continue;
            }
            TiledMapTileLayer tileLayer = (TiledMapTileLayer) map.getLayers().get(i);

            if (tileLayer == null) {
                System.out.println("Warning: No tile layer found for collision!");
                return;
            }

            int tileWidth = (int) tileLayer.getTileWidth();
            int tileHeight = (int) tileLayer.getTileHeight();

            // Loop through all tiles in the map
            for (int x = 0; x < tileLayer.getWidth(); x++) {
                for (int y = 0; y < tileLayer.getHeight(); y++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);

                    if (cell != null && cell.getTile() != null) {
                        MapObjects objects = cell.getTile().getObjects();

                        // Process each collision object defined for this tile
                        for (MapObject object : objects) {
                            float worldX = x * tileWidth;
                            float worldY = y * tileHeight;

                            if (object instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                                // Create a new rectangle offset by the tile's world position
                                Rectangle worldRect = new Rectangle(
                                    rect.x + worldX,
                                    rect.y + worldY,
                                    rect.width,
                                    rect.height
                                );
                                collisionRectangles.add(worldRect);

                            } else if (object instanceof PolygonMapObject) {
                                Polygon poly = ((PolygonMapObject) object).getPolygon();
                                // Clone the polygon and set its world position
                                float[] vertices = poly.getVertices().clone();
                                Polygon worldPoly = new Polygon(vertices);
                                worldPoly.setPosition(worldX, worldY);
                                collisionPolygons.add(worldPoly);
                            }
                        }
                    }
                }
            }

            System.out.println("Loaded " + collisionRectangles.size() + " collision rectangles and "
                + collisionPolygons.size() + " collision polygons");
        }


    }

    /**
     * Check if a single point is blocked by collision
     */
    public boolean isPointBlocked(float x, float y) {
        // Check rectangles first (faster)
        for (Rectangle rect : collisionRectangles) {
            if (rect.contains(x, y)) {
                return true;
            }
        }

        // Check polygons
        for (Polygon poly : collisionPolygons) {
            if (poly.contains(x, y)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a rectangular area (like a player hitbox) collides with anything
     */
    public boolean isAreaBlocked(float x, float y, float width, float height) {
        // Check the four corners of the rectangle
        if (isPointBlocked(x, y)) return true;
        if (isPointBlocked(x + width, y)) return true;
        if (isPointBlocked(x, y + height)) return true;
        if (isPointBlocked(x + width, y + height)) return true;

        // Optional: Check center point for better collision
        if (isPointBlocked(x + width/2, y + height/2)) return true;

        return false;
    }

    /**
     * Check if a rectangle overlaps with any collision shapes
     */
    public boolean isRectangleBlocked(Rectangle playerRect) {
        // Check against all collision rectangles
        for (Rectangle rect : collisionRectangles) {
            if (playerRect.overlaps(rect)) {
                return true;
            }
        }

        // Check against polygons (approximate with corner points)
        return isAreaBlocked(playerRect.x, playerRect.y, playerRect.width, playerRect.height);
    }

    /**
     * Get all collision rectangles (for debug rendering)
     */
    public List<Rectangle> getCollisionRectangles() {
        return collisionRectangles;
    }

    /**
     * Get all collision polygons (for debug rendering)
     */
    public List<Polygon> getCollisionPolygons() {
        return collisionPolygons;
    }

    public void clear() {
        collisionRectangles.clear();
        collisionPolygons.clear();
    }
}
