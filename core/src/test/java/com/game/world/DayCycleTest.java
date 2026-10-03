package com.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DayCycleTest {

    @Test
    void theClockRunsFromSixInTheMorningToTwoAtNightInTenMinuteSteps() {
        DayCycle days = new DayCycle(42);
        days.setDayLength(200f); // 20 clock hours: 1 real second = 6 clock minutes

        assertEquals("06:00", days.getClockText());
        days.advance(100f);
        assertEquals("16:00", days.getClockText());
        days.advance(1f); // 16:06 shows as 16:00
        assertEquals("16:00", days.getClockText());
        days.advance(79f);
        assertEquals("00:00", days.getClockText());
        days.advance(19f);
        assertEquals("01:50", days.getClockText());
    }

    @Test
    void theDayRunsOutOnceThenWaitsForTheNextMorning() {
        DayCycle days = new DayCycle(42);
        days.setDayLength(10f);

        assertFalse(days.advance(9f));
        assertTrue(days.advance(2f), "time ran out");
        assertEquals("02:00", days.getClockText());
        assertFalse(days.advance(1f), "reported only once");

        days.startNextDay();
        assertEquals(2, days.getDay());
        assertEquals("06:00", days.getClockText());
    }

    @Test
    void seedsAreTheSameForEveryoneOnADayButChangeWithTheDayTheNameAndTheWorld() {
        DayCycle days = new DayCycle(42);
        assertEquals(days.seedFor("cave", 3), new DayCycle(42).seedFor("cave", 3));
        assertNotEquals(days.seedFor("cave", 3), days.seedFor("cave", 4));
        assertNotEquals(days.seedFor("cave", 3), days.seedFor("forest", 3));
        assertNotEquals(days.seedFor("cave", 3), new DayCycle(43).seedFor("cave", 3));
    }
}
