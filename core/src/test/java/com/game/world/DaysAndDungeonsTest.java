package com.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.GatewayEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.level.LevelData;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-player days (sleeping, passing out) and the daily cave dungeon behind house1.
 */
class DaysAndDungeonsTest extends GameTestBase {
    private static final String HOUSE = "Maps/house1.tmx";
    private static final String CAVE_TODAY = "dungeon:cave:1";

    // ========== Days ==========

    @Test
    void whenTimeRunsOutThePlayerPassesOutAndWakesUpAtHomeTheNextDay() {
        TestWorld game = newAuthoritativeWorld(OTHER_LEVEL);
        game.world.getDayCycle().setDayLength(1f);
        PlayerEntity player = game.world.getLocalPlayer();
        player.getHealthComponent().setHealth(5);

        game.run(1.2f);

        assertEquals(2, game.world.getDayCycle().getDay());
        assertEquals(GameWorld.START_LEVEL, game.world.getCurrentInstance().getLevelId());
        assertEquals(game.world.getCurrentInstance().getSpawnPosition(null), player.getTransform().getPosition());
        assertEquals(player.getMaxHealth(), player.getHealth(), "a night's rest");
        assertTrue(game.presenter.lastMessage().startsWith("You passed out"));
    }

    @Test
    void sleepingInABedStartsTheNextDay() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        LevelData.LevelObject bed = new LevelData.LevelObject("bed", "bed", player.getTransform().getX(), player.getTransform().getY());
        bed.setSize(16, 16);
        game.world.getCurrentInstance().getLevelData().addObject(bed);

        assertTrue(game.world.interactWithLevel());

