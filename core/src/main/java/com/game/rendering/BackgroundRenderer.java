package com.game.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.game.systems.level.BackgroundFill;

/**
 * Draws a map's {@link BackgroundFill} over everything the camera sees, before the map itself.
 * It fills inside the map too, so gaps in the ground layer show the fill rather than black.
 */
public class BackgroundRenderer {
    private static final int TILE_SIZE = 16;

    private final BackgroundFill fill;

    public BackgroundRenderer(TiledMap map) {
        this.fill = new BackgroundFill(map);
    }

    /** The color to clear the screen with: the map's Background Color, or black. */
    public Color getClearColor() {
        return fill.getColor() != null ? fill.getColor() : Color.BLACK;
    }

    /** Draw the fill. The batch must be begun with the camera's projection. */
    public void render(SpriteBatch batch, OrthographicCamera camera) {
        if (!fill.hasTiles()) return;

        float halfWidth = camera.viewportWidth * camera.zoom / 2f;
        float halfHeight = camera.viewportHeight * camera.zoom / 2f;
        int minX = (int) Math.floor((camera.position.x - halfWidth) / TILE_SIZE);
        int maxX = (int) Math.floor((camera.position.x + halfWidth) / TILE_SIZE);
        int minY = (int) Math.floor((camera.position.y - halfHeight) / TILE_SIZE);
        int maxY = (int) Math.floor((camera.position.y + halfHeight) / TILE_SIZE);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                TiledMapTile tile = fill.tileAt(x, y);
                TextureRegion region = tile != null ? tile.getTextureRegion() : null;
                if (region != null) {
                    batch.draw(region, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }
}
