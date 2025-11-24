package com.game.networking;

/**
 * Sent when a player disconnects (either client or server).
 */
public class DisconnectPacket extends Packet {
    private static final long serialVersionUID = 1L;

    private int playerId;
    private String reason;

    public DisconnectPacket() {
    }

    public DisconnectPacket(int playerId, String reason) {
        this.playerId = playerId;
        this.reason = reason;
    }

    @Override
    public PacketType getType() {
        return PacketType.DISCONNECT;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getReason() {
        return reason;
    }
}
