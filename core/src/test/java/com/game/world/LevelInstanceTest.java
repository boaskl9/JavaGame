package com.game.world;

import com.badlogic.gdx.math.Vector2;
import com.game.integration.WorldItemManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.GatewayEntity;
import com.game.systems.entity.entities.enemies.LizardEnemy;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelInstanceTest extends GameTestBase {

    @Test
    void authoritativeLevelsContainGatewaysAndBreakables() {
        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.AUTHORITATIVE);
        assertTrue(count(level, GatewayEntity.class) > 0);
        assertTrue(count(level, BreakableEntity.class) > 0);
    }

    @Test
    void replicaLevelsOnlyContainStaticParts() {
        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.REPLICA);
        assertTrue(count(level, GatewayEntity.class) > 0, "gateways are needed to walk between levels");
        assertEquals(0, count(level, BreakableEntity.class), "breakables come from the host");
    }

    @Test
    void listenersSeeEveryObjectTheLevelIsPopulatedWith() {
        int[] added = {0};
        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.AUTHORITATIVE,
            new com.game.integration.WorldManager.Listener() {
                @Override
                public void onObjectAdded(GameObject obj) {
                    added[0]++;
                }

                @Override
                public void onObjectRemoved(GameObject obj) {
                }
            });
        assertEquals(level.getWorld().getGameObjects().size(), added[0]);
    }

    @Test
    void deadEnemiesAreRemovedAfterTheUpdate() {
        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.AUTHORITATIVE);
        LizardEnemy lizard = new LizardEnemy(level.getWorld(), 100, 100);
        level.getWorld().addGameObject(lizard);

        lizard.damage(1000);
        level.update(DT, new WorldItemManager());

        assertFalse(level.getWorld().contains(lizard));
    }

    @Test
    void lootDropsIntoTheLevelThatIsBeingUpdated() {
        WorldItemManager items = new WorldItemManager();
        com.game.systems.loot.LootSystem.initialize(items);
        items.setCurrentLevel("somewhere else");

        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.AUTHORITATIVE);
        BreakableEntity pot = (BreakableEntity) level.getWorld().getGameObjects().stream()
            .filter(o -> o instanceof BreakableEntity).findFirst().orElseThrow();

        // Break it during the level's own update, the way a player's attack would
        level.getWorld().addGameObject(new GameObject() {
            @Override
            public void update(float delta) {
                if (pot.isAlive()) pot.damage(1000);
            }
        });
        for (int i = 0; i < 5; i++) {
            level.update(DT, items);
        }

        assertTrue(items.getItems("somewhere else").isEmpty(), "loot must not land in another level");
        assertEquals("somewhere else", items.getCurrentLevel(), "the active level is restored afterwards");
    }

    @Test
    void unknownSpawnPointsFallBackToTheDefault() {
        LevelInstance level = LevelInstanceFactory.create(START_LEVEL, LevelInstanceFactory.Mode.REPLICA);
        Vector2 fallback = level.getSpawnPosition("no_such_spawn");
        assertEquals(level.getSpawnPosition(null), fallback);
        assertEquals(0, fallback.x % 16, "spawns are snapped to the tile grid");
    }

    private static int count(LevelInstance level, Class<?> type) {
        List<GameObject> objects = level.getWorld().getGameObjects();
        return (int) objects.stream().filter(type::isInstance).count();
    }
}