        assertEquals(2, game.world.getDayCycle().getDay());
        assertEquals("06:00", game.world.getDayCycle().getClockText());
        assertFalse(game.world.isLocalPlayerSleeping(), "awake again in the morning");
        assertFalse(player.isFrozen());
        assertEquals("Day 2", game.presenter.lastMessage());
    }

    @Test
    void thereIsNothingToInteractWithAwayFromBeds() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        assertFalse(game.world.interactWithLevel());
        assertEquals(1, game.world.getDayCycle().getDay());
    }

    @Test
    void theDayAndTheWorldSeedAreSaved() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        game.world.sleep();
        game.run(0.5f);
        long seed = game.world.getDayCycle().getWorldSeed();
        com.game.save.SaveManager.getInstance().initialize(game.world.getLocalPlayer(),
            game.world.getLocalPlayer().getInventory(), com.game.systems.furniture.FurnitureManager.getInstance(), game.items);
        com.game.save.SaveManager.getInstance().save("days_test");

        TestWorld loaded = newAuthoritativeWorld(START_LEVEL);
        com.game.save.SaveManager.getInstance().initialize(loaded.world.getLocalPlayer(),
            loaded.world.getLocalPlayer().getInventory(), com.game.systems.furniture.FurnitureManager.getInstance(), loaded.items);
        com.game.save.SaveManager.getInstance().applySaveData(com.game.save.SaveManager.getInstance().load("days_test"));
        com.game.save.SaveManager.getInstance().deleteSave("days_test");

        assertEquals(2, loaded.world.getDayCycle().getDay());
        assertEquals(seed, loaded.world.getDayCycle().getWorldSeed());
        assertEquals(0.5f, loaded.world.getDayCycle().getElapsed(), 0.05f);
    }

    // ========== The daily dungeon ==========

    @Test
    void theHouseDoorLeadsIntoTodaysCaveDungeon() {
        TestWorld game = inHouse();
        PlayerEntity player = game.world.getLocalPlayer();

        standOn(player, gatewayTo(game, "dungeon:cave"));
        game.run(0.1f);

        LevelInstance dungeon = game.world.getCurrentInstance();
        assertEquals(CAVE_TODAY, dungeon.getLevelId());
        Vector2 at = player.getTransform().getPosition();
        assertEquals(dungeon.getSpawnPosition(null), at, "at the entrance");
        assertTrue(dungeon.getWorld().isPositionWalkable(at.x + 4, at.y, 8, 4), "the entrance is walkable");
        assertFalse(enemies(dungeon).isEmpty(), "monsters live here");

        game.run(0.5f);
        assertEquals(CAVE_TODAY, game.world.getCurrentInstance().getLevelId(),
            "arriving on the exit doesn't send you straight back out");
    }

    @Test
    void theDungeonExitLeadsBackIntoTheHouse() {
        TestWorld game = inHouse();
        PlayerEntity player = game.world.getLocalPlayer();
        standOn(player, gatewayTo(game, "dungeon:cave"));
        game.run(0.1f);
        GatewayEntity exit = gatewayTo(game, "dungeon:exit");

        player.getTransform().setPosition(player.getTransform().getX(), player.getTransform().getY() + 48); // Step off it
        game.run(0.1f);
        standOn(player, exit);
        game.run(0.1f);

        assertEquals(HOUSE, game.world.getCurrentInstance().getLevelId());
        assertEquals(game.world.getCurrentInstance().getSpawnPosition("spawn_point2"), player.getTransform().getPosition());
    }

    @Test
    void theLayoutIsTheSameAllDayAndNewTheNextDay() {
        TestWorld game = inHouse();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");
        List<String> firstVisit = describe(game.world.getCurrentInstance());
        game.world.leaveDungeon();

        game.world.enterDungeon("dungeon:cave", "spawn_point2");
        assertEquals(firstVisit, describe(game.world.getCurrentInstance()), "same rooms and monsters when you come back");

        game.world.sleep(); // Day 2
        game.world.changeLevel(HOUSE, "spawn_point2");
        game.world.enterDungeon("dungeon:cave", "spawn_point2");
        assertEquals("dungeon:cave:2", game.world.getCurrentInstance().getLevelId());
        assertNotEquals(firstVisit, describe(game.world.getCurrentInstance()), "a new dungeon every day");
    }

    @Test
    void dyingInADungeonKeepsYouOutForTheRestOfTheDay() {
        TestWorld game = inHouse();
        PlayerEntity player = game.world.getLocalPlayer();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");

        player.damage(player.getMaxHealth());
        game.run(GameWorld.RESPAWN_DELAY + 0.1f);

        assertEquals(GameWorld.START_LEVEL, game.world.getCurrentInstance().getLevelId(), "carried out, home");
        assertTrue(game.world.isExhausted("dungeon:cave"));

        game.world.changeLevel(HOUSE, "spawn_point2");
        standOn(player, gatewayTo(game, "dungeon:cave"));
        game.run(0.5f);
        assertEquals(HOUSE, game.world.getCurrentInstance().getLevelId(), "the door won't take you in");
        assertEquals("You're too worn out to go back in there today.", game.presenter.lastMessage());
        assertEquals(1, game.presenter.messages.stream().filter(m -> m.startsWith("You're too worn out")).count(),
            "told once, not every frame while standing there");

        game.world.sleep();
        assertFalse(game.world.isExhausted("dungeon:cave"));
        game.world.changeLevel(HOUSE, "spawn_point2");
        assertTrue(game.world.enterDungeon("dungeon:cave", "spawn_point2"), "a new day, a new chance");
    }

    @Test
    void monstersYouKilledStayDeadForTheRestOfTheDay() {
        TestWorld game = inHouse();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");
        LevelInstance dungeon = game.world.getCurrentInstance();
        int monsters = enemies(dungeon).size();
        enemies(dungeon).get(0).damage(10_000);
        game.run(0.1f);
        assertEquals(monsters - 1, enemies(dungeon).size());

        game.world.leaveDungeon();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");

        assertSame(dungeon, game.world.getCurrentInstance(), "the same dungeon, kept for the day");
        assertEquals(monsters - 1, enemies(game.world.getCurrentInstance()).size());
    }

    @Test
    void yesterdaysDungeonIsUnloadedInTheMorning() {
        TestWorld game = inHouse();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");
        game.world.leaveDungeon();
        assertTrue(hasLevel(game, CAVE_TODAY), "kept while it's still today");

        game.world.sleep();

        assertFalse(hasLevel(game, CAVE_TODAY));
    }

    @Test
    void sleepingInsideTheDungeonAlsoUnloadsIt() {
        TestWorld game = inHouse();
        game.world.enterDungeon("dungeon:cave", "spawn_point2");

        game.world.sleep(); // e.g. passing out down there

        assertEquals(GameWorld.START_LEVEL, game.world.getCurrentInstance().getLevelId());
        assertFalse(hasLevel(game, CAVE_TODAY));
    }

    private static boolean hasLevel(TestWorld game, String levelId) {
        return game.world.getInstances().stream().anyMatch(i -> i.getLevelId().equals(levelId));
    }

    // ========== Helpers ==========

    private static TestWorld inHouse() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        game.world.changeLevel(HOUSE, "spawn_point2");
        return game;
    }

    private static GatewayEntity gatewayTo(TestWorld game, String target) {
        for (GameObject obj : game.world.getCurrentInstance().getWorld().getGameObjects()) {
            if (obj instanceof GatewayEntity gateway && target.equals(gateway.getTargetLevel())) return gateway;
        }
        throw new AssertionError("No gateway to " + target + " in " + game.world.getCurrentInstance().getLevelId());
    }

    /** Move the player so their feet are on the gateway. */
    private static void standOn(PlayerEntity player, GatewayEntity gateway) {
        Rectangle target = gateway.getComponent(ColliderComponent.class).getBounds(gateway);
        Rectangle feet = player.getComponent(ColliderComponent.class).getBounds(player);
        float offsetX = feet.x - player.getTransform().getX();
        float offsetY = feet.y - player.getTransform().getY();
        player.getTransform().setPosition(
            target.x + target.width / 2 - feet.width / 2 - offsetX,
            target.y + target.height / 2 - feet.height / 2 - offsetY);
    }

    private static List<EnemyEntity> enemies(LevelInstance level) {
        return level.getWorld().getGameObjects().stream()
            .filter(o -> o instanceof EnemyEntity).map(o -> (EnemyEntity) o).collect(Collectors.toList());
    }

    /** The dungeon's size, entrance and monsters. */
    private static List<String> describe(LevelInstance level) {
        List<String> parts = new java.util.ArrayList<>();
        parts.add(level.getLevelData().getWidth() + "x" + level.getLevelData().getHeight());
        parts.add("entrance " + level.getSpawnPosition(null));
        for (EnemyEntity enemy : enemies(level)) {
            parts.add(enemy.getClass().getSimpleName() + " " + enemy.getTransform().getPosition());
        }
        return parts;
    }
}
