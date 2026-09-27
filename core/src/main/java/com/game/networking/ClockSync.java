package com.game.networking;

/**
 * Maps a remote machine's clock to ours, so snapshot timestamps reflect when the sender
 * produced them rather than when they happened to arrive. This smooths out packets that
 * arrive in bursts.
 *
 * Tracks the smallest observed (local - remote) offset, i.e. the least-delayed packet, and lets it
 * creep up slowly so it recovers from clock drift or one unusually fast packet.
 */
public class ClockSync {
    private long offset;
    private boolean initialized;

    public long toLocalTime(long remoteTime) {
        return toLocalTime(remoteTime, System.currentTimeMillis());
    }

    long toLocalTime(long remoteTime, long now) {
        long sample = now - remoteTime;
        if (!initialized || sample < offset) {
            offset = sample;
            initialized = true;
        } else {
            offset = Math.min(offset + 1, sample);
        }
        return remoteTime + offset;
    }
}
