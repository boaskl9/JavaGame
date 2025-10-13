package com.game.systems.collision;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

/**
 * Loader for extracting collision shapes from Tiled maps.
 * This is a separate utility class that bridges Tiled maps to the SpatialQuery system.
 */
public class TiledMapCollisionLoader {

    /**
     * Load all collision shapes from a Tiled map into a SpatialQuery system.
     * Extracts collision objects defined in the tile collision editor.
     *
     * @param map The tiled map to extract collisions from
     * @param spatialQuery The spatial query system to load shapes into
     * @return Number of shapes loaded
     */
    public static int loadFromTiledMap(TiledMap map, SpatialQuery spatialQuery) {
        int shapeCount = 0;

        for (int i = 0; i < map.getLayers().getCount(); i++) {
            if (!(map.getLayers().get(i) instanceof TiledMapTileLayer)) {
                continue;
            }

            TiledMapTileLayer tileLayer = (TiledMapTileLayer) map.getLayers().get(i);
            if (tileLayer == null) continue;

            int tileWidth = (int) tileLayer.getTileWidth();
            int tileHeight = (int) tileLayer.getTileHeight();

            // Loop through all tiles
            for (int x = 0; x < tileLayer.getWidth(); x++) {
                for (int y = 0; y < tileLayer.getHeight(); y++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);

                    if (cell != null && cell.getTile() != null) {
                        MapObjects objects = cell.getTile().getObjects();

                        // Process each collision object for this tile
                        for (MapObject object : objects) {
                            float worldX = x * tileWidth;
                            float worldY = y * tileHeight;

                            if (object instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                                Rectangle worldRect = new Rectangle(
                                    rect.x + worldX,
                                    rect.y + worldY,
                                    rect.width,
                                    rect.height
                                );
                                spatialQuery.addRectangle(worldRect);
                                shapeCount++;

                            } else if (object instanceof PolygonMapObject) {
                                Polygon poly = ((PolygonMapObject) object).getPolygon();
                                float[] vertices = poly.getVertices().clone();
                                Polygon worldPoly = new Polygon(vertices);
                                worldPoly.setPosition(worldX, worldY);
                                spatialQuery.addPolygon(worldPoly);
                                shapeCount++;
                            }
                        }
                    }
                }
            }
        }

        return shapeCount;
    }
}
