package com.game.networking;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game server that hosts the multiplayer session using KryoNet.
 * Accepts client connections, receives input, and broadcasts game state.
 */
public class GameServer {
    private static final int DEFAULT_PORT = 25565;
    private static final int WRITE_BUFFER_SIZE = 16384;
    private static final int READ_BUFFER_SIZE = 16384;

    private Server server;
    private boolean running = false;
    private int port;

    // Map of connection ID to client ID
    private final Map<Integer, Integer> connectionToClientId = new ConcurrentHashMap<>();
    private int nextClientId = 1; // Start at 1 (0 is host)

    // Callbacks for game integration
    private ClientConnectedCallback clientConnectedCallback;
    private ClientDisconnectedCallback clientDisconnectedCallback;
    private InputReceivedCallback inputReceivedCallback;

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.server = new Server(WRITE_BUFFER_SIZE, READ_BUFFER_SIZE);

        // Register packet classes with Kryo
        NetworkRegistrar.register(server);

        // Set up listener for network events
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("GameServer: Client connected from " + connection.getRemoteAddressTCP());
            }

            @Override
            public void disconnected(Connection connection) {
                Integer clientId = connectionToClientId.remove(connection.getID());
                if (clientId != null) {
                    onClientDisconnected(clientId, "Disconnected");
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof ConnectionPacket) {
                    ConnectionPacket packet = (ConnectionPacket) object;
                    // Assign client ID
                    int clientId = nextClientId++;
                    connectionToClientId.put(connection.getID(), clientId);

                    // Send PlayerJoinPacket back to the client with their ID
                    PlayerJoinPacket joinPacket = new PlayerJoinPacket(clientId, packet.getPlayerName());
                    connection.sendTCP(joinPacket);

                    // Notify game
                    onClientConnected(clientId, packet.getPlayerName());

                } else if (object instanceof InputPacket) {
                    InputPacket packet = (InputPacket) object;
                    Integer clientId = connectionToClientId.get(connection.getID());
                    if (clientId != null) {
                        onInputReceived(clientId, packet);
                    }

                } else if (object instanceof DisconnectPacket) {
                    DisconnectPacket packet = (DisconnectPacket) object;
                    Integer clientId = connectionToClientId.get(connection.getID());
                    if (clientId != null) {
                        onClientDisconnected(clientId, packet.getReason());
                    }
                }
            }
        });
    }

    /**
     * Start the server and begin accepting connections.
     */
    public void start() {
        if (running) {
            System.out.println("GameServer: Already running");
            return;
        }

        try {
            server.bind(port, port + 1); // TCP port, UDP port
            server.start();
            running = true;

            System.out.println("GameServer: Started on port " + port);
        } catch (IOException e) {
            System.err.println("GameServer: Failed to start server");
            e.printStackTrace();
        }
    }

    /**
     * Stop the server and disconnect all clients.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        // Disconnect all clients
        for (Connection connection : server.getConnections()) {
            connection.close();
        }
        connectionToClientId.clear();

        // Stop server
        server.stop();

        System.out.println("GameServer: Stopped");
    }

    /**
     * Called when a client sends a connection packet.
     */
    private void onClientConnected(int clientId, String playerName) {
        System.out.println("GameServer: Client " + clientId + " connected as " + playerName);

        // Notify game
        if (clientConnectedCallback != null) {
            clientConnectedCallback.onClientConnected(clientId, playerName);
        }
    }

    /**
     * Called when a client disconnects.
     */
    private void onClientDisconnected(int clientId, String reason) {
        System.out.println("GameServer: Client " + clientId + " disconnected: " + reason);

        // Notify game
        if (clientDisconnectedCallback != null) {
            clientDisconnectedCallback.onClientDisconnected(clientId, reason);
        }

        // Send DisconnectPacket to all remaining clients
        DisconnectPacket disconnectPacket = new DisconnectPacket(clientId, reason);
        broadcastPacket(disconnectPacket);
    }

    /**
     * Called when a client sends an input packet.
     */
    private void onInputReceived(int clientId, InputPacket packet) {
        // Notify game to apply input
        if (inputReceivedCallback != null) {
            inputReceivedCallback.onInputReceived(clientId, packet);
        }
    }

    /**
     * Broadcast a packet to all connected clients.
     */
    public void broadcastPacket(Object packet) {
        server.sendToAllTCP(packet);
    }

    /**
     * Send a packet to a specific client.
     */
    public void sendPacketToClient(int clientId, Object packet) {
        // Find connection by client ID
        for (Map.Entry<Integer, Integer> entry : connectionToClientId.entrySet()) {
            if (entry.getValue().equals(clientId)) {
                Connection connection = server.getConnections()[0]; // Get connection by ID
                for (Connection conn : server.getConnections()) {
                    if (conn.getID() == entry.getKey()) {
                        conn.sendTCP(packet);
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the port the server is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get number of connected clients (excluding host).
     */
    public int getClientCount() {
        return connectionToClientId.size();
    }

    // Callback setters
    public void setClientConnectedCallback(ClientConnectedCallback callback) {
        this.clientConnectedCallback = callback;
    }

    public void setClientDisconnectedCallback(ClientDisconnectedCallback callback) {
        this.clientDisconnectedCallback = callback;
    }

    public void setInputReceivedCallback(InputReceivedCallback callback) {
        this.inputReceivedCallback = callback;
    }

    // Callback interfaces
    public interface ClientConnectedCallback {
        void onClientConnected(int clientId, String playerName);
    }

    public interface ClientDisconnectedCallback {
        void onClientDisconnected(int clientId, String reason);
    }

    public interface InputReceivedCallback {
        void onInputReceived(int clientId, InputPacket packet);
    }
}
