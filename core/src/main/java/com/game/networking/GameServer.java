package com.game.networking;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;

/**
 * Thin KryoNet server transport. Everything received is pushed onto a {@link PacketQueue}
 * and handled on the game thread by {@link HostSession}.
 */
public class GameServer {
    // Override with -Dgame.port=... (e.g. to run several test sessions on one machine)
    public static final int DEFAULT_PORT = Integer.getInteger("game.port", 25565);
    private static final int WRITE_BUFFER_SIZE = 256 * 1024;
    private static final int OBJECT_BUFFER_SIZE = 64 * 1024;

    private final Server server;
    private final PacketQueue queue = new PacketQueue();
    private final int port;
    private boolean running = false;

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.server = new Server(WRITE_BUFFER_SIZE, OBJECT_BUFFER_SIZE);
        Packets.register(server.getKryo());

        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("GameServer: Client connected from " + connection.getRemoteAddressTCP());
                queue.push(connection.getID(), PacketQueue.CONNECTED);
            }

            @Override
            public void disconnected(Connection connection) {
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
    }

    /**
     * Start the server and begin accepting connections.
     * @return true if the server is listening
     */
    public boolean start() {
        if (running) {
            return true;
        }

        try {
            server.bind(port, port + 1); // TCP port, UDP port
            server.start();
            running = true;
            System.out.println("GameServer: Started on port " + port);
        } catch (IOException e) {
            System.err.println("GameServer: Failed to start server on port " + port);
            e.printStackTrace();
        }
        return running;
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        server.stop();
        System.out.println("GameServer: Stopped");
    }

    public void send(int connectionId, Object packet) {
        server.sendToTCP(connectionId, packet);
    }

    public void kick(int connectionId) {
        for (Connection connection : server.getConnections()) {
            if (connection.getID() == connectionId) {
                connection.close();
            }
        }
    }

    public PacketQueue getQueue() {
        return queue;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}
