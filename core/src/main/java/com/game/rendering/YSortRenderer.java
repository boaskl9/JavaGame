package com.game.rendering;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer that handles Y-sorting for proper depth ordering.
 *
 * In top-down 2D games like Stardew Valley, objects with a lower Y position
 * should render behind objects with a higher Y position.
 *
 * This renderer splits the map into:
 * 1. Background layers (terrain, ground details)
 * 2. Y-sorted layers (features like trees, houses) - rendered with entities
 * 3. Top layers (roofs, overlays)
 */
public class YSortRenderer {

    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMap map;

    // Layer configuration
    private int[] backgroundLayers;  // Layers to render first (terrain)
    private int[] ySortedLayers;     // Layers to Y-sort with entities (trees, houses)
    private int[] topLayers;         // Layers to render last (roofs, overlays)

    // Debug mode
    private boolean debugMode = false;
    private com.badlogic.gdx.graphics.g2d.BitmapFont debugFont;

    public YSortRenderer(OrthogonalTiledMapRenderer mapRenderer, TiledMap map) {
        this.mapRenderer = mapRenderer;
        this.map = map;

        // Default configuration - you can customize this
        detectLayers();

        // Create debug font
        debugFont = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        debugFont.setColor(1, 1, 0, 1); // Yellow
        debugFont.getData().setScale(0.4f);
    }

    /**
     * Automatically detect which layers to use based on custom properties.
     *
     * Reads layer properties from Tiled:
     * - "foregroundRender" (boolean): If true, layer is Y-sorted with entities
     * - "topLayer" (boolean): If true, layer always renders on top
     * - Otherwise: Layer is rendered as background (default)
     */
    private void detectLayers() {
        List<Integer> background = new ArrayList<>();
        List<Integer> ySorted = new ArrayList<>();
        List<Integer> top = new ArrayList<>();

        for (int i = 0; i < map.getLayers().getCount(); i++) {
            // Only process tile layers
            if (!(map.getLayers().get(i) instanceof TiledMapTileLayer)) {
                continue;
            }

            String layerName = map.getLayers().get(i).getName();

            // Check custom properties
            Boolean foregroundRender = map.getLayers().get(i).getProperties().get("foregroundRender", Boolean.class);
            Boolean topLayer = map.getLayers().get(i).getProperties().get("topLayer", Boolean.class);

            if (topLayer != null && topLayer) {
                // Top layer - always renders on top
                top.add(i);
                System.out.println("  [TOP] Layer " + i + ": " + layerName);
            } else if (foregroundRender != null && foregroundRender) {
                // Foreground - Y-sorted with entities
                ySorted.add(i);
                System.out.println("  [YSORT] Layer " + i + ": " + layerName);
            } else {
                // Default to background
                background.add(i);
                System.out.println("  [BACKGROUND] Layer " + i + ": " + layerName);
            }
        }

        backgroundLayers = background.stream().mapToInt(Integer::intValue).toArray();
        ySortedLayers = ySorted.stream().mapToInt(Integer::intValue).toArray();
        topLayers = top.stream().mapToInt(Integer::intValue).toArray();

        System.out.println("YSortRenderer configured:");
        System.out.println("  - Background layers: " + background.size());
        System.out.println("  - Y-sorted layers: " + ySorted.size());
        System.out.println("  - Top layers: " + top.size());
    }

    /**
     * Render the map with proper Y-sorting.
     *
     * @param batch SpriteBatch to render with
     * @param gameObjects List of entities to render (must have Transform component)
     * @param entityRenderer Callback to render each entity
     */
    public void render(SpriteBatch batch, List<GameObject> gameObjects, EntityRenderer entityRenderer) {
        // IMPORTANT: Everything must be rendered in a single batch to maintain proper Z-order!

        batch.begin();

        // 1. Render background layers manually
        renderLayers(batch, backgroundLayers);

        // 2. Close batch before Y-sorted content (it has its own batch management)
        batch.end();

        // 3. Render Y-sorted content (feature layers + entities)
        renderYSortedContent(batch, gameObjects, entityRenderer);

        // 4. Render top layers
        if (topLayers.length > 0) {
            batch.begin();
            renderLayers(batch, topLayers);
            batch.end();
        }
    }

