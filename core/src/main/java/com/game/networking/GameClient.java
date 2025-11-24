package com.game.networking;

import java.io.*;
import java.net.Socket;

/**
 * Game client that connects to a GameServer.
 * Sends input to server and receives game state updates.
 */
public class GameClient {
    private static final int DEFAULT_PORT = 25565;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;
    private boolean connected = false;

    private String serverHost;
    private int serverPort;
    private int assignedPlayerId = -1;

    // Callbacks for game integration
    private ConnectionCallback connectionCallback;
    private DisconnectionCallback disconnectionCallback;
    private StateUpdateCallback stateUpdateCallback;
    private PlayerJoinCallback playerJoinCallback;

    public GameClient() {
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
            socket = new Socket(host, port);

            // Create streams (output first to avoid deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // Start receive thread
            receiveThread = new Thread(this::receiveLoop, "Client-Receive");
            receiveThread.setDaemon(true);
            receiveThread.start();

            System.out.println("GameClient: Connected to " + host + ":" + port);

            // Send connection packet
            sendPacket(new ConnectionPacket("Player " + (assignedPlayerId + 1)));

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
        sendPacket(new DisconnectPacket(assignedPlayerId, reason));

        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("GameClient: Disconnected: " + reason);

        // Notify callback
        if (disconnectionCallback != null) {
            disconnectionCallback.onDisconnected(reason);
        }
    }

    /**
     * Receive loop running in separate thread.
     */
    private void receiveLoop() {
        while (connected) {
            try {
                Packet packet = (Packet) in.readObject();

                switch (packet.getType()) {
                    case PLAYER_JOIN:
                        PlayerJoinPacket joinPacket = (PlayerJoinPacket) packet;
                        onPlayerJoined(joinPacket);
                        break;

                    case STATE_UPDATE:
                        StateUpdatePacket statePacket = (StateUpdatePacket) packet;
                        onStateUpdate(statePacket);
                        break;

                    case DISCONNECT:
                        DisconnectPacket discPacket = (DisconnectPacket) packet;
                        disconnect(discPacket.getReason());
                        break;

                    default:
                        System.out.println("GameClient: Unknown packet type: " + packet.getType());
                        break;
                }

            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    disconnect("Connection error");
                }
                break;
            }
        }
    }

    /**
     * Handle player join packet.
     */
    private void onPlayerJoined(PlayerJoinPacket packet) {
        System.out.println("GameClient: Player " + packet.getPlayerId() + " joined: " + packet.getPlayerName());

        // If this is us, store our player ID
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
    public void sendPacket(Packet packet) {
        if (!connected) {
            System.err.println("GameClient: Cannot send packet - not connected");
            return;
        }

        try {
            synchronized (out) {
                out.writeObject(packet);
                out.flush();
                out.reset(); // Prevent memory leak from object caching
            }
        } catch (IOException e) {
            System.err.println("GameClient: Failed to send packet");
            e.printStackTrace();
            disconnect("Failed to send packet");
        }
    }

    /**
     * Send input packet to server.
     */
    public void sendInput(float movementX, float movementY,
                         boolean attackPressed, boolean attackJustPressed,
                         float aimDirectionX, float aimDirectionY,
                         boolean running) {
        InputPacket packet = new InputPacket(
            assignedPlayerId, movementX, movementY,
            attackPressed, attackJustPressed,
            aimDirectionX, aimDirectionY, running
        );
        sendPacket(packet);
    }

    /**
     * Check if client is connected.
     */
    public boolean isConnected() {
        return connected;
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
