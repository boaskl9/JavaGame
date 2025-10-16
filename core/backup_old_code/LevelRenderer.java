package com.game.main;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.game.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class LevelRenderer {

    /**
     * Renders the map and entities interleaved between layers.
     *
     * @param batch       SpriteBatch to render entities
     * @param mapRenderer OrthogonalTiledMapRenderer for the TiledMap
     * @param entities    List of all entities to render
     * @param camera      Camera for the view
     * @param insertIndex Index (layer ID) after which to render entities
     */
    public static void render(SpriteBatch batch,
                              OrthogonalTiledMapRenderer mapRenderer,
                              List<Entity> entities,
                              OrthographicCamera camera,
                              int insertIndex) {

        mapRenderer.setView(camera);
        TiledMap map = mapRenderer.getMap();
        int totalLayers = map.getLayers().getCount();


        List<Integer> beforeLayers = new ArrayList<>();
        List<Integer> afterLayers = new ArrayList<>();

        // Split layers based on your custom "layer" property
        for (int i = 0; i < map.getLayers().getCount(); i++) {
            MapLayer layer = map.getLayers().get(i);
            int layerId = layer.getProperties().get("layer", i, Integer.class); // fallback to index if missing

            if (layerId <= insertIndex) {
                beforeLayers.add(i);
            } else {
                afterLayers.add(i);
            }
        }

        // Convert Lists to int[]
        int[] beforeArray = beforeLayers.stream().mapToInt(Integer::intValue).toArray();
        int[] afterArray = afterLayers.stream().mapToInt(Integer::intValue).toArray();

        batch.begin();
        // Render layers before entities
        mapRenderer.render(beforeArray);

        batch.end();

        // Render entities
        batch.setProjectionMatrix(camera.combined);

        for (Entity entity : entities) {
            batch.begin();
            entity.render(batch);
            batch.end();
        }

        batch.begin();
        // Render remaining layers
        mapRenderer.render(afterArray);

        batch.end();
    }
}
