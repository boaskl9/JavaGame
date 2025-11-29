package com.game.networking;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

/**
 * Game client that connects to a GameServer using KryoNet.
 * Sends input to server and receives game state updates.
 */
public class GameClient {
    private static final int DEFAULT_PORT = 25565;
    private static final int WRITE_BUFFER_SIZE = 16384;
    private static final int READ_BUFFER_SIZE = 16384;
    private static final int TIMEOUT_MS = 5000;

    private Client client;
    private boolean connected = false;

    private String serverHost;
    private int serverPort;
    private int assignedPlayerId = -1;

    // Input sequence tracking for reconciliation
    private int nextInputSequence = 1; // Start at 1 (0 reserved for "no input")

    // Callbacks for game integration
    private ConnectionCallback connectionCallback;
    private DisconnectionCallback disconnectionCallback;
    private StateUpdateCallback stateUpdateCallback;
    private PlayerJoinCallback playerJoinCallback;

    public GameClient() {
        this.client = new Client(WRITE_BUFFER_SIZE, READ_BUFFER_SIZE);

        // Register packet classes with Kryo
        NetworkRegistrar.register(client);

        // Set up listener for network events
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("GameClient: Connected to server");
                connected = true;

                // Send connection packet
                client.sendTCP(new ConnectionPacket("Player"));
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("GameClient: Disconnected from server");
                connected = false;

                // Notify callback
                if (disconnectionCallback != null) {
                    disconnectionCallback.onDisconnected("Connection closed");
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerJoinPacket) {
                    PlayerJoinPacket packet = (PlayerJoinPacket) object;
                    onPlayerJoined(packet);

                } else if (object instanceof StateUpdatePacket) {
                    StateUpdatePacket packet = (StateUpdatePacket) object;
                    onStateUpdate(packet);

                } else if (object instanceof DisconnectPacket) {
                    DisconnectPacket packet = (DisconnectPacket) object;
                    disconnect(packet.getReason());
                }
            }
        });

        // Start client thread
        client.start();
    }

    /**
     * Connect to a server.
     * @param host Server hostname or IP
     * @return true if connection successful
     */
    public boolean connect(String host) {
        return connect(host, DEFAULT_PORT);
    }

    /**
     * Connect to a server.
     * @param host Server hostname or IP
     * @param port Server port
     * @return true if connection successful
     */
    public boolean connect(String host, int port) {
        if (connected) {
            System.out.println("GameClient: Already connected");
            return false;
        }

        this.serverHost = host;
        this.serverPort = port;

        try {
            client.connect(TIMEOUT_MS, host, port, port + 1); // TCP port, UDP port
            System.out.println("GameClient: Attempting to connect to " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("GameClient: Failed to connect to " + host + ":" + port);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disconnect from server.
     */
    public void disconnect() {
        disconnect("Client disconnected");
    }

    /**
     * Disconnect from server with reason.
     */
    public void disconnect(String reason) {
        if (!connected) return;

        // Send disconnect packet
        if (assignedPlayerId != -1) {
            client.sendTCP(new DisconnectPacket(assignedPlayerId, reason));
        }

        connected = false;
        client.close();

        System.out.println("GameClient: Disconnected: " + reason);

        // Notify callback
        if (disconnectionCallback != null) {
            disconnectionCallback.onDisconnected(reason);
        }
    }

    /**
     * Handle player join packet.
     */
    private void onPlayerJoined(PlayerJoinPacket packet) {
        System.out.println("GameClient: Player " + packet.getPlayerId() + " joined: " + packet.getPlayerName());

        // If this is us (first join packet we receive), store our player ID
        if (assignedPlayerId == -1) {
            assignedPlayerId = packet.getPlayerId();
            System.out.println("GameClient: Assigned player ID: " + assignedPlayerId);

            // Notify connection callback
            if (connectionCallback != null) {
                connectionCallback.onConnected(assignedPlayerId);
            }
        }

        // Notify player join callback
        if (playerJoinCallback != null) {
            playerJoinCallback.onPlayerJoined(packet.getPlayerId(), packet.getPlayerName());
        }
    }

    /**
     * Handle state update packet.
     */
    private void onStateUpdate(StateUpdatePacket packet) {
        // Notify game to update player states
        if (stateUpdateCallback != null) {
            stateUpdateCallback.onStateUpdate(packet);
        }
    }

    /**
     * Send a packet to the server.
     */
    public void sendPacket(Object packet) {
        if (!connected) {
            System.err.println("GameClient: Cannot send packet - not connected");
            return;
        }

        client.sendTCP(packet);
    }

    /**
     * Send input packet to server with sequence number.
     * @param currentLevelId The level the client is currently in
     * @return The sequence number assigned to this input
     */
    public int sendInput(String currentLevelId, float movementX, float movementY,
                         boolean attackPressed, boolean attackJustPressed,
                         float aimDirectionX, float aimDirectionY,
                         boolean running) {
        int sequenceNumber = nextInputSequence++;
        InputPacket packet = new InputPacket(
            assignedPlayerId, sequenceNumber, currentLevelId,
            movementX, movementY,
            attackPressed, attackJustPressed,
            aimDirectionX, aimDirectionY, running
        );
        sendPacket(packet);
        return sequenceNumber;
    }

    /**
     * Check if client is connected.
     */
    public boolean isConnected() {
        return connected && client.isConnected();
    }

    /**
     * Get assigned player ID.
     */
    public int getAssignedPlayerId() {
        return assignedPlayerId;
    }

    // Callback setters
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    public void setDisconnectionCallback(DisconnectionCallback callback) {
        this.disconnectionCallback = callback;
    }

    public void setStateUpdateCallback(StateUpdateCallback callback) {
        this.stateUpdateCallback = callback;
    }

    public void setPlayerJoinCallback(PlayerJoinCallback callback) {
        this.playerJoinCallback = callback;
    }

    // Callback interfaces
    public interface ConnectionCallback {
        void onConnected(int assignedPlayerId);
    }

    public interface DisconnectionCallback {
        void onDisconnected(String reason);
    }

    public interface StateUpdateCallback {
        void onStateUpdate(StateUpdatePacket packet);
    }

    public interface PlayerJoinCallback {
        void onPlayerJoined(int playerId, String playerName);
    }
}
