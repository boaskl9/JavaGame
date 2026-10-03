package com.game.systems.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;

import java.util.ArrayList;
import java.util.List;

/**
 * The ground drawn behind a map, so the world doesn't end in black past the map's edges.
 * Set up entirely in Tiled (see docs/tiled-maps.md):
 * <ul>
 *   <li>Map property {@code background} (string), e.g. "grass": the fill uses every tile in the map's
 *       tilesets whose own {@code background} property has that value.</li>
 *   <li>Each tile's Tiled <b>Probability</b> is its weight (default 1), so a plain tile at 30 and a
 *       rock at 1 gives the occasional rock.</li>
 *   <li>The map's <b>Background Color</b> (Map Properties) is the screen clear color.</li>
 * </ul>
 * Which tile goes where depends only on the tile coordinates, so it never flickers and every
 * machine in multiplayer sees the same ground.
 */
public class BackgroundFill {
    public static final String MAP_PROPERTY = "background";
    public static final String TILE_PROPERTY = "background";
    public static final String COLOR_PROPERTY = "backgroundcolor"; // Set by libGDX from Tiled's map Background Color

    private final List<TiledMapTile> tiles = new ArrayList<>();
    private final float[] cumulativeWeights;
    private final float totalWeight;
    private final int salt;
    private final Color color;

    public BackgroundFill(TiledMap map) {
        MapProperties properties = map.getProperties();
        String name = properties.get(MAP_PROPERTY, String.class);
        color = parseColor(properties.get(COLOR_PROPERTY, String.class));
        salt = name != null ? name.hashCode() : 0;

        List<Float> weights = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            for (TiledMapTileSet tileset : map.getTileSets()) {
                for (TiledMapTile tile : tileset) {
                    if (name.equals(tile.getProperties().get(TILE_PROPERTY))) {
                        float weight = weightOf(tile);
                        if (weight > 0) {
                            tiles.add(tile);
                            weights.add(weight);
                        }
                    }
                }
            }
            if (tiles.isEmpty()) {
                System.err.println("BackgroundFill: map wants background '" + name
                    + "' but no tile has the property " + TILE_PROPERTY + " = " + name);
            }
        }

        cumulativeWeights = new float[tiles.size()];
        float sum = 0f;
        for (int i = 0; i < tiles.size(); i++) {
            sum += weights.get(i);
            cumulativeWeights[i] = sum;
        }
        totalWeight = sum;
    }

    /** Whether there are any tiles to draw. */
    public boolean hasTiles() {
        return !tiles.isEmpty();
    }

    /** The tile at a tile coordinate (any coordinate, including outside the map), or null if there are none. */
    public TiledMapTile tileAt(int tileX, int tileY) {
        if (tiles.isEmpty()) return null;
        if (tiles.size() == 1) return tiles.get(0);

        // Uniform value in [0, 1) from the coordinates
        float roll = (hash(tileX, tileY, salt) >>> 8) / (float) (1 << 24);
        float target = roll * totalWeight;
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (target < cumulativeWeights[i]) return tiles.get(i);
        }
        return tiles.get(tiles.size() - 1);
    }

    /** The map's Background Color, or null if it has none. */
    public Color getColor() {
        return color;
    }

    private static float weightOf(TiledMapTile tile) {
        Object probability = tile.getProperties().get("probability"); // Tiled's tile Probability
        if (probability == null) return 1f;
        try {
            return Float.parseFloat(probability.toString());
        } catch (NumberFormatException e) {
            return 1f;
        }
    }

    /** A well-mixed hash of a tile coordinate (so neighbouring tiles don't make patterns). */
    static int hash(int x, int y, int salt) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ salt * 0x9e3779b9;
        h ^= h >>> 15;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    /** Tiled colors are "#RRGGBB" or "#AARRGGBB". */
    static Color parseColor(String value) {
        if (value == null) return null;
        String hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            if (hex.length() == 6) {
                return Color.valueOf(hex + "ff");
            }
            if (hex.length() == 8) {
                return Color.valueOf(hex.substring(2) + hex.substring(0, 2));
            }
        } catch (IllegalArgumentException e) {
            // Fall through
        }
        System.err.println("BackgroundFill: can't read background color '" + value + "'");
        return null;
    }
}
