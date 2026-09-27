package com.game.testsupport;

import com.badlogic.gdx.math.Vector2;
import com.game.integration.WorldManager;

/**
 * Finds tile positions in a level for tests (free floor for furniture, walls, ...).
 */
public final class LevelSpots {
    private LevelSpots() {
    }

    /**
     * The nearest 16x16 tile-aligned spot to (x, y) where furniture fits, at least minDistance away
     * (so it doesn't sit under a player standing at x, y).
     */
    public static Vector2 freeFurnitureSpot(WorldManager world, float x, float y, float minDistance) {
        float baseX = Math.round(x / 16f) * 16f;
        float baseY = Math.round(y / 16f) * 16f;
        for (int radius = 1; radius <= 6; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    float sx = baseX + dx * 16;
                    float sy = baseY + dy * 16;
                    if (Vector2.dst(sx, sy, x, y) >= minDistance && world.isPositionWalkable(sx, sy, 16, 16)) {
                        return new Vector2(sx, sy);
                    }
                }
            }
        }
        throw new AssertionError("No free furniture spot near " + x + "," + y);
    }

    /** A spot where furniture can't go (a wall). */
    public static Vector2 blockedSpot(WorldManager world) {
        for (int y = 16; y < world.getWorldHeight() * 16 - 16; y += 16) {
            for (int x = 16; x < world.getWorldWidth() * 16 - 16; x += 16) {
                if (!world.isPositionWalkable(x, y, 16, 16)) {
                    return new Vector2(x, y);
                }
            }
        }
        throw new AssertionError("Level has no walls?");
    }
}
