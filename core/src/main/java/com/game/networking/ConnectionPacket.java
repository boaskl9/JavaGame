package com.game.networking;

/**
 * Sent by client to server when attempting to join.
 * Server responds with PlayerJoinPacket if accepted.
 */
public class ConnectionPacket extends Packet {
    private static final long serialVersionUID = 1L;

    private String playerName;

    public ConnectionPacket() {
        this("Player");
    }

    public ConnectionPacket(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public PacketType getType() {
        return PacketType.CONNECTION;
    }

    public String getPlayerName() {
        return playerName;
    }
}
