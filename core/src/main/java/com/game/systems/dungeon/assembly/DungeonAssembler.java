package com.game.systems.dungeon.assembly;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.dungeon.generation.DungeonGenerationResult;
import com.game.systems.level.LevelData;

import java.util.List;
import java.util.Map;

/**
 * Main orchestrator for assembling a generated dungeon into a playable TiledMap.
 *
 * Takes a DungeonGenerationResult (placed rooms) and produces an AssembledDungeon
 * containing a complete TiledMap, collision data, and spawn objects.
 *
 * Assembly process:
 * 1. Merge tile layers from all placed rooms
 * 2. Create TiledMap and add merged layers
 * 3. Merge collision shapes with world offsets
 * 4. Collect spawn objects with world offsets
 * 5. Create LevelData from collected objects
 * 6. Package everything into AssembledDungeon
 */
public class DungeonAssembler {

    private static final int TILE_SIZE = 16;

    /**
     * Assembles a generated dungeon into a complete, playable map.
     *
     * @param genResult The generation result containing placed rooms
     * @param theme The dungeon theme (needed for tilesets)
     * @return AssembledDungeon ready to be loaded into GameScreen
     */
    public static AssembledDungeon assemble(DungeonGenerationResult genResult, com.game.systems.dungeon.DungeonTheme theme) {
        if (genResult == null) {
            throw new IllegalArgumentException("Generation result cannot be null");
        }

        if (genResult.getPlacedRooms().isEmpty()) {
            throw new IllegalArgumentException("Cannot assemble dungeon with no placed rooms");
        }

        // Step 1: Merge tile layers from all rooms
        TileLayerMerger.MergeResult mergeResult =
            TileLayerMerger.mergeLayersWithOffset(genResult.getPlacedRooms(), genResult.getBounds());
        Map<String, TiledMapTileLayer> mergedLayers = mergeResult.layers;

        // Step 2: Create TiledMap and add layers in correct order
        TiledMap tiledMap = new TiledMap();

        // Copy tilesets from source theme
        int tilesetCount = 0;
        if (theme != null && theme.getSourceMap() != null) {
            com.badlogic.gdx.maps.tiled.TiledMapTileSets sourceTileSets = theme.getSourceMap().getTileSets();
            for (com.badlogic.gdx.maps.tiled.TiledMapTileSet tileset : sourceTileSets) {
                tiledMap.getTileSets().addTileSet(tileset);
                tilesetCount++;
            }
            System.out.println("DungeonAssembler: Copied " + tilesetCount + " tilesets from source theme");
        }

        // DEBUG: Check tilesets
        int finalTilesetCount = 0;
        for (com.badlogic.gdx.maps.tiled.TiledMapTileSet ts : tiledMap.getTileSets()) {
            finalTilesetCount++;
        }
        System.out.println("DungeonAssembler: New TiledMap has " + finalTilesetCount + " tilesets");

        // Add layers in rendering order (bottom to top)
        String[] layerOrder = {"Background", "Features", "Top"};
        for (String layerName : layerOrder) {
            TiledMapTileLayer layer = mergedLayers.get(layerName);
            if (layer != null) {
                tiledMap.getLayers().add(layer);

                // DEBUG: Check if layer has tiles
                int nonNullCells = 0;
                int cellsWithTiles = 0;
                for (int x = 0; x < layer.getWidth(); x++) {
                    for (int y = 0; y < layer.getHeight(); y++) {
                        TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                        if (cell != null) {
                            nonNullCells++;
                            if (cell.getTile() != null) {
                                cellsWithTiles++;
                            }
                        }
                    }
                }
                System.out.println("DungeonAssembler: Layer '" + layerName + "' has " + nonNullCells + " cells, " + cellsWithTiles + " with tiles");
            }
        }

        // Step 3: Merge collision shapes (use same offset as tiles!)
        List<Rectangle> collisionShapes =
            CollisionMerger.mergeCollisionWithOffset(genResult.getPlacedRooms(), mergeResult.offsetX, mergeResult.offsetY);

        // Step 4: Collect spawn objects (use same offset as tiles!)
        List<LevelData.LevelObject> objects =
            ObjectSpawner.collectObjectsWithOffset(genResult.getPlacedRooms(), mergeResult.offsetX, mergeResult.offsetY);

        // Step 5: Create LevelData from bounds and objects
        int mapWidthTiles = (int) Math.ceil(genResult.getBounds().width / TILE_SIZE);
        int mapHeightTiles = (int) Math.ceil(genResult.getBounds().height / TILE_SIZE);

        LevelData levelData = new LevelData(mapWidthTiles, mapHeightTiles, TILE_SIZE);

        // Add all collected objects to level data
        for (LevelData.LevelObject obj : objects) {
            levelData.addObject(obj);
        }

        // Step 6: Package everything into AssembledDungeon
        return new AssembledDungeon(
            tiledMap,
            levelData,
            collisionShapes,
            genResult.getThemeName(),
            genResult.getTargetBudget(),
            genResult.getSeed(),
            mergeResult.offsetX,
            mergeResult.offsetY
        );
    }

    /**
     * Quick assembly for testing - generates and immediately assembles.
     *
     * @param themeName The dungeon theme to use
     * @param targetBudget Target cost budget for room selection
     * @param seed Random seed for generation
     * @return AssembledDungeon ready to load
     */
    public static AssembledDungeon generateAndAssemble(String themeName, int targetBudget, long seed) {
        DungeonGenerationResult genResult =
            com.game.systems.dungeon.generation.DungeonGenerator.generateWithSeed(
                themeName, targetBudget, seed);

        // Get theme for tilesets
        com.game.systems.dungeon.DungeonTheme theme =
            com.game.systems.dungeon.DungeonThemeRegistry.getInstance().getTheme(themeName);

        return assemble(genResult, theme);
    }
}
