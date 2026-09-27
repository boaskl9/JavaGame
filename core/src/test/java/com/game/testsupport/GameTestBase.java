package com.game.testsupport;

import com.game.integration.WorldItemManager;
import com.game.systems.breakable.BreakableObjectRegistry;
import com.game.systems.entity.PlayerManager;
import com.game.systems.item.TestItems;
import com.game.systems.loot.LootSystem;
import com.game.util.SingletonManager;
import com.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base for tests that run the game simulation: fresh singletons and registries per test.
 */
public abstract class GameTestBase extends GdxTest {
    public static final String START_LEVEL = "Maps/prototype.tmx";
    public static final String OTHER_LEVEL = "Maps/WestArea.tmx";
    public static final float DT = 1f / 60f;

    @BeforeEach
    void resetGameState() {
        SingletonManager.resetAllGameSingletons();
        TestItems.registerTestItems();
        BreakableObjectRegistry.loadConfigs();
    }

    /** A single-player (or future host) world, already standing in a level. */
    public static TestWorld newAuthoritativeWorld(String level) {
        TestWorld world = new TestWorld(false);
        LootSystem.initialize(world.items);
        world.world.changeLevel(level, null);
        return world;
    }

    /** One machine's game: the simulation plus the fake screen it reports to. */
    public static class TestWorld {
        public final WorldItemManager items = new WorldItemManager();
        public final PlayerManager players = new PlayerManager();
        public final TestPresenter presenter = new TestPresenter();
        public final GameWorld world;

        public TestWorld(boolean clientMode) {
            world = new GameWorld(clientMode, items, players, presenter);
        }

        public void step(float delta) {
            world.updateNetwork(delta);
            world.update(delta);
        }

        public void run(float seconds) {
            for (float t = 0; t < seconds; t += DT) {
                step(DT);
            }
        }
    }
}
