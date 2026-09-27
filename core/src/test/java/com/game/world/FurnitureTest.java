package com.game.world;

import com.badlogic.gdx.math.Vector2;
import com.game.save.FurnitureData;
import com.game.save.SaveData;
import com.game.save.PlayerData;
import com.game.save.WorldData;
import com.game.save.SaveManager;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.furniture.ChestEntity;
import com.game.systems.furniture.FurnitureEntity;
import com.game.systems.furniture.FurnitureManager;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.LevelSpots;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-player furniture: placing, picking up, saving.
 */
class FurnitureTest extends GameTestBase {

    @Test
    void placedFurnitureIsInTheLevelAndInTheSave() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        Vector2 spot = freeSpotNearPlayer(game);

        assertTrue(place(game, spot));

        ChestEntity chest = chestAt(game, spot);
        assertNotNull(chest, "chest is in the world");
        assertTrue(FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).contains(chest), "and saved");
        assertFalse(game.world.getCurrentInstance().getWorld().isPositionWalkable(spot.x, spot.y, 16, 16), "and solid");
    }

    @Test
    void furnitureCantBePlacedInsideWalls() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        Vector2 wall = LevelSpots.blockedSpot(game.world.getCurrentInstance().getWorld());

        assertFalse(place(game, wall));
        assertTrue(FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).isEmpty());
    }

    @Test
    void anEmptyChestCanBePickedUpButAFullOneCant() {
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        Vector2 spot = freeSpotNearPlayer(game);
        place(game, spot);
        ChestEntity chest = chestAt(game, spot);
        PlayerEntity player = game.world.getLocalPlayer();

        chest.getContainer().addItem(ItemFactory.create("wood", 1));
        assertFalse(game.world.pickUpFurniture(chest), "chests with items stay put");

        chest.getContainer().clear();
        assertTrue(game.world.pickUpFurniture(chest));
        assertEquals(1, player.getInventory().countItem("wooden_chest"));
        assertNull(chestAt(game, spot));
        assertTrue(FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).isEmpty());
    }

    @Test
    void loadingASaveGivesTheWorldTheSameFurnitureTheSaveKeepsTrackOf() {
        // Reproduces the old bug: furniture was imported twice, so the world's chest and the
        // manager's chest were different objects and later changes never reached the save.
        TestWorld game = newAuthoritativeWorld(START_LEVEL);
        Vector2 spot = freeSpotNearPlayer(game);
        place(game, spot);
        chestAt(game, spot).getContainer().addItem(ItemFactory.create("stone", 3));
        Map<String, List<FurnitureData>> saved = FurnitureManager.getInstance().exportSaveData();

        // Load it again, the way GameScreen does
        FurnitureManager.getInstance().importSaveData(saved);
        TestWorld loaded = new TestWorld(false);
        loaded.world.changeLevel(START_LEVEL, null);
        PlayerEntity player = loaded.world.getLocalPlayer();
        SaveManager.getInstance().initialize(player, player.getInventory(), FurnitureManager.getInstance(), loaded.items); // As the game's UI setup does
        SaveData save = new SaveData("s", 0, new PlayerData(), new WorldData(START_LEVEL, "tiled_map", saved, Map.of()));
        SaveManager.getInstance().applySaveData(save);

        ChestEntity inWorld = chestAt(loaded, spot);
        assertNotNull(inWorld);
        assertEquals(3, inWorld.getContainer().countItem("stone"), "contents survive");
        assertSame(inWorld, FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).get(0),
            "the chest in the world is the one that gets saved");
    }

    // ========== Helpers ==========

    static boolean place(TestWorld game, Vector2 spot) {
        AtomicReference<Boolean> result = new AtomicReference<>();
        game.world.requestPlaceFurniture("wooden_chest", spot.x, spot.y, result::set);
        assertNotNull(result.get(), "single-player placement answers immediately");
        return result.get();
    }

    static Vector2 freeSpotNearPlayer(TestWorld game) {
        PlayerEntity player = game.world.getLocalPlayer();
        return LevelSpots.freeFurnitureSpot(game.world.getCurrentInstance().getWorld(),
            player.getTransform().getX(), player.getTransform().getY(), 20);
    }

    static ChestEntity chestAt(TestWorld game, Vector2 spot) {
        for (Object obj : game.world.getCurrentInstance().getWorld().getGameObjects()) {
            if (obj instanceof ChestEntity chest && chest.getTransform().getPosition().epsilonEquals(spot, 0.01f)) {
                return chest;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static FurnitureEntity any(TestWorld game) {
        return FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).get(0);
    }
}
