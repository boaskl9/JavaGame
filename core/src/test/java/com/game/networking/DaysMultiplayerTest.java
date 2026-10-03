package com.game.networking;

import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.MultiplayerRig;
import com.game.world.GameWorld;
import com.game.world.LevelInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The host's clock is everyone's clock; the day ends when everyone sleeps or time runs out;
 * the daily dungeon is the same for everyone and shared.
 */
class DaysMultiplayerTest extends GameTestBase {
    private static final String CAVE_TODAY = "dungeon:cave:1";

    private MultiplayerRig rig;
    private TestWorld host;
    private TestWorld guest;

    @BeforeEach
    void startSession() {
        rig = new MultiplayerRig(START_LEVEL);
        host = rig.host;
        guest = rig.join("Bob");
    }

    @AfterEach
    void endSession() {
        rig.close();
    }

    @Test
    void guestsUseTheHostsClockAndWorldSeed() {
        host.world.getDayCycle().setDayLength(100f);
        rig.runFor(1.5f);

        assertEquals(host.world.getDayCycle().getWorldSeed(), guest.world.getDayCycle().getWorldSeed());
        assertEquals(1, guest.world.getDayCycle().getDay());
        assertEquals(100f, guest.world.getDayCycle().getDayLength());
        assertEquals(host.world.getDayCycle().getElapsed(), guest.world.getDayCycle().getElapsed(), 0.5f);
    }

    @Test
    void theDayEndsOnlyOnceEveryoneIsAsleep() {
        host.world.sleep();
        rig.runUntil(() -> guest.world.getPlayersAsleep() == 1, "Bob hears the host went to bed");
        assertEquals(2, guest.world.getPlayersTotal());
        assertEquals(1, host.world.getDayCycle().getDay(), "not yet: Bob is awake");
        assertTrue(host.world.getLocalPlayer().isFrozen());

        guest.world.sleep();
        rig.runUntil(() -> guest.world.getDayCycle().getDay() == 2 && !guest.world.isLocalPlayerSleeping(), "Bob wakes up on day 2");

        assertEquals(2, host.world.getDayCycle().getDay());
        assertFalse(host.world.isLocalPlayerSleeping());
        assertFalse(guest.world.isLocalPlayerSleeping());
        assertEquals("Day 2", guest.presenter.lastMessage());
    }

    @Test
    void gettingUpAgainKeepsTheDayGoing() {
        guest.world.sleep();
        rig.runUntil(() -> host.world.getPlayersAsleep() == 1, "host hears Bob went to bed");
        guest.world.wakeUp();
        rig.runUntil(() -> host.world.getPlayersAsleep() == 0, "host hears Bob got up");

        host.world.sleep();
        rig.runFor(0.3f);
        assertEquals(1, host.world.getDayCycle().getDay());
    }

    @Test
    void whenTimeRunsOutEveryonePassesOutAndWakesUpAtHome() {
        guest.world.changeLevel(OTHER_LEVEL, null);
        rig.runFor(0.2f);
        host.world.getDayCycle().setDayLength(host.world.getDayCycle().getElapsed() + 0.5f);

        rig.runUntil(() -> guest.world.getDayCycle().getDay() == 2 && guest.presenter.lastMessage() != null, "day 2");
        assertEquals(GameWorld.START_LEVEL, guest.world.getCurrentInstance().getLevelId());
        assertTrue(guest.presenter.lastMessage().startsWith("You passed out"));
        rig.runUntil(() -> START_LEVEL.equals(levelOf(host, hostCopyOfGuest())), "host moved Bob's copy home");
    }

    @Test
    void everyoneGetsTheSameDungeonAndMeetsTheSameMonsters() {
        guest.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        guest.world.enterDungeon("dungeon:cave", "spawn_point2");
        assertEquals(CAVE_TODAY, guest.world.getCurrentInstance().getLevelId());

        rig.runUntil(() -> hostInstance(CAVE_TODAY) != null, "the host builds the same dungeon");
        LevelInstance hostDungeon = hostInstance(CAVE_TODAY);
        LevelInstance guestDungeon = guest.world.getCurrentInstance();
        assertEquals(hostDungeon.getLevelData().getWidth(), guestDungeon.getLevelData().getWidth());
        assertEquals(hostDungeon.getLevelData().getHeight(), guestDungeon.getLevelData().getHeight());
        assertEquals(hostDungeon.getSpawnPosition(null), guestDungeon.getSpawnPosition(null));

        int monsters = enemies(hostDungeon).size();
        assertTrue(monsters > 0);
        rig.runUntil(() -> enemies(guest.world.getCurrentInstance()).size() == monsters, "Bob sees the host's monsters");

        // The host walks in too and joins Bob in the same dungeon
        host.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        host.world.enterDungeon("dungeon:cave", "spawn_point2");
        assertSame(hostDungeon, host.world.getCurrentInstance());
    }

