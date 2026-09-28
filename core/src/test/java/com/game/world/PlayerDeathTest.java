package com.game.world;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.enemies.LizardEnemy;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-player death: the player is knocked out for a moment, then wakes up at the start level
 * with full health and everything they carried (no penalty).
 */
class PlayerDeathTest extends GameTestBase {

    @Test
    void aKnockedOutPlayerWakesUpAtTheStartLevelWithFullHealth() {
        TestWorld game = newAuthoritativeWorld(OTHER_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        int fullHealth = player.getMaxHealth();

        player.damage(fullHealth);

        assertTrue(game.world.isLocalPlayerDead());
        assertEquals(1, game.presenter.deaths);
        assertEquals(1, game.presenter.deathAnimations, "a death puff where they fell");

        game.run(GameWorld.RESPAWN_DELAY + 0.1f);

        assertFalse(game.world.isLocalPlayerDead());
        assertEquals(1, game.presenter.respawns);
        assertEquals(GameWorld.START_LEVEL, game.world.getCurrentInstance().getLevelId());
        assertEquals(game.world.getCurrentInstance().getSpawnPosition(null), player.getTransform().getPosition());
        assertEquals(fullHealth, player.getHealth());
        assertSame(player, game.world.getLocalPlayer(), "the same player, not a new one");
        assertTrue(game.world.getCurrentInstance().getWorld().contains(player));
    }

    @Test
    void dyingInTheStartLevelMovesThePlayerWithoutReloadingTheLevel() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        LevelInstance start = game.world.getCurrentInstance();
        player.getTransform().setPosition(player.getTransform().getX() + 32, player.getTransform().getY());

        player.damage(player.getMaxHealth());
        game.run(GameWorld.RESPAWN_DELAY + 0.1f);

        assertSame(start, game.world.getCurrentInstance());
        assertEquals(1, game.presenter.levelsEntered.size(), "no level change");
        assertEquals(start.getSpawnPosition(null), player.getTransform().getPosition());
    }

    @Test
    void thePlayerKeepsEverythingTheyCarried() {
        TestWorld game = newAuthoritativeWorld(OTHER_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        player.getInventory().addItem(ItemFactory.create("wood", 5));

        player.damage(player.getMaxHealth());
        game.run(GameWorld.RESPAWN_DELAY + 0.1f);

        assertEquals(5, player.getInventory().countItem("wood"));
    }

    @Test
    void aKnockedOutPlayerCantMoveOrPickThingsUp() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        Vector2 fellAt = player.getTransform().getPosition().cpy();
        game.items.spawnItem(ItemFactory.create("stone", 1), fellAt.x + 20, fellAt.y, 0f);

        player.damage(player.getMaxHealth());
        game.presenter.input.move(1, 0);
        game.run(GameWorld.RESPAWN_DELAY / 2);

        assertEquals(fellAt, player.getTransform().getPosition());
        assertEquals(0, player.getInventory().countItem("stone"));
    }

    @Test
    void hitsOnAKnockedOutPlayerDontCount() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        LizardEnemy lizard = new LizardEnemy(game.world.getCurrentInstance().getWorld(),
            player.getTransform().getX() + 10, player.getTransform().getY());
        game.world.getCurrentInstance().getWorld().addGameObject(lizard);
        player.getHealthComponent().setHealth(1);

        game.run(5f); // Knocked out by the lizard, respawned, and more

        assertEquals(1, game.presenter.deaths, "hits on a knocked-out player don't count");
        assertTrue(player.isAlive());
    }

    @Test
    void respawnIfDeadWakesThePlayerRightAway() {
        TestWorld game = newAuthoritativeWorld(OTHER_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        player.damage(player.getMaxHealth());

        game.world.respawnIfDead();

        assertFalse(game.world.isLocalPlayerDead());
        assertEquals(GameWorld.START_LEVEL, game.world.getCurrentInstance().getLevelId());
        assertEquals(player.getMaxHealth(), player.getHealth());
    }
}
