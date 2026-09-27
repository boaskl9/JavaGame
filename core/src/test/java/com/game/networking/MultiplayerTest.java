package com.game.networking;

import com.game.save.PlayerData;
import com.game.save.SaveManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.enemies.LizardEnemy;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.MultiplayerRig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end multiplayer behaviour: a real host and client over localhost.
 */
class MultiplayerTest extends GameTestBase {
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

    // ========== Joining ==========

    @Test
    void clientJoinsTheHostsLevelAndBothSeeEachOther() {
        assertEquals(START_LEVEL, client.world.getCurrentInstance().getLevelId());

        rig.runUntil(() -> remotePlayers(host).size() == 1 && remotePlayers(client).size() == 1,
            "host and client see each other");

        assertEquals(client.world.getLocalPlayer().getPlayerId(), remotePlayers(host).get(0).getPlayerId());
        assertEquals(HostSession.HOST_PLAYER_ID, remotePlayers(client).get(0).getPlayerId());
    }

    // ========== Owner-authoritative movement ==========

    @Test
    void clientMovementIsNeverCorrectedAndTheHostFollowsIt() {
        PlayerEntity me = client.world.getLocalPlayer();
        float startX = me.getTransform().getX();

        client.presenter.input.move(1, 0);
        float lastX = startX;
        long end = System.currentTimeMillis() + 800;
        while (System.currentTimeMillis() < end) {
            rig.step();
            float x = me.getTransform().getX();
            assertTrue(x >= lastX, "client was pulled backwards: " + lastX + " -> " + x);
            lastX = x;
        }
        client.presenter.input.move(0, 0);
        assertTrue(lastX > startX + 10, "client should have walked right");

        float finalX = lastX;
        rig.runUntil(() -> Math.abs(remotePlayers(host).get(0).getTransform().getX() - finalX) < 1f,
            "host's copy of the client catches up");
    }

    // ========== Replication ==========

    @Test
    void enemiesSpawnedOnTheHostAppearOnTheClientAndFollowIt() {
        LizardEnemy lizard = spawnLizardNearHost(80);
        rig.runUntil(() -> clientCopyOf(lizard) != null, "lizard replicated to client");

        EnemyEntity copy = clientCopyOf(lizard);
        assertTrue(copy.isNetworkControlled(), "client copy must be a puppet");

        rig.runFor(1.5f);
        float dist = copy.getTransform().getPosition().dst(lizard.getTransform().getPosition());
        assertTrue(dist < 8f, "client copy should track the host's lizard, but was " + dist + "px off");
    }

    @Test
    void killedEnemiesDisappearForTheClientWithADeathAnimation() {
        LizardEnemy lizard = spawnLizardNearHost(80);
        rig.runUntil(() -> clientCopyOf(lizard) != null, "lizard replicated");

        lizard.damage(1000);
        rig.runUntil(() -> clientCopyOf(lizard) == null, "lizard despawned on client");
        rig.runUntil(() -> client.presenter.deathAnimations > 0, "client shows death animation");
    }

    @Test
    void breakingAPotOnTheHostBreaksItForTheClient() {
        BreakableEntity pot = (BreakableEntity) objectsIn(host).stream()
            .filter(o -> o instanceof BreakableEntity b && b.getNetId() != 0)
            .findFirst().orElseThrow(() -> new AssertionError("start level should have pots"));
        rig.runUntil(() -> findByNetId(client, pot.getNetId()) != null, "pot replicated");

        pot.damage(1000);
        rig.runUntil(() -> client.presenter.particles > 0, "client plays the break effect");
        rig.runUntil(() -> findByNetId(client, pot.getNetId()) == null, "pot removed on client", 8000);
    }

    // ========== Items ==========

    @Test
    void clientPicksUpAnItemThroughTheHost() {
        moveClientAwayFromHost(); // Guests join on the host's spot; the host would grab it first
        PlayerEntity me = client.world.getLocalPlayer();
        host.items.spawnItem(START_LEVEL, ItemFactory.create("wood", 3),
            me.getTransform().getX(), me.getTransform().getY(), 0f);

        rig.runUntil(() -> me.getInventory().countItem("wood") == 3, "client receives the wood");
        rig.runUntil(() -> host.items.getItems(START_LEVEL).isEmpty(), "item gone on host");
        rig.runUntil(() -> client.items.getItems(START_LEVEL).isEmpty(), "item gone on client");
    }

    @Test
    void itemsTheClientDropsAreSpawnedByTheHostForEveryone() {
        client.world.dropItem(ItemFactory.create("stone", 2), 100, 100);
        rig.runUntil(() -> host.items.getItems(START_LEVEL).size() == 1, "host spawned the dropped item");
        rig.runUntil(() -> client.items.getItems(START_LEVEL).size() == 1, "client sees the dropped item");
    }

    // ========== Levels ==========

    @Test
    void clientCanTravelToAnotherLevelAndBack() {
        LizardEnemy lizard = spawnLizardNearHost(80);
        rig.runUntil(() -> clientCopyOf(lizard) != null, "lizard replicated");

        client.world.changeLevel(OTHER_LEVEL, null);
        rig.runUntil(() -> remotePlayers(host).isEmpty(), "client left the host's level");
        rig.runFor(0.3f);
        assertTrue(enemies(client).isEmpty(), "enemies from the old level must not follow the client");
        assertTrue(remotePlayers(client).isEmpty(), "host is in another level");
        assertNotNull(host.world.getInstances().stream()
            .filter(i -> i.getLevelId().equals(OTHER_LEVEL)).findFirst().orElse(null),
            "host keeps the client's level loaded");

        client.world.changeLevel(START_LEVEL, null);
        rig.runUntil(() -> clientCopyOf(lizard) != null, "lizard visible again after returning");
        rig.runUntil(() -> remotePlayers(client).size() == 1 && remotePlayers(host).size() == 1,
            "players see each other again");
    }

