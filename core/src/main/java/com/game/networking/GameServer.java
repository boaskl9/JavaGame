package com.game.networking;

import com.game.systems.entity.PlayerManager;
import com.game.systems.entity.entities.PlayerEntity;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game server that hosts the multiplayer session.
 * Accepts client connections, receives input, and broadcasts game state.
 */
public class GameServer {
    private static final int DEFAULT_PORT = 25565;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private boolean running = false;
    private int port;

    // Map of client ID to ClientHandler
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
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
            serverSocket = new ServerSocket(port);
            running = true;

            // Start accept thread
            acceptThread = new Thread(this::acceptLoop, "Server-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

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
        for (ClientHandler client : clients.values()) {
            client.disconnect("Server shutting down");
        }
        clients.clear();

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("GameServer: Stopped");
    }

    /**
     * Accept loop running in separate thread.
     */
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("GameServer: Client connected from " + clientSocket.getInetAddress());

                // Assign client ID
                int clientId = nextClientId++;

                // Create client handler
                ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
                clients.put(clientId, handler);
                handler.start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("GameServer: Error accepting client");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Called when a client sends a connection packet.
     */
    void onClientConnected(int clientId, String playerName) {
        System.out.println("GameServer: Client " + clientId + " connected as " + playerName);

        // Notify game
        if (clientConnectedCallback != null) {
            clientConnectedCallback.onClientConnected(clientId, playerName);
        }

        // Send PlayerJoinPacket to all clients
        PlayerJoinPacket joinPacket = new PlayerJoinPacket(clientId, playerName);
        broadcastPacket(joinPacket);
    }

    /**
     * Called when a client disconnects.
     */
    void onClientDisconnected(int clientId, String reason) {
        clients.remove(clientId);
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
    void onInputReceived(int clientId, InputPacket packet) {
        // Notify game to apply input
        if (inputReceivedCallback != null) {
            inputReceivedCallback.onInputReceived(clientId, packet);
        }
    }

    /**
     * Broadcast a packet to all connected clients.
     */
    public void broadcastPacket(Packet packet) {
        for (ClientHandler client : clients.values()) {
            client.sendPacket(packet);
        }
    }

    /**
     * Send a packet to a specific client.
     */
    public void sendPacketToClient(int clientId, Packet packet) {
        ClientHandler client = clients.get(clientId);
        if (client != null) {
            client.sendPacket(packet);
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
        return clients.size();
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

    /**
     * Handles communication with a single client.
     */
    private static class ClientHandler {
        private final Socket socket;
        private final int clientId;
        private final GameServer server;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private Thread receiveThread;
        private boolean connected = true;

        public ClientHandler(Socket socket, int clientId, GameServer server) {
            this.socket = socket;
            this.clientId = clientId;
            this.server = server;
        }

        public void start() {
            try {
                // Create streams (output first to avoid deadlock)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                // Start receive thread
                receiveThread = new Thread(this::receiveLoop, "Server-Client-" + clientId);
                receiveThread.setDaemon(true);
                receiveThread.start();

            } catch (IOException e) {
                System.err.println("GameServer: Failed to start client handler " + clientId);
                e.printStackTrace();
                disconnect("Failed to initialize connection");
            }
        }

        private void receiveLoop() {
            while (connected) {
                try {
                    Packet packet = (Packet) in.readObject();

                    switch (packet.getType()) {
                        case CONNECTION:
                            ConnectionPacket connPacket = (ConnectionPacket) packet;
                            server.onClientConnected(clientId, connPacket.getPlayerName());
                            break;

                        case INPUT:
                            InputPacket inputPacket = (InputPacket) packet;
                            server.onInputReceived(clientId, inputPacket);
                            break;

                        case DISCONNECT:
                            DisconnectPacket discPacket = (DisconnectPacket) packet;
                            disconnect(discPacket.getReason());
                            break;

                        default:
                            System.out.println("GameServer: Unknown packet type from client " + clientId);
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

        public void sendPacket(Packet packet) {
            if (!connected) return;

            try {
                synchronized (out) {
                    out.writeObject(packet);
                    out.flush();
                    out.reset(); // Prevent memory leak from object caching
                }
            } catch (IOException e) {
                System.err.println("GameServer: Failed to send packet to client " + clientId);
                disconnect("Failed to send packet");
            }
        }

        public void disconnect(String reason) {
            if (!connected) return;

            connected = false;

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            server.onClientDisconnected(clientId, reason);
        }
    }
}
