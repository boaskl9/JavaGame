package com.game.systems.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundFillTest {

    @Test
    void mapsWithoutABackgroundHaveNoFill() {
        TiledMap map = mapWith(tile(1, "grass", null));

        BackgroundFill fill = new BackgroundFill(map);

        assertFalse(fill.hasTiles());
        assertNull(fill.tileAt(-5, 3));
        assertNull(fill.getColor());
    }

    @Test
    void theFillUsesTheTilesMarkedWithTheMapsBackground() {
        TiledMapTile grass = tile(1, "grass", null);
        TiledMap map = mapWith(grass, tile(2, "sand", null), tile(3, null, null));
        map.getProperties().put("background", "grass");

        BackgroundFill fill = new BackgroundFill(map);

        for (int x = -50; x < 50; x++) {
            assertSame(grass, fill.tileAt(x, x * 7));
        }
    }

    @Test
    void tileProbabilityIsTheWeightAndTheChoiceNeverChanges() {
        TiledMapTile plain = tile(1, "grass", "30");
        TiledMapTile rock = tile(2, "grass", null); // Tiled leaves out a probability of 1
        TiledMap map = mapWith(plain, rock);
        map.getProperties().put("background", "grass");
        BackgroundFill fill = new BackgroundFill(map);
        BackgroundFill sameMapAgain = new BackgroundFill(map);

        int rocks = 0;
        for (int y = -100; y < 100; y++) {
            for (int x = -100; x < 100; x++) {
                TiledMapTile chosen = fill.tileAt(x, y);
                if (chosen == rock) rocks++;
                assertSame(chosen, fill.tileAt(x, y), "same tile every frame");
                assertSame(chosen, sameMapAgain.tileAt(x, y), "same tile on every machine");
            }
        }

        double share = rocks / 40_000.0;
        assertEquals(1 / 31.0, share, 0.01, "about 1 rock per 31 tiles");
    }

    @Test
    void readsTiledBackgroundColors() {
        assertEquals(new Color(0x336699ff), BackgroundFill.parseColor("#336699"));
        assertEquals(new Color(0x33669980), BackgroundFill.parseColor("#80336699"), "Tiled puts alpha first");
        assertNull(BackgroundFill.parseColor("not a color"));
    }

    private static TiledMapTile tile(int id, String background, String probability) {
        StaticTiledMapTile tile = new StaticTiledMapTile(new TextureRegion());
        tile.setId(id);
        if (background != null) tile.getProperties().put("background", background);
        if (probability != null) tile.getProperties().put("probability", probability);
        return tile;
    }

    private static TiledMap mapWith(TiledMapTile... tiles) {
        TiledMapTileSet tileset = new TiledMapTileSet();
        for (TiledMapTile tile : tiles) {
            tileset.putTile(tile.getId(), tile);
        }
        TiledMap map = new TiledMap();
        map.getTileSets().addTileSet(tileset);
        return map;
    }
}