    // ========== Combat ==========

    @Test
    void theClientsAttacksAreResolvedByTheHost() {
        PlayerEntity me = client.world.getLocalPlayer();
        me.getInventory().getEquipment().equipItem(EquipmentSlot.WEAPON, ItemFactory.create("wooden_sword", 1));

        rig.runUntil(() -> remotePlayers(host).size() == 1, "host has the client's copy");
        PlayerEntity copy = remotePlayers(host).get(0);
        LizardEnemy lizard = new LizardEnemy(host.world.getCurrentInstance().getWorld(),
            copy.getTransform().getX() + 12, copy.getTransform().getY());
        host.world.getCurrentInstance().getWorld().addGameObject(lizard);
        int fullHealth = lizard.getHealth();

        rig.runUntil(() -> {
            if (!client.world.getLocalPlayer().getAttackComponent().isAttacking()) {
                float angle = lizard.getTransform().getPosition().cpy().sub(me.getTransform().getPosition()).angleDeg();
                client.presenter.input.attack(angle);
            }
            return lizard.getHealth() < fullHealth || !lizard.isActive();
        }, "host applies the client's sword hit");

        rig.runUntil(() -> !client.presenter.damageNumbers.isEmpty(), "client sees the damage number");
    }

    @Test
    void enemiesHitTheClientAndTheClientAppliesTheDamage() {
        moveClientAwayFromHost(); // So the lizard targets the client, not the host
        rig.runUntil(() -> remotePlayers(host).size() == 1, "host has the client's copy");
        PlayerEntity copy = remotePlayers(host).get(0);
        LizardEnemy lizard = new LizardEnemy(host.world.getCurrentInstance().getWorld(),
            copy.getTransform().getX() + 10, copy.getTransform().getY());
        host.world.getCurrentInstance().getWorld().addGameObject(lizard);

        PlayerEntity me = client.world.getLocalPlayer();
        int fullHealth = me.getHealth();
        rig.runUntil(() -> me.getHealth() < fullHealth, "lizard damages the client", 10000);
    }

    // ========== Leaving ==========

    @Test
    void leavingRemovesThePlayerAndTheHostKeepsTheirCharacter() {
        client.world.getLocalPlayer().getInventory().addItem(ItemFactory.create("wood", 7));
        rig.runUntil(() -> remotePlayers(host).size() == 1, "host has the client's copy");

        rig.disconnect(client);
        rig.runUntil(() -> remotePlayers(host).isEmpty(), "host removed the client's copy");

        PlayerData saved = SaveManager.getInstance().getGuestData("test:Bob");
        assertNotNull(saved, "host stored Bob's character");
        assertTrue(saved.inventory.defaultSlots.stream()
            .anyMatch(s -> s != null && s.itemId.equals("wood") && s.quantity == 7), "saved inventory has the wood");
    }

    @Test
    void aReturningClientGetsTheirInventoryBack() {
        client.world.getLocalPlayer().getInventory().addItem(ItemFactory.create("wood", 4));
        rig.disconnect(client);
        rig.runUntil(() -> remotePlayers(host).isEmpty(), "Bob left");

        TestWorld again = rig.join("Bob");
        assertEquals(4, again.world.getLocalPlayer().getInventory().countItem("wood"));
        assertEquals(1, again.presenter.dataRestored);
    }

    // ========== Helpers ==========

    /**
     * Guests join standing on the host. Step the client 48px away and wait until the host's copy follows.
     */
    private void moveClientAwayFromHost() {
        PlayerEntity me = client.world.getLocalPlayer();
        me.getTransform().setPosition(me.getTransform().getX() + 48, me.getTransform().getY());
        float targetX = me.getTransform().getX();
        rig.runUntil(() -> remotePlayers(host).size() == 1
            && Math.abs(remotePlayers(host).get(0).getTransform().getX() - targetX) < 1f, "host's copy follows the client");
    }

    private LizardEnemy spawnLizardNearHost(float offsetX) {
        PlayerEntity hostPlayer = host.world.getLocalPlayer();
        LizardEnemy lizard = new LizardEnemy(host.world.getCurrentInstance().getWorld(),
            hostPlayer.getTransform().getX() + offsetX, hostPlayer.getTransform().getY());
        host.world.getCurrentInstance().getWorld().addGameObject(lizard);
        assertNotEquals(0, lizard.getNetId(), "host should give new enemies a net ID");
        return lizard;
    }

    private EnemyEntity clientCopyOf(EnemyEntity hostEnemy) {
        return findByNetId(client, hostEnemy.getNetId()) instanceof EnemyEntity e ? e : null;
    }

    private static GameObject findByNetId(TestWorld world, int netId) {
        return objectsIn(world).stream().filter(o -> o.getNetId() == netId).findFirst().orElse(null);
    }

    private static List<GameObject> objectsIn(TestWorld world) {
        return world.world.getCurrentInstance().getWorld().getGameObjects();
    }

    private static List<EnemyEntity> enemies(TestWorld world) {
        return objectsIn(world).stream().filter(o -> o instanceof EnemyEntity)
            .map(o -> (EnemyEntity) o).collect(Collectors.toList());
    }

    /** Other machines' players visible in this machine's current level. */
    private static List<PlayerEntity> remotePlayers(TestWorld world) {
        return objectsIn(world).stream()
            .filter(o -> o instanceof PlayerEntity p && p.isNetworkControlled())
            .map(o -> (PlayerEntity) o).collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private static List<ItemPickupEntity> items(TestWorld world, String level) {
        return world.items.getItems(level);
    }
}
