package com.game.world;

/**
 * The world's calendar: which day it is and how far into the day.
 *
 * A day runs from 06:00 to 02:00 on the clock, over {@link #DEFAULT_DAY_LENGTH} real seconds.
 * It ends early when the players sleep; when time runs out, everyone passes out.
 * The host (or single-player) advances it; guests follow the host's clock.
 *
 * The world seed makes per-day content (like dungeon layouts) the same for every player that day.
 */
public class DayCycle {
    public static final float DEFAULT_DAY_LENGTH = 180f; // Real seconds
    public static final int START_MINUTES = 6 * 60;      // 06:00
    public static final int END_MINUTES = 26 * 60;       // 02:00 the next morning
    private static final int CLOCK_STEP_MINUTES = 10;    // The clock ticks in 10-minute steps

    private int day = 1;
    private float elapsed = 0f; // Real seconds into the current day
    private float dayLength = DEFAULT_DAY_LENGTH;
    private long worldSeed;

    public DayCycle(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    /**
     * Advance the clock.
     * @return true if the day just ran out (players pass out)
     */
    public boolean advance(float delta) {
        if (elapsed >= dayLength) return false;
        elapsed = Math.min(dayLength, elapsed + delta);
        return elapsed >= dayLength;
    }

    /** Start the next morning. */
    public void startNextDay() {
        day++;
        elapsed = 0f;
    }

    /** Minutes since midnight on the day's clock (can exceed 24h after midnight), in 10-minute steps. */
    public int getClockMinutes() {
        float progress = dayLength > 0 ? elapsed / dayLength : 1f;
        int minutes = START_MINUTES + (int) (progress * (END_MINUTES - START_MINUTES));
        return minutes - minutes % CLOCK_STEP_MINUTES;
    }

    /** The clock as a 24-hour digital time, e.g. "06:00", "23:50", "01:30". */
    public String getClockText() {
        int minutes = getClockMinutes();
        return String.format("%02d:%02d", (minutes / 60) % 24, minutes % 60);
    }

    /** A seed for something that should be the same for everyone today, e.g. a dungeon's layout. */
    public long seedFor(String name, int forDay) {
        long h = worldSeed * 0x9E3779B97F4A7C15L + forDay;
        for (int i = 0; i < name.length(); i++) {
            h = h * 31 + name.charAt(i);
        }
        // Mix the bits (SplitMix64 finalizer) so neighbouring days give unrelated seeds
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** Take over another clock: the host's (guests) or a saved one. */
    public void set(int day, float elapsed, float dayLength, long worldSeed) {
        this.day = day;
        this.elapsed = elapsed;
        this.dayLength = dayLength;
        this.worldSeed = worldSeed;
    }

    public int getDay() {
        return day;
    }

    public float getElapsed() {
        return elapsed;
    }

    public float getDayLength() {
        return dayLength;
    }

    /** Shorten or lengthen the day (tests and debugging). */
    public void setDayLength(float dayLength) {
        this.dayLength = dayLength;
    }

    public long getWorldSeed() {
        return worldSeed;
    }
}
