package com.game.networking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClockSyncTest {

    @Test
    void burstsAreSpreadBackOutToTheSendersSpacing() {
        // Sent at 0, 33 and 66ms. The first arrives promptly, the next two together in a burst.
        ClockSync clock = new ClockSync();
        long a = clock.toLocalTime(0, 50);
        long b = clock.toLocalTime(33, 120);
        long c = clock.toLocalTime(66, 120);

        assertEquals(33, b - a, 2);
        assertEquals(33, c - b, 2);
    }

    @Test
    void aFasterPacketLowersTheEstimatedDelay() {
        ClockSync clock = new ClockSync();
        clock.toLocalTime(0, 100);                // 100ms delay
        long mapped = clock.toLocalTime(100, 130); // Only 30ms delay: the better estimate wins
        assertEquals(130, mapped);
    }

    @Test
    void estimateRecoversSlowlyAfterAnUnusuallyFastPacket() {
        ClockSync clock = new ClockSync();
        clock.toLocalTime(0, 10); // Freak 10ms packet
        long first = clock.toLocalTime(1000, 1080);
        long later = first;
        for (int i = 1; i <= 50; i++) {
            later = clock.toLocalTime(1000 + i * 33, 1080 + i * 33);
        }
        // Offset creeps up by at most 1ms per packet toward the real 80ms delay
        assertTrue(later - (1000 + 50 * 33) > first - 1000);
        assertTrue(later - (1000 + 50 * 33) <= 80);
    }
}
