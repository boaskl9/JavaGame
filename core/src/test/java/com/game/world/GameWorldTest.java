package com.game.world;

import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.enemies.LizardEnemy;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-player behaviour of the simulation (no network).
 */
class GameWorldTest extends GameTestBase {

    @Test
    void startingAGameCreatesThePlayerInTheLevel() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);

        PlayerEntity player = game.world.getLocalPlayer();
        assertNotNull(player);
        assertTrue(game.world.getCurrentInstance().getWorld().contains(player));
        assertEquals(java.util.List.of(START_LEVEL), game.presenter.levelsEntered);
    }

    @Test
    void walkingOverAnItemPicksItUp() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        game.items.spawnItem(ItemFactory.create("wood", 2), player.getTransform().getX(), player.getTransform().getY(), 0f);

        game.run(0.1f);

        assertEquals(2, player.getInventory().countItem("wood"));
        assertTrue(game.items.getAllItems().isEmpty());
        assertTrue(game.presenter.inventoryChanges > 0, "UI is told to refresh");
    }

    @Test
    void levelsKeepTheirStateWhenYouComeBack() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        LevelInstance start = game.world.getCurrentInstance();
        int potsBefore = countPots(start);
        firstPot(start).damage(1000);
        game.run(2f); // Break animation finishes, pot is removed

        game.world.changeLevel(OTHER_LEVEL, null);
        game.world.changeLevel(START_LEVEL, null);

        assertSame(start, game.world.getCurrentInstance(), "the same level instance is reused");
        assertEquals(potsBefore - 1, countPots(game.world.getCurrentInstance()));
    }

    @Test
    void thePlayersSwordDamagesEnemies() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        player.getInventory().getEquipment().equipItem(EquipmentSlot.WEAPON, ItemFactory.create("wooden_sword", 1));
        LizardEnemy lizard = new LizardEnemy(game.world.getCurrentInstance().getWorld(),
            player.getTransform().getX() + 14, player.getTransform().getY());
        game.world.getCurrentInstance().getWorld().addGameObject(lizard);
        int fullHealth = lizard.getHealth();

        game.presenter.input.attack(0); // Aim right, at the lizard
        game.run(0.6f);

        assertTrue(lizard.getHealth() < fullHealth || !lizard.isActive(), "sword should hit the lizard");
        assertFalse(game.presenter.damageNumbers.isEmpty(), "a damage number is shown");
    }

    @Test
    void enemiesAttackThePlayer() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        PlayerEntity player = game.world.getLocalPlayer();
        LizardEnemy lizard = new LizardEnemy(game.world.getCurrentInstance().getWorld(),
            player.getTransform().getX() + 10, player.getTransform().getY());
        game.world.getCurrentInstance().getWorld().addGameObject(lizard);
        int fullHealth = player.getHealth();

        game.run(3f);

        assertTrue(player.getHealth() < fullHealth, "a lizard standing next to the player should attack");
    }

    private static int countPots(LevelInstance level) {
        return (int) level.getWorld().getGameObjects().stream().filter(o -> o instanceof BreakableEntity).count();
    }

    private static BreakableEntity firstPot(LevelInstance level) {
        for (GameObject obj : level.getWorld().getGameObjects()) {
            if (obj instanceof BreakableEntity pot) return pot;
        }
        throw new AssertionError("level has no pots");
    }
}
