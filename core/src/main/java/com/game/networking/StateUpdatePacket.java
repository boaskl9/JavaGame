package com.game.networking;

import java.util.HashMap;
import java.util.Map;

/**
 * Sent by server to all clients with current game state.
 * Contains positions, health, and other state for all players.
 */
public class StateUpdatePacket extends Packet {
    private static final long serialVersionUID = 1L;

    private Map<Integer, PlayerState> playerStates;

    public StateUpdatePacket() {
        this.playerStates = new HashMap<>();
    }

    public StateUpdatePacket(Map<Integer, PlayerState> playerStates) {
        this.playerStates = playerStates;
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
        public boolean facingRight;

        public PlayerState() {
        }

        public PlayerState(float x, float y, int health, int maxHealth,
                          String currentAnimation, boolean facingRight) {
            this.x = x;
            this.y = y;
            this.health = health;
            this.maxHealth = maxHealth;
            this.currentAnimation = currentAnimation;
            this.facingRight = facingRight;
        }
    }
}
