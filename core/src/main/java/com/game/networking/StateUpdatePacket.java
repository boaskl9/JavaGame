package com.game.networking;

import java.util.HashMap;
import java.util.Map;

/**
 * Sent by server to all clients with current game state.
 * Contains positions, health, and other state for all players.
 * Includes input sequence acknowledgments for client prediction reconciliation.
 */
public class StateUpdatePacket extends Packet {
    private static final long serialVersionUID = 1L;

    private Map<Integer, PlayerState> playerStates;

    /**
     * Map of playerId -> lastProcessedInputSequence.
     * Tells each client which of their inputs the server has confirmed processing.
     * Used for client prediction reconciliation.
     */
    private Map<Integer, Integer> lastProcessedInputs;

    public StateUpdatePacket() {
        this.playerStates = new HashMap<>();
        this.lastProcessedInputs = new HashMap<>();
    }

    public StateUpdatePacket(Map<Integer, PlayerState> playerStates) {
        this.playerStates = playerStates;
        this.lastProcessedInputs = new HashMap<>();
    }

    @Override
    public PacketType getType() {
        return PacketType.STATE_UPDATE;
    }

    public Map<Integer, PlayerState> getPlayerStates() {
        return playerStates;
    }

    public void addPlayerState(int playerId, PlayerState state) {
        playerStates.put(playerId, state);
    }

    public Map<Integer, Integer> getLastProcessedInputs() {
        return lastProcessedInputs;
    }

    public void setLastProcessedInput(int playerId, int sequenceNumber) {
        lastProcessedInputs.put(playerId, sequenceNumber);
    }

    public Integer getLastProcessedInput(int playerId) {
        return lastProcessedInputs.get(playerId);
    }

    /**
     * Represents the state of a single player.
     */
    public static class PlayerState implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public float x;
        public float y;
        public int health;
        public int maxHealth;
        public String currentAnimation;
        public int currentDirection; // Animation direction angle (0=down, 90=left, 180=up, 270=right)
        public boolean flipX; // Horizontal flip for sprite

        public PlayerState() {
        }

        public PlayerState(float x, float y, int health, int maxHealth,
                          String currentAnimation, int currentDirection, boolean flipX) {
            this.x = x;
            this.y = y;
            this.health = health;
            this.maxHealth = maxHealth;
            this.currentAnimation = currentAnimation;
            this.currentDirection = currentDirection;
            this.flipX = flipX;
        }
    }
}