    /**
     * Render specific layer indices manually to our batch.
     */
    private void renderLayers(SpriteBatch batch, int[] layerIndices) {
        for (int layerIndex : layerIndices) {
            if (!(map.getLayers().get(layerIndex) instanceof TiledMapTileLayer)) continue;

            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerIndex);
            renderLayerTiles(batch, layer);
        }
    }

    /**
     * Render all tiles in a layer.
     */
    private void renderLayerTiles(SpriteBatch batch, TiledMapTileLayer layer) {
        int tileWidth = (int) layer.getTileWidth();
        int tileHeight = (int) layer.getTileHeight();

        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    float worldX = x * tileWidth;
                    float worldY = y * tileHeight;

                    batch.draw(
                        cell.getTile().getTextureRegion(),
                        worldX, worldY,
                        tileWidth, tileHeight
                    );
                }
            }
        }
    }

    /**
     * Get the layer indices that are being Y-sorted (for debugging).
     */
    public int[] getYSortedLayerIndices() {
        return ySortedLayers;
    }

    /**
     * Enable or disable debug mode.
     * When enabled, shows render order numbers on tiles and entities.
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    /**
     * Render Y-sorted layers tile-by-tile, interleaved with entities.
     */
    private void renderYSortedContent(SpriteBatch batch, List<GameObject> gameObjects, EntityRenderer entityRenderer) {
        // Collect all renderable items (tiles + entities)
        List<RenderItem> items = new ArrayList<>();

        // Add tiles from Y-sorted layers
        for (int layerIndex : ySortedLayers) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerIndex);
            if (layer == null) continue;

            int tileWidth = (int) layer.getTileWidth();
            int tileHeight = (int) layer.getTileHeight();

            for (int x = 0; x < layer.getWidth(); x++) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                    if (cell != null && cell.getTile() != null) {
                        float worldY = y * tileHeight;
                        items.add(new TileRenderItem(layer, x, y, worldY + tileHeight));
                    }
                }
            }
        }

        // Add entities
        for (GameObject obj : gameObjects) {
            Transform transform = obj.getComponent(Transform.class);
            if (transform != null && obj.isActive()) {
                // Get render component to find sprite height
                com.game.components.RenderComponent renderComp = obj.getComponent(com.game.components.RenderComponent.class);
                float spriteHeight = renderComp != null ? renderComp.getHeight() : 16; // Default to 16 if no render component

                // Use bottom of sprite for Y-sorting (transform.y is top, so add height)
                float bottomY = transform.getY() + spriteHeight;
                items.add(new EntityRenderItem(obj, bottomY));
            }
        }

        // Sort by Y position
        // In SpriteBatch: items drawn LATER appear ON TOP
        // In top-down 2D: HIGHER Y = further back in world = should appear BEHIND
        // So: HIGHER Y should be drawn FIRST (lower render order)
        // Therefore: ASCENDING sort (lower Y values last = drawn last = on top)
        items.sort((a, b) -> Float.compare(b.sortY, a.sortY));

        // Render in sorted order
        batch.begin();
        int renderOrder = 0;
        for (RenderItem item : items) {
            if (item instanceof EntityRenderItem) {
                EntityRenderItem entityItem = (EntityRenderItem) item;
                entityRenderer.render(batch, entityItem.gameObject);

                // Debug: Show render order and Y value on entities
                if (debugMode) {
                    Transform transform = entityItem.gameObject.getComponent(Transform.class);
                    if (transform != null) {
                        String debugText = "#" + renderOrder + " Y:" + (int)entityItem.sortY;
                        debugFont.draw(batch, debugText, transform.getX() + 2, transform.getY() + 14);
                    }
                }
            } else if (item instanceof TileRenderItem) {
                TileRenderItem tileItem = (TileRenderItem) item;
                renderTile(batch, tileItem);

                // Debug: Show render order and Y value on tiles
                if (debugMode) {
                    float tileWidth = tileItem.layer.getTileWidth();
                    float tileHeight = tileItem.layer.getTileHeight();
                    float x = tileItem.x * tileWidth;
                    float y = tileItem.y * tileHeight;
                    String debugText = "#" + renderOrder + " Y:" + (int)tileItem.sortY;
                    debugFont.draw(batch, debugText, x + 1, y + tileHeight - 2);
                }
            }
            renderOrder++;
        }
        batch.end();
    }

    /**
     * Render a single tile.
     */
    private void renderTile(SpriteBatch batch, TileRenderItem item) {
        TiledMapTileLayer.Cell cell = item.layer.getCell(item.x, item.y);
        if (cell == null || cell.getTile() == null) return;

        float tileWidth = item.layer.getTileWidth();
        float tileHeight = item.layer.getTileHeight();
        float x = item.x * tileWidth;
        float y = item.y * tileHeight;

        batch.draw(
            cell.getTile().getTextureRegion(),
            x, y,
            tileWidth, tileHeight
        );
    }

    /**
     * Set custom layer indices for rendering.
     */
    public void setLayerConfiguration(int[] background, int[] ySorted, int[] top) {
        this.backgroundLayers = background;
        this.ySortedLayers = ySorted;
        this.topLayers = top;
    }

    // ========== Helper Classes ==========

    private static abstract class RenderItem {
        float sortY;
    }

    private static class EntityRenderItem extends RenderItem {
        GameObject gameObject;

        EntityRenderItem(GameObject gameObject, float sortY) {
            this.gameObject = gameObject;
            this.sortY = sortY;
        }
    }

    private static class TileRenderItem extends RenderItem {
        TiledMapTileLayer layer;
        int x, y;

        TileRenderItem(TiledMapTileLayer layer, int x, int y, float sortY) {
            this.layer = layer;
            this.x = x;
            this.y = y;
            this.sortY = sortY;
        }
    }

    /**
     * Callback interface for rendering entities.
     */
    public interface EntityRenderer {
        void render(SpriteBatch batch, GameObject gameObject);
    }
}
