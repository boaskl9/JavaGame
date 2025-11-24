package com.game.networking;

import java.io.Serializable;

/**
 * Base class for all network packets.
 * All packets must be Serializable to be sent over the network.
 */
public abstract class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Get the type of this packet for routing/handling.
     */
    public abstract PacketType getType();

    /**
     * Enum of all packet types for easy identification.
     */
    public enum PacketType {
        CONNECTION,      // Client wants to join
        PLAYER_JOIN,     // Server notifies a player joined
        INPUT,           // Client sends input
        STATE_UPDATE,    // Server sends game state
        DISCONNECT       // Either side disconnects
    }
}
