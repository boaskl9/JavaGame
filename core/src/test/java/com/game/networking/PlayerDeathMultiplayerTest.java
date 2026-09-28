package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.save.PlayerData;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.MultiplayerRig;
import com.game.world.GameWorld;
import com.game.world.LevelInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Each player is knocked out and respawns on their own machine; the others just see it happen.
 */
class PlayerDeathMultiplayerTest extends GameTestBase {
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
    void aGuestRespawnsAtTheStartLevelOnTheirOwn() {
        guest.world.changeLevel(OTHER_LEVEL, null);
        rig.runUntil(() -> hostCopy() != null && OTHER_LEVEL.equals(levelOf(host, hostCopy())), "host moved Bob's copy");

        guest.world.getLocalPlayer().damage(guest.world.getLocalPlayer().getMaxHealth());
        rig.runUntil(() -> !hostCopy().isAlive(), "host sees Bob knocked out");

        rig.runUntil(() -> !guest.world.isLocalPlayerDead(), "Bob respawns", 5000);
        assertEquals(GameWorld.START_LEVEL, guest.world.getCurrentInstance().getLevelId());

        Vector2 spawn = guest.world.getCurrentInstance().getSpawnPosition(null);
        rig.runUntil(() -> hostCopy().isAlive() && START_LEVEL.equals(levelOf(host, hostCopy()))
            && hostCopy().getTransform().getPosition().dst(spawn) < 1f, "host's copy is back, at the spawn");

        assertEquals(0, host.presenter.deaths, "the host wasn't affected");
        assertTrue(host.world.getLocalPlayer().isAlive());
    }

    @Test
    void othersSeeThePlayerFallAndComeBack() {
        rig.runUntil(() -> puppetOfHost() != null, "Bob sees the host");
        PlayerEntity hostPlayer = host.world.getLocalPlayer();
        hostPlayer.getTransform().setPosition(hostPlayer.getTransform().getX() + 32, hostPlayer.getTransform().getY());

        hostPlayer.damage(hostPlayer.getMaxHealth());
        rig.runUntil(() -> !puppetOfHost().isAlive(), "Bob sees the host knocked out");
        assertEquals(1, guest.presenter.deathAnimations, "a death puff on Bob's screen too");

        rig.runUntil(() -> puppetOfHost().isAlive(), "host respawns", 5000);
        Vector2 spawn = host.world.getCurrentInstance().getSpawnPosition(null);
        rig.runUntil(() -> puppetOfHost().getTransform().getPosition().dst(spawn) < 1f, "Bob sees the host at the spawn");
        assertEquals(0, guest.presenter.deaths);
    }

    @Test
    void aGuestWhoLeavesWhileKnockedOutIsSavedAsRespawned() {
        guest.world.changeLevel(OTHER_LEVEL, null);
        rig.runUntil(() -> hostCopy() != null && OTHER_LEVEL.equals(levelOf(host, hostCopy())), "host moved Bob's copy");
        guest.world.getLocalPlayer().damage(guest.world.getLocalPlayer().getMaxHealth());
        rig.runUntil(() -> !hostCopy().isAlive(), "host sees Bob knocked out");

        rig.disconnect(guest); // Before the respawn (e.g. the game crashed)
        rig.runUntil(() -> hostCopy() == null, "Bob left");

        PlayerData saved = MultiplayerRig.savedCharacter("Bob");
        LevelInstance start = host.world.getOrCreateInstance(GameWorld.START_LEVEL);
        assertEquals(GameWorld.START_LEVEL, saved.levelId);
        assertEquals(start.getSpawnPosition(null), new Vector2(saved.x, saved.y));
        assertEquals(saved.maxHealth, saved.currentHealth);
        assertTrue(saved.maxHealth > 0);
    }

    // ========== Helpers ==========

    private PlayerEntity hostCopy() {
        for (PlayerEntity player : host.players.getAllPlayers()) {
            if (player.isNetworkControlled()) return player;
        }
        return null;
    }

    private PlayerEntity puppetOfHost() {
        for (PlayerEntity player : guest.players.getAllPlayers()) {
            if (player.isNetworkControlled() && player.getPlayerId() == HostSession.HOST_PLAYER_ID) return player;
        }
        return null;
    }

    private static String levelOf(TestWorld world, PlayerEntity player) {
        for (LevelInstance level : world.world.getInstances()) {
            if (level.getWorld() == player.getWorld()) return level.getLevelId();
        }
        return null;
    }
}
