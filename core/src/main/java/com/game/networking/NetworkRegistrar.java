package com.game.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import java.util.HashMap;

/**
 * Registers all packet classes with Kryo for serialization.
 * This must be called on both server and client before networking begins.
 */
public class NetworkRegistrar {

    /**
     * Register all network packet classes with the given endpoint.
     * Both server and client must register the same classes in the same order.
     */
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        // Register all packet classes in a consistent order
        kryo.register(ConnectionPacket.class);
        kryo.register(PlayerJoinPacket.class);
        kryo.register(InputPacket.class);
        kryo.register(StateUpdatePacket.class);
        kryo.register(StateUpdatePacket.PlayerState.class);
        kryo.register(DisconnectPacket.class);

        // Register HashMap for StateUpdatePacket's playerStates map
        kryo.register(HashMap.class);
    }
}
