package com.game.networking;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import com.game.networking.identity.PlayerIdentity;

import java.io.IOException;

/**
 * Thin KryoNet client transport. Everything received is pushed onto a {@link PacketQueue}
 * and handled on the game thread by {@link ClientSession}.
 *
 * Packets are queued from the moment the connection opens, so nothing is lost while the
 * game screen is still being created.
 */
public class GameClient {
    private static final int WRITE_BUFFER_SIZE = 64 * 1024;
    private static final int OBJECT_BUFFER_SIZE = 64 * 1024;
    private static final int TIMEOUT_MS = 5000;

    private final Client client;
    private final PacketQueue queue = new PacketQueue();
    private final PlayerIdentity identity;

    public GameClient(PlayerIdentity identity) {
        this.identity = identity;
        this.client = new Client(WRITE_BUFFER_SIZE, OBJECT_BUFFER_SIZE);
        Packets.register(client.getKryo());

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("GameClient: Connected to server");
                Packets.Hello hello = new Packets.Hello();
                hello.identityProvider = identity.getProvider();
                hello.identityId = identity.getId();
                connection.sendTCP(hello);
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("GameClient: Disconnected from server");
                queue.push(connection.getID(), PacketQueue.DISCONNECTED);
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof com.esotericsoftware.kryonet.FrameworkMessage) {
                    return; // KryoNet keep-alives etc.
                }
                queue.push(connection.getID(), object);
            }
        });

        client.start();
    }

    /**
     * Connect to a server (blocks until connected or timed out).
     * @return true if connection successful
     */
    public boolean connect(String host) {
        return connect(host, GameServer.DEFAULT_PORT);
    }

    public boolean connect(String host, int port) {
        try {
            client.connect(TIMEOUT_MS, host, port, port + 1); // TCP port, UDP port
            System.out.println("GameClient: Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("GameClient: Failed to connect to " + host + ":" + port + " - " + e.getMessage());
            return false;
        }
    }

    public void send(Object packet) {
        if (client.isConnected()) {
            client.sendTCP(packet);
        }
    }

    public void disconnect() {
        client.stop(); // Closes the connection and stops the network thread
    }

    public PacketQueue getQueue() {
        return queue;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public PlayerIdentity getIdentity() {
        return identity;
    }
}
