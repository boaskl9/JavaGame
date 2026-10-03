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

    /**
     * A 16x16 spot about `distance` from (x, y) with a clear straight line between their centers
     * (no walls, breakables or furniture), so a ranged enemy standing there can hit (x, y).
     * Tries 16 directions, and distances a little closer or further if none works.
     */
    public static Vector2 clearShotSpot(WorldManager world, float x, float y, float distance) {
        for (float d : new float[]{distance, distance - 8, distance + 8, distance - 16, distance + 16}) {
            for (float angle = 0; angle < 360; angle += 22.5f) {
                float sx = x + (float) Math.cos(Math.toRadians(angle)) * d;
                float sy = y + (float) Math.sin(Math.toRadians(angle)) * d;
                if (!world.isPositionWalkable(sx, sy, 16, 16)) continue;
                if (clearLine(world, x + 8, y + 8, sx + 8, sy + 8)) {
                    return new Vector2(sx, sy);
                }
            }
        }
        throw new AssertionError("No clear shot " + distance + "px from " + x + "," + y);
    }

    private static boolean clearLine(WorldManager world, float x1, float y1, float x2, float y2) {
        int steps = (int) (Vector2.dst(x1, y1, x2, y2) / 2f);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            // Generous width so the spit (and its slight random spread) has room
            if (!world.isPositionWalkable(x1 + (x2 - x1) * t - 4, y1 + (y2 - y1) * t - 4, 8, 8)) {
                return false;
            }
        }
        return true;
    }
}
