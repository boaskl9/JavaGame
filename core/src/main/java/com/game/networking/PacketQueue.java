package com.game.networking;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hands packets from KryoNet's network thread to the game thread.
 * The game drains it once per frame, so all game state is only ever touched on the game thread.
 */
public class PacketQueue {

    /** A received packet, or a connect/disconnect event (packet == CONNECTED / DISCONNECTED). */
    public static final class Incoming {
        public final int connectionId;
        public final Object packet;

        Incoming(int connectionId, Object packet) {
            this.connectionId = connectionId;
            this.packet = packet;
        }
    }

    public static final Object CONNECTED = new Object();
    public static final Object DISCONNECTED = new Object();

    private final ConcurrentLinkedQueue<Incoming> queue = new ConcurrentLinkedQueue<>();

    void push(int connectionId, Object packet) {
        queue.add(new Incoming(connectionId, packet));
    }

    /**
     * @return the next packet, or null when empty
     */
    public Incoming poll() {
        return queue.poll();
    }
}
