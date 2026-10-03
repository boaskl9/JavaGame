package com.game.systems.dungeon.assembly;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.dungeon.RoomBounds;
import com.game.systems.dungeon.generation.PlacedRoom;
import com.game.systems.level.LevelData;

import java.util.List;
import java.util.Random;

/**
 * Adds what a playable dungeon needs on top of its rooms, deterministically from the seed:
 * - the entrance (the default spawn point) in the first room, on a spot a player can stand;
 * - an exit gateway on that spot, which leads back to where the players came from;
 * - enemies in the other rooms (0-2 per room).
 *
 * Every machine that builds the same dungeon gets the same objects; only the host spawns the
 * enemies (clients get them replicated).
 */
public final class DungeonPopulator {
    /** Gateway target that means "leave the dungeon" (resolved by GameWorld). */
    public static final String EXIT_TARGET = "dungeon:exit";
    public static final String ENEMY_OBJECT = "enemy";
    public static final String ENEMY_TYPE_PROPERTY = "enemyType";

    private static final int TILE = 16;
    private static final int MAX_ENEMIES_PER_ROOM = 2;
    private static final int PLACEMENT_TRIES = 12;

    private DungeonPopulator() {
    }

    /**
     * @param enemyTypes replicated enemy types to pick from, e.g. "enemy:lizard"
     */
    public static void populate(AssembledDungeon dungeon, List<PlacedRoom> rooms, long seed, List<String> enemyTypes) {
        if (rooms.isEmpty()) return;

        SpatialQuery walls = new SpatialQuery();
        for (Rectangle shape : dungeon.getCollisionShapes()) {
            walls.addRectangle(shape);
        }
        LevelData levelData = dungeon.getLevelData();

        // Entrance + exit in the first room
        Vector2 entrance = nearestStandable(dungeon, walls, roomRect(dungeon, rooms.get(0)));
        if (entrance == null) {
            System.err.println("DungeonPopulator: No standable spot in the first room");
            return;
        }
        levelData.addSpawnPoint("player_spawn", entrance.x, entrance.y);
        LevelData.LevelObject exit = new LevelData.LevelObject("gateway", "dungeon_exit", entrance.x, entrance.y - 4);
        exit.setSize(TILE, 12); // Covers the player's feet when standing on the entrance
        exit.setProperty("targetLevel", EXIT_TARGET);
        levelData.addObject(exit);

        // Enemies in the other rooms
        if (enemyTypes.isEmpty()) return;
        Random random = new Random(seed ^ 0x5DEECE66DL);
        for (int i = 1; i < rooms.size(); i++) {
            Rectangle room = roomRect(dungeon, rooms.get(i));
            int count = random.nextInt(MAX_ENEMIES_PER_ROOM + 1);
            for (int n = 0; n < count; n++) {
                Vector2 spot = randomStandable(dungeon, walls, room, random);
                if (spot == null) continue;
                String type = enemyTypes.get(random.nextInt(enemyTypes.size()));
                LevelData.LevelObject enemy = new LevelData.LevelObject(ENEMY_OBJECT, type, spot.x, spot.y);
                enemy.setProperty(ENEMY_TYPE_PROPERTY, type);
                levelData.addObject(enemy);
            }
        }
    }

    /** A room's area in the assembled map, in pixels. */
    private static Rectangle roomRect(AssembledDungeon dungeon, PlacedRoom room) {
        RoomBounds bounds = room.getTemplate().getBounds();
        return new Rectangle(
            room.getWorldX() - dungeon.getOffsetX() * TILE,
            room.getWorldY() - dungeon.getOffsetY() * TILE,
            bounds.getWidth() * TILE,
            bounds.getHeight() * TILE);
    }

    /** The standable tile closest to the room's center. */
    private static Vector2 nearestStandable(AssembledDungeon dungeon, SpatialQuery walls, Rectangle room) {
        float cx = room.x + room.width / 2f;
        float cy = room.y + room.height / 2f;
        Vector2 best = null;
        float bestDistance = Float.MAX_VALUE;
        for (float y = tileAlign(room.y) + TILE; y < room.y + room.height - TILE; y += TILE) {
            for (float x = tileAlign(room.x) + TILE; x < room.x + room.width - TILE; x += TILE) {
                if (!isStandable(dungeon, walls, x, y)) continue;
                float distance = Vector2.dst2(x, y, cx, cy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new Vector2(x, y);
                }
            }
        }
        return best;
    }

    private static Vector2 randomStandable(AssembledDungeon dungeon, SpatialQuery walls, Rectangle room, Random random) {
        int columns = (int) (room.width / TILE) - 2;
        int rows = (int) (room.height / TILE) - 2;
        if (columns <= 0 || rows <= 0) return null;
        for (int attempt = 0; attempt < PLACEMENT_TRIES; attempt++) {
            float x = tileAlign(room.x) + TILE * (1 + random.nextInt(columns));
            float y = tileAlign(room.y) + TILE * (1 + random.nextInt(rows));
            if (isStandable(dungeon, walls, x, y)) {
                return new Vector2(x, y);
            }
        }
        return null;
    }

    /**
     * Whether a character's feet (8x4 at the bottom center of a 16px tile, like the players' and
     * enemies' environment colliders) fit here, on a floor tile.
     */
    private static boolean isStandable(AssembledDungeon dungeon, SpatialQuery walls, float x, float y) {
        if (walls.testRectangle(new Rectangle(x + 4, y, 8, 4))) return false;
        return hasFloor(dungeon, (int) (x / TILE), (int) (y / TILE));
    }

    private static boolean hasFloor(AssembledDungeon dungeon, int tileX, int tileY) {
        for (MapLayer layer : dungeon.getTiledMap().getLayers()) {
            if (layer instanceof TiledMapTileLayer tiles && tiles.getCell(tileX, tileY) != null) {
                return true;
            }
        }
        return false;
    }

    private static float tileAlign(float value) {
        return (float) Math.floor(value / TILE) * TILE;
    }
}
