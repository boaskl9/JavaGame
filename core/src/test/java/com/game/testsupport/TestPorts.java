package com.game.testsupport;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public final class TestPorts {
    private TestPorts() {
    }

    /** A random TCP port whose next port is free for UDP too (KryoNet binds both). */
    public static int freePortPair() {
        for (int attempt = 0; attempt < 50; attempt++) {
            int port = ThreadLocalRandom.current().nextInt(30000, 50000);
            try (ServerSocket tcp = new ServerSocket(port); DatagramSocket udp = new DatagramSocket(port + 1)) {
                return port;
            } catch (IOException ignored) {
                // Taken, try another
            }
        }
        throw new IllegalStateException("No free port pair found");
    }
}
