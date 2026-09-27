package com.game.systems.dungeon;

import com.game.systems.dungeon.generation.DungeonGenerationResult;
import com.game.systems.dungeon.generation.DungeonGenerator;
import com.game.systems.dungeon.generation.PlacedRoom;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DungeonGenerationTest extends GameTestBase {
    private static final String THEME = "forest";

    @Test
    void theSameSeedAlwaysBuildsTheSameDungeon() {
        DungeonGenerationResult first = DungeonGenerator.generate(THEME, 20, 12345L);
        DungeonGenerationResult second = DungeonGenerator.generate(THEME, 20, 12345L);

        assertNotNull(first, "generation should succeed");
        assertEquals(layout(first), layout(second));
    }

    @Test
    void generatedRoomsDoNotOverlap() {
        for (long seed = 1; seed <= 5; seed++) {
            DungeonGenerationResult result = DungeonGenerator.generate(THEME, 20, seed);
            assertNotNull(result, "seed " + seed);
            List<PlacedRoom> rooms = result.getPlacedRooms();
            assertFalse(rooms.isEmpty(), "seed " + seed + " placed no rooms");

            for (int i = 0; i < rooms.size(); i++) {
                for (int j = i + 1; j < rooms.size(); j++) {
                    assertFalse(overlaps(rooms.get(i), rooms.get(j)),
                        "seed " + seed + ": rooms " + i + " and " + j + " overlap");
                }
            }
        }
    }

    private static List<String> layout(DungeonGenerationResult result) {
        return result.getPlacedRooms().stream()
            .map(r -> r.getTemplate().getName() + "@" + r.getWorldX() + "," + r.getWorldY())
            .collect(Collectors.toList());
    }

    private static boolean overlaps(PlacedRoom a, PlacedRoom b) {
        RoomBounds ra = a.getWorldBounds(16);
        RoomBounds rb = b.getWorldBounds(16);
        // Touching edges (shared walls/doors) is fine; real overlap is not
        return ra.getX() < rb.getX() + rb.getWidth() && rb.getX() < ra.getX() + ra.getWidth()
            && ra.getY() < rb.getY() + rb.getHeight() && rb.getY() < ra.getY() + ra.getHeight();
    }
}
