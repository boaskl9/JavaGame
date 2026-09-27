package com.game.world;

import com.game.integration.WorldManager;
import com.game.systems.breakable.BreakableObjectFactory;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.GatewayEntity;
import com.game.systems.furniture.FurnitureManager;
import com.game.systems.level.LevelData;
import com.game.systems.level.LevelSource;
import com.game.systems.level.TiledMapLevelSource;

/**
 * The single way to build a {@link LevelInstance}.
 */
public final class LevelInstanceFactory {

    /**
     * AUTHORITATIVE: this machine owns the level's gameplay objects (single-player or host).
     * REPLICA: gameplay objects arrive from the host over the network (client), so only the
     * static parts of the level are built locally.
     */
    public enum Mode {
        AUTHORITATIVE,
        REPLICA
    }

    private static final String[] BREAKABLE_TYPES = {"pot", "clay_pot"};

    private LevelInstanceFactory() {
    }

    public static LevelInstance create(String levelPath, Mode mode, WorldManager.Listener... listeners) {
        return create(new TiledMapLevelSource(levelPath), mode, listeners);
    }

    /**
     * Build a level. Listeners are attached before any objects are added, so they see
     * every object the level is populated with.
     */
    public static LevelInstance create(LevelSource source, Mode mode, WorldManager.Listener... listeners) {
        String levelId = source.getLevelName();
        System.out.println("LevelInstanceFactory: Building " + levelId + " (" + mode + ")");

        LevelData levelData = source.getLevelData();
        WorldManager world = new WorldManager(levelData.getWidth(), levelData.getHeight());

        SpatialQuery collisionSystem = new SpatialQuery();
        source.loadCollision(collisionSystem);
        world.setCollisionSystem(collisionSystem);
        world.buildGridPathfinder(source.getTiledMap());

        for (WorldManager.Listener listener : listeners) {
            world.addListener(listener);
        }

        LevelInstance instance = new LevelInstance(levelId, source, levelData, world);

        addGateways(world, levelData);
        if (mode == Mode.AUTHORITATIVE) {
            addBreakables(world, levelData);
            FurnitureManager.getInstance().loadFurnitureIntoWorld(levelId, world);
        }

        return instance;
    }

    private static void addGateways(WorldManager world, LevelData levelData) {
        for (LevelData.LevelObject obj : levelData.getObjectsByType("gateway")) {
            String targetLevel = obj.getPropertyString("targetLevel", null);
            String targetSpawn = obj.getPropertyString("targetSpawn", null);

            if (targetLevel != null) {
                world.addGameObject(new GatewayEntity(
                    obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight(),
                    targetLevel, targetSpawn
                ));
            }
        }
    }

    private static void addBreakables(WorldManager world, LevelData levelData) {
        for (String breakableType : BREAKABLE_TYPES) {
            for (LevelData.LevelObject obj : levelData.getObjectsByType(breakableType)) {
                BreakableEntity breakable = BreakableObjectFactory.create(breakableType, obj.getX(), obj.getY());
                if (breakable != null) {
                    world.addGameObject(breakable);
                }
            }
        }
    }
}
