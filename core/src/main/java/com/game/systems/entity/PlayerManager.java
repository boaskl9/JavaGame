package com.game.systems.entity;

import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.input.InputSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages multiple PlayerEntity instances for local and networked multiplayer.
 *
 * Responsibilities:
 * - Track all active players (List<PlayerEntity>)
 * - Assign unique player IDs
 * - Manage player lifecycle (add/remove)
 * - Provide convenient access to all players
 *
 * Design notes:
 * - Replaces the PlayerEntity singleton pattern
 * - Supports both local multi-player (Phase 1) and networked (Phase 2+)
 * - Each player has a unique ID and InputSource
 */
public class PlayerManager {
    private final List<PlayerEntity> players;
    private int nextPlayerId = 0;

    public PlayerManager() {
        this.players = new ArrayList<>();
    }

    /**
     * Add a new player to the game.
     * Automatically assigns a unique player ID.
     *
     * @param player The PlayerEntity to add
     * @return The assigned player ID
     */
    public int addPlayer(PlayerEntity player) {
        int playerId = nextPlayerId++;
        player.setPlayerId(playerId);
        players.add(player);

        System.out.println("PlayerManager: Added player " + playerId +
                         " at (" + player.getTransform().getX() + ", " + player.getTransform().getY() + ")");
        return playerId;
    }

    /**
     * Remove a player from the game.
     * @param player The player to remove
     */
    public void removePlayer(PlayerEntity player) {
        if (players.remove(player)) {
            System.out.println("PlayerManager: Removed player " + player.getPlayerId());
        }
    }

    /**
     * Remove a player by ID.
     * @param playerId The player ID to remove
     */
    public void removePlayerById(int playerId) {
        PlayerEntity player = getPlayerById(playerId);
        if (player != null) {
            removePlayer(player);
        }
    }

    /**
     * Get a player by ID.
     * @param playerId The player ID
     * @return The PlayerEntity, or null if not found
     */
    public PlayerEntity getPlayerById(int playerId) {
        for (PlayerEntity player : players) {
            if (player.getPlayerId() == playerId) {
                return player;
            }
        }
        return null;
    }

    /**
     * Get all active players.
     * @return Immutable list of all players
     */
    public List<PlayerEntity> getAllPlayers() {
        return new ArrayList<>(players); // Return copy to prevent external modification
    }

    /**
     * Get the number of active players.
     * @return Player count
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Get the first player (useful for single-player compatibility).
     * @return The first player, or null if no players exist
     */
    public PlayerEntity getFirstPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * Check if there are any players.
     * @return true if at least one player exists
     */
    public boolean hasPlayers() {
        return !players.isEmpty();
    }

    /**
     * Clear all players.
     * Used when loading a new game or returning to menu.
     */
    public void clear() {
        System.out.println("PlayerManager: Clearing all players");
        players.clear();
        nextPlayerId = 0;
    }

    /**
     * Update all players.
     * @param delta Time since last frame
     */
    public void update(float delta) {
        // Update all players
        for (PlayerEntity player : players) {
            player.update(delta);
        }
    }

    @Override
    public String toString() {
        return "PlayerManager{players=" + players.size() + ", nextId=" + nextPlayerId + "}";
    }
}
