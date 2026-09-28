package com.game.testsupport;

import com.game.networking.GameClient;
import com.game.networking.Packets;
import com.game.save.PlayerData;
import com.game.save.SaveManager;
import com.game.networking.identity.PlayerIdentity;
import com.game.systems.loot.LootSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A real host and real clients in one JVM, talking over localhost on a private port.
 * Every machine is stepped together; {@link #runUntil} keeps stepping until a condition holds.
 */
public class MultiplayerRig implements AutoCloseable {
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    public final int port = TestPorts.freePortPair();
    public final GameTestBase.TestWorld host;
    public final List<GameTestBase.TestWorld> clients = new ArrayList<>();

    public MultiplayerRig(String hostLevel) {
        host = new GameTestBase.TestWorld(false);
        LootSystem.initialize(host.items); // Only the host ever rolls loot
        host.world.changeLevel(hostLevel, null);
        if (!host.world.startHosting(port)) {
            fail("Host could not start on port " + port);
        }
    }

    public static final String TEST_PROVIDER = "test";

    /**
     * Join from a machine whose identity is the character name ("test:Bob"), playing the character
     * with that name (created if the world has none).
     */
    public GameTestBase.TestWorld join(String characterName) {
        return join(new PlayerIdentity(TEST_PROVIDER, characterName, characterName), characterName);
    }

    /**
     * Connect a client, then play the world's character with this name, or create it if there is none.
     * Steps until the client has joined (level built, local player created).
     */
    public GameTestBase.TestWorld join(PlayerIdentity identity, String characterName) {
        GameTestBase.TestWorld client = connect(identity);
        Packets.CharacterInfo existing = client.presenter.character(characterName);
        if (existing != null) {
            client.world.playCharacter(existing.id);
        } else {
            client.world.createCharacter(characterName);
        }
        runUntil(() -> client.world.getLocalPlayer() != null, characterName + " joins");
        return client;
    }

    /** Connect a client and step until the host's character list arrives (not joined yet). */
    public GameTestBase.TestWorld connect(PlayerIdentity identity) {
        GameClient connection = new GameClient(identity);
        if (!connection.connect("localhost", port)) {
            fail("Client could not connect to port " + port);
        }
        GameTestBase.TestWorld client = new GameTestBase.TestWorld(true);
        client.world.join(connection);
        clients.add(client);
        runUntil(() -> client.presenter.characters != null, identity + " gets the character list");
        return client;
    }

    /** The saved guest character with this name, or null. */
    public static PlayerData savedCharacter(String name) {
        for (PlayerData data : SaveManager.getInstance().getGuestCharacters().values()) {
            if (name.equals(data.displayName)) return data;
        }
        return null;
    }

    private long nextStepAt = 0;

    /**
     * Step every machine once, paced to real time (60 steps per second). Interpolation and
     * clock sync run on the wall clock, so the simulation must not run faster than it.
     */
    public void step() {
        long now = System.currentTimeMillis();
        if (nextStepAt > now) {
            sleep(nextStepAt - now);
        }
        nextStepAt = Math.max(now, nextStepAt) + (long) (GameTestBase.DT * 1000);

        host.step(GameTestBase.DT);
        for (GameTestBase.TestWorld client : clients) {
            client.step(GameTestBase.DT);
        }
    }

    public void runFor(float seconds) {
        long end = System.currentTimeMillis() + (long) (seconds * 1000);
        while (System.currentTimeMillis() < end) {
            step();
        }
    }

    public void runUntil(BooleanSupplier condition, String description) {
        runUntil(condition, description, DEFAULT_TIMEOUT_MS);
    }

    public void runUntil(BooleanSupplier condition, String description, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for: " + description);
            }
            step();
        }
    }

    public void disconnect(GameTestBase.TestWorld client) {
        client.world.stopMultiplayer();
        clients.remove(client);
    }

    @Override
    public void close() {
        for (GameTestBase.TestWorld client : clients) {
            client.world.dispose();
        }
        host.world.dispose();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
