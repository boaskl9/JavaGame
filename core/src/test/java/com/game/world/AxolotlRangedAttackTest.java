package com.game.world;

import com.badlogic.gdx.math.Vector2;
import com.game.integration.WorldManager;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.ProjectileEntity;
import com.game.systems.entity.entities.enemies.Axolot;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.LevelSpots;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The axolotl keeps its distance and spits water projectiles at the player.
 */
class AxolotlRangedAttackTest extends GameTestBase {

    @Test
    void theAxolotlHitsThePlayerFromADistance() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        WorldManager world = game.world.getCurrentInstance().getWorld();
        Vector2 spot = LevelSpots.clearShotSpot(world, player.getTransform().getX(), player.getTransform().getY(), 64);
        Axolot axolotl = new Axolot(world, spot.x, spot.y);
        world.addGameObject(axolotl);
        int fullHealth = player.getHealth();

        boolean sawProjectile = false;
        float closest = Float.MAX_VALUE;
        for (int i = 0; i < 300 && player.getHealth() == fullHealth; i++) { // Up to 5 s
            game.run(1 / 60f);
            sawProjectile |= world.getGameObjects().stream().anyMatch(o -> o instanceof ProjectileEntity);
            closest = Math.min(closest, axolotl.getTransform().getPosition().dst(player.getTransform().getPosition()));
        }

        assertTrue(sawProjectile, "the axolotl fired a projectile");
        assertEquals(fullHealth - ProjectileEntity.Kind.WATER_SPIT.damage, player.getHealth(), "one spit hit the player");
        assertTrue(closest > 30, "it shot from a distance instead of walking up (closest: " + closest + ")");
        assertFalse(game.presenter.damageNumbers.isEmpty(), "the hit shows a damage number");
    }

    @Test
    void projectilesStopAtWalls() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        WorldManager world = game.world.getCurrentInstance().getWorld();
        Vector2[] spots = wallWithFloorNextToIt(world);
        Vector2 start = spots[0];
        Vector2 wall = spots[1];

        ProjectileEntity spit = new ProjectileEntity(world, ProjectileEntity.Kind.WATER_SPIT, null,
            start.x + 8, start.y + 8, (wall.x - start.x) * 6, (wall.y - start.y) * 6); // ~100 px/s at the wall
        world.addGameObject(spit);
        game.run(0.4f); // ~40 px: well short of its range, but past the wall

        assertFalse(world.contains(spit), "the projectile was removed when it hit the wall");
    }

    /** A free 16x16 tile right next to a wall tile; returns {free, wall}. */
    private static Vector2[] wallWithFloorNextToIt(WorldManager world) {
        int[][] offsets = {{0, -16}, {0, 16}, {-16, 0}, {16, 0}};
        for (int y = 16; y < world.getWorldHeight() * 16 - 16; y += 16) {
            for (int x = 16; x < world.getWorldWidth() * 16 - 16; x += 16) {
                if (world.isPositionWalkable(x, y, 16, 16)) continue;
                for (int[] o : offsets) {
                    if (world.isPositionWalkable(x + o[0], y + o[1], 16, 16)) {
                        return new Vector2[]{new Vector2(x + o[0], y + o[1]), new Vector2(x, y)};
                    }
                }
            }
        }
        throw new AssertionError("No wall next to open floor");
    }
}