    @Test
    void monstersStayDeadWhenEveryoneLeavesAndComesBack() {
        guest.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        guest.world.enterDungeon("dungeon:cave", "spawn_point2");
        rig.runUntil(() -> hostInstance(CAVE_TODAY) != null, "host builds the dungeon");
        LevelInstance hostDungeon = hostInstance(CAVE_TODAY);
        int monsters = enemies(hostDungeon).size();
        rig.runUntil(() -> enemies(guest.world.getCurrentInstance()).size() == monsters, "Bob sees the monsters");

        ((EnemyEntity) enemies(hostDungeon).get(0)).damage(10_000); // As if Bob killed it
        rig.runUntil(() -> enemies(guest.world.getCurrentInstance()).size() == monsters - 1, "it dies for Bob too");

        guest.world.leaveDungeon();
        rig.runUntil(() -> "Maps/house1.tmx".equals(levelOf(host, hostCopyOfGuest())), "Bob left the dungeon");
        assertSame(hostDungeon, hostInstance(CAVE_TODAY), "the host keeps today's dungeon");

        guest.world.enterDungeon("dungeon:cave", "spawn_point2");
        rig.runFor(0.5f);
        assertEquals(monsters - 1, enemies(guest.world.getCurrentInstance()).size(), "still dead when Bob comes back");
    }

    @Test
    void theHostUnloadsYesterdaysDungeonOnceTheLastPlayerIsOut() {
        guest.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        guest.world.enterDungeon("dungeon:cave", "spawn_point2");
        rig.runUntil(() -> hostInstance(CAVE_TODAY) != null, "host builds the dungeon");

        host.world.sleep();
        guest.world.sleep();
        rig.runUntil(() -> host.world.getDayCycle().getDay() == 2, "day 2");
        rig.runUntil(() -> hostInstance(CAVE_TODAY) == null, "host unloads yesterday's dungeon");
        assertEquals(GameWorld.START_LEVEL, guest.world.getCurrentInstance().getLevelId());
    }

    @Test
    void aGuestWhoDiesInTheDungeonIsLockedOutButTheHostIsNot() {
        guest.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        guest.world.enterDungeon("dungeon:cave", "spawn_point2");
        PlayerEntity bob = guest.world.getLocalPlayer();
        bob.damage(bob.getMaxHealth());
        rig.runUntil(() -> !guest.world.isLocalPlayerDead(), "Bob respawns", 5000);

        assertTrue(guest.world.isExhausted("dungeon:cave"));
        assertFalse(host.world.isExhausted("dungeon:cave"));
        guest.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        assertFalse(guest.world.enterDungeon("dungeon:cave", "spawn_point2"));

        host.world.changeLevel("Maps/house1.tmx", "spawn_point2");
        assertTrue(host.world.enterDungeon("dungeon:cave", "spawn_point2"));
    }

    // ========== Helpers ==========

    private LevelInstance hostInstance(String levelId) {
        return host.world.getInstances().stream().filter(i -> i.getLevelId().equals(levelId)).findFirst().orElse(null);
    }

    private PlayerEntity hostCopyOfGuest() {
        for (PlayerEntity player : host.players.getAllPlayers()) {
            if (player.isNetworkControlled()) return player;
        }
        return null;
    }

    private static String levelOf(TestWorld world, PlayerEntity player) {
        if (player == null) return null;
        for (LevelInstance level : world.world.getInstances()) {
            if (level.getWorld() == player.getWorld()) return level.getLevelId();
        }
        return null;
    }

    private static List<GameObject> enemies(LevelInstance level) {
        return level.getWorld().getGameObjects().stream().filter(o -> o instanceof EnemyEntity).collect(Collectors.toList());
    }
}
