package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.integration.WorldManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.ProjectileEntity;
import com.game.systems.entity.entities.enemies.Axolot;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.LevelSpots;
import com.game.testsupport.MultiplayerRig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enemy projectiles: the host simulates and resolves them, guests see a copy fly.
 */
class ProjectileMultiplayerTest extends GameTestBase {
    private MultiplayerRig rig;
    private TestWorld host;
    private TestWorld client;

    @BeforeEach
    void startSession() {
        rig = new MultiplayerRig(START_LEVEL);
        host = rig.host;
        client = rig.join("Bob");
    }

    @AfterEach
    void endSession() {
        rig.close();
    }

    @Test
    void anAxolotlsSpitIsSeenByTheGuestAndHurtsThem() {
        // Move the guest away from the host (down: there are pots to the sides) so the axolotl targets the guest
        PlayerEntity me = client.world.getLocalPlayer();
        me.getTransform().setPosition(me.getTransform().getX(), me.getTransform().getY() - 48);
        float targetY = me.getTransform().getY();
        rig.runUntil(() -> !remotePlayers(host).isEmpty()
            && Math.abs(remotePlayers(host).get(0).getTransform().getY() - targetY) < 1f, "host's copy follows the guest");

        WorldManager hostWorld = host.world.getCurrentInstance().getWorld();
        Vector2 spot = LevelSpots.clearShotSpot(hostWorld, me.getTransform().getX(), me.getTransform().getY(), 40);
        Vector2 hostPos = host.world.getLocalPlayer().getTransform().getPosition();
        assertTrue(spot.dst(me.getTransform().getPosition()) < spot.dst(hostPos), "the guest is the nearest target");
        hostWorld.addGameObject(new Axolot(hostWorld, spot.x, spot.y));

        rig.runUntil(() -> objectsIn(client).stream()
            .anyMatch(o -> o instanceof ProjectileEntity p && p.isNetworkControlled()), "guest sees the spit fly");

        int fullHealth = me.getHealth();
        rig.runUntil(() -> me.getHealth() < fullHealth, "the spit hurts the guest", 10000);
        rig.runUntil(() -> objectsIn(client).stream().noneMatch(o -> o instanceof ProjectileEntity),
            "spent spit removed for the guest");
    }

    private static List<GameObject> objectsIn(TestWorld world) {
        return world.world.getCurrentInstance().getWorld().getGameObjects();
    }

    private static List<PlayerEntity> remotePlayers(TestWorld world) {
        return objectsIn(world).stream()
            .filter(o -> o instanceof PlayerEntity p && p.isNetworkControlled())
            .map(o -> (PlayerEntity) o).toList();
    }
}
