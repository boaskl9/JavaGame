package com.game.networking;

import com.game.testsupport.TestPorts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every packet type survives a real KryoNet round trip (registration, field types, arrays, nulls).
 */
class PacketTransportTest {
    private GameServer server;
    private GameClient client;
    private int connectionId;

    @BeforeEach
    void connect() throws InterruptedException {
        int port = TestPorts.freePortPair();
        server = new GameServer(port);
        assertTrue(server.start());
        client = new GameClient(new com.game.networking.identity.PlayerIdentity("local", "abc-123", "Alice"));
        assertTrue(client.connect("localhost", port));

        assertSame(PacketQueue.CONNECTED, awaitEvent(server.getQueue()).packet);
        Packets.Hello hello = await(server.getQueue(), Packets.Hello.class);
        assertEquals("local", hello.identityProvider, "client introduces itself on connect");
        assertEquals("abc-123", hello.identityId);
        connectionId = lastConnectionId;
    }

    @AfterEach
    void disconnect() {
        client.disconnect();
        server.stop();
    }

    @Test
    void welcomeAndSnapshot() throws InterruptedException {
        Packets.Welcome welcome = new Packets.Welcome();
        welcome.playerId = 3;
        welcome.levelId = "Maps/prototype.tmx";
        welcome.x = 12.5f;
        server.send(connectionId, welcome);
        Packets.Welcome w = await(client.getQueue(), Packets.Welcome.class);
        assertEquals(3, w.playerId);
        assertEquals(12.5f, w.x);
        assertNull(w.savedPlayerJson);

        Packets.EntitySpawn enemy = new Packets.EntitySpawn();
        enemy.netId = 7;
        enemy.type = "enemy:lizard";
        Packets.EntitySpawn item = new Packets.EntitySpawn();
        item.netId = 8;
        item.type = Packets.TYPE_ITEM;
        item.itemId = "wood";
        item.quantity = 3;
        Packets.LevelSnapshot snapshot = new Packets.LevelSnapshot();
        snapshot.epoch = 2;
        snapshot.entities = new Packets.EntitySpawn[]{enemy, item};
        server.send(connectionId, snapshot);

        Packets.LevelSnapshot s = await(client.getQueue(), Packets.LevelSnapshot.class);
        assertEquals(2, s.epoch);
        assertEquals("enemy:lizard", s.entities[0].type);
        assertEquals(3, s.entities[1].quantity);
    }

    @Test
    void largeStateBatch() throws InterruptedException {
        int n = 200;
        Packets.EntityStateBatch batch = new Packets.EntityStateBatch();
        batch.time = 123456789L;
        batch.netIds = new int[n];
        batch.x = new float[n];
        batch.y = new float[n];
        batch.vx = new float[n];
        batch.vy = new float[n];
        batch.anims = new String[n];
        batch.dirs = new int[n];
        batch.flips = new boolean[n];
        batch.hp = new int[n];
        for (int i = 0; i < n; i++) {
            batch.netIds[i] = i;
            batch.x[i] = i * 1.5f;
            batch.anims[i] = "walk";
            batch.flips[i] = i % 2 == 0;
        }
        server.send(connectionId, batch);

        Packets.EntityStateBatch b = await(client.getQueue(), Packets.EntityStateBatch.class);
        assertEquals(n, b.netIds.length);
        assertEquals(298.5f, b.x[199]);
        assertTrue(b.flips[4]);
        assertFalse(b.flips[5]);
        assertEquals(123456789L, b.time);
    }

    @Test
    void hostEvents() throws InterruptedException {
        Packets.Effect effect = new Packets.Effect();
        effect.kind = Packets.Effect.PLAYER_ATTACK;
        effect.text = "wooden_sword";
        Packets.EntityDespawn despawn = new Packets.EntityDespawn();
        despawn.netId = 7;
        Packets.PlayerHit hit = new Packets.PlayerHit();
        hit.knockbackX = -80f;
        Packets.ItemGrant grant = new Packets.ItemGrant();
        grant.quantity = 3;
        Packets.PlayerLeft left = new Packets.PlayerLeft();
        left.playerId = 5;

        server.send(connectionId, effect);
        server.send(connectionId, despawn);
        server.send(connectionId, hit);
        server.send(connectionId, grant);
        server.send(connectionId, left);

        assertEquals("wooden_sword", await(client.getQueue(), Packets.Effect.class).text);
        assertEquals(7, await(client.getQueue(), Packets.EntityDespawn.class).netId);
        assertEquals(-80f, await(client.getQueue(), Packets.PlayerHit.class).knockbackX);
        assertEquals(3, await(client.getQueue(), Packets.ItemGrant.class).quantity);
        assertEquals(5, await(client.getQueue(), Packets.PlayerLeft.class).playerId);
    }

    @Test
    void clientRequests() throws InterruptedException {
        Packets.PlayerState state = new Packets.PlayerState();
        state.levelId = "Maps/prototype.tmx";
        state.anim = "run";
        state.flipX = true;
        state.weaponId = null;
        Packets.LevelChange change = new Packets.LevelChange();
        change.epoch = 2;
        Packets.AttackRequest attack = new Packets.AttackRequest();
        attack.weaponId = "wooden_sword";
        Packets.PickupRequest pickup = new Packets.PickupRequest();
        pickup.netId = 8;
        Packets.DropItem drop = new Packets.DropItem();
        drop.itemId = "wood";
        Packets.InventorySync sync = new Packets.InventorySync();
        sync.playerJson = "{\"x\":1}";

        client.send(state);
        client.send(change);
        client.send(attack);
        client.send(pickup);
        client.send(drop);
        client.send(sync);

        Packets.PlayerState ps = await(server.getQueue(), Packets.PlayerState.class);
        assertEquals("run", ps.anim);
        assertTrue(ps.flipX);
        assertNull(ps.weaponId);
        assertEquals(2, await(server.getQueue(), Packets.LevelChange.class).epoch);
        assertEquals("wooden_sword", await(server.getQueue(), Packets.AttackRequest.class).weaponId);
        assertEquals(8, await(server.getQueue(), Packets.PickupRequest.class).netId);
        assertEquals("wood", await(server.getQueue(), Packets.DropItem.class).itemId);
        assertEquals("{\"x\":1}", await(server.getQueue(), Packets.InventorySync.class).playerJson);
    }

    @Test
    void disconnectIsReportedToTheHost() throws InterruptedException {
        client.disconnect();
        assertSame(PacketQueue.DISCONNECTED, awaitEvent(server.getQueue()).packet);
    }

    // ========== Helpers ==========

    private int lastConnectionId;

    /** Wait for the next packet of a type, skipping others. */
    private <T> T await(PacketQueue queue, Class<T> type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            PacketQueue.Incoming in = awaitEvent(queue);
            if (type.isInstance(in.packet)) {
                lastConnectionId = in.connectionId;
                return type.cast(in.packet);
            }
        }
        return fail("Timed out waiting for " + type.getSimpleName());
    }

    /** Wait for the next packet or connect/disconnect event. */
    private PacketQueue.Incoming awaitEvent(PacketQueue queue) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            PacketQueue.Incoming in = queue.poll();
            if (in != null) {
                return in;
            }
            Thread.sleep(2);
        }
        return fail("Timed out waiting for a network event");
    }
}
