package com.game.world;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ProjectileEntity;
import com.game.systems.level.LevelData;
import com.game.systems.level.LevelSource;

/**
 * One loaded level: its map, its world simulation and everything in it.
 * Built exclusively by {@link LevelInstanceFactory}, so every copy of a level is complete
 * (gateways, breakables, furniture) no matter who asked for it.
 *
 * The host may keep several instances alive at once (one per level with a player in it);
 * a client only ever has the instance it is standing in.
 */
public class LevelInstance {
    private final String levelId;
    private final LevelSource source;
    private final LevelData levelData;
    private final WorldManager world;

    LevelInstance(String levelId, LevelSource source, LevelData levelData, WorldManager world) {
        this.levelId = levelId;
        this.source = source;
        this.levelData = levelData;
        this.world = world;
    }

    /**
     * Advance the simulation of this level.
     */
    public void update(float delta, WorldItemManager items) {
        String previousActive = items.getCurrentLevel();
        items.setCurrentLevel(levelId); // Loot dropped during this update lands in this level
        world.update(delta);
        items.update(levelId, delta);
        items.setCurrentLevel(previousActive);
        removeFinishedEntities();
    }

    /**
     * Remove dead enemies, fully broken breakables and spent projectiles from the world.
     * Removal notifies world listeners, which despawns them on clients.
     */
    private void removeFinishedEntities() {
        for (GameObject obj : world.getGameObjects()) {
            if (obj.isActive()) continue;
            if (obj instanceof EnemyEntity || obj instanceof BreakableEntity || obj instanceof ProjectileEntity) {
                world.removeGameObject(obj);
            }
        }
    }

    /**
     * Resolve a spawn point to a world position, snapped to the tile grid.
     * Falls back to the default spawn point when the named one doesn't exist.
     */
    public Vector2 getSpawnPosition(String spawnPointName) {
        LevelData.SpawnPoint spawn = null;
        if (spawnPointName != null) {
            spawn = levelData.getSpawnPoint(spawnPointName);
            if (spawn == null) {
                System.out.println("LevelInstance: Spawn point '" + spawnPointName + "' not found in " + levelId + ", using default");
            }
        }
        if (spawn == null) {
            spawn = levelData.getDefaultSpawnPoint();
        }

        float x = spawn != null ? spawn.getX() : 50;
        float y = spawn != null ? spawn.getY() : 750;

        int tile = world.getTileSize();
        return new Vector2((int) (x / tile) * tile, (int) (y / tile) * tile);
    }

    public String getLevelId() {
        return levelId;
    }

    public WorldManager getWorld() {
        return world;
    }

    public TiledMap getTiledMap() {
        return source.getTiledMap();
    }

    public LevelData getLevelData() {
        return levelData;
    }

    public LevelSource getSource() {
        return source;
    }

    /**
     * Whether another machine can rebuild this level from its ID alone
     * (Tiled maps and the seeded daily dungeons; not dungeons made with the debug console).
     */
    public boolean isShareable() {
        return source.isShareable();
    }

    public void dispose() {
        source.dispose();
    }
}
