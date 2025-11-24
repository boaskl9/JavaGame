package com.game.networking;

/**
 * Sent by server to all clients when a player joins.
 * Includes the assigned player ID.
 */
public class PlayerJoinPacket extends Packet {
    private static final long serialVersionUID = 1L;

    private int playerId;
    private String playerName;

    public PlayerJoinPacket() {
    }

    public PlayerJoinPacket(int playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_JOIN;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }
}
