package com.game.integration;

import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldItemManagerTest extends GameTestBase {
    private WorldItemManager items;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void create() {
        items = new WorldItemManager();
        items.addListener(new WorldItemManager.Listener() {
            @Override
            public void onItemSpawned(String levelId, ItemPickupEntity item) {
                events.add("spawn " + levelId);
            }

            @Override
            public void onItemRemoved(String levelId, ItemPickupEntity item) {
                events.add("remove " + levelId);
            }
        });
    }

    @Test
    void itemsStayInTheLevelTheyWereSpawnedIn() {
        items.setCurrentLevel("A");
        items.spawnItem(ItemFactory.create("wood", 1), 0, 0, 0);
        items.spawnItem("B", ItemFactory.create("stone", 1), 0, 0, 0);

        assertEquals(1, items.getItems("A").size());
        assertEquals(1, items.getItems("B").size());
        assertEquals(1, items.getAllItems().size(), "level-less calls use the current level");

        items.setCurrentLevel("B");
        assertEquals("stone", items.getAllItems().get(0).getItemStack().getDefinition().getId());
    }

    @Test
    void listenersHearAboutSpawnsRemovalsAndPickups() {
        ItemPickupEntity removed = items.spawnItem("A", ItemFactory.create("wood", 1), 0, 0, 0);
        ItemPickupEntity pickedUp = items.spawnItem("A", ItemFactory.create("wood", 1), 5, 5, 0);

        items.removeItem(removed);
        pickedUp.onPickup(); // Becomes inactive; swept on the next update
        items.update("A", 0.016f);

        assertEquals(List.of("spawn A", "spawn A", "remove A", "remove A"), events);
        assertTrue(items.getItems("A").isEmpty());
    }

    @Test
    void onlyNearbyItemsAreFound() {
        items.setCurrentLevel("A");
        items.spawnItem(ItemFactory.create("wood", 1), 10, 10, 0);
        items.spawnItem(ItemFactory.create("wood", 1), 200, 200, 0);

        assertEquals(1, items.getItemsNear(new com.badlogic.gdx.math.Vector2(0, 0), 32).size());
    }

    @Test
    void saveDataCoversEveryLevel() {
        items.spawnItem("A", ItemFactory.create("wood", 3), 1, 2, 0);
        items.spawnItem("B", ItemFactory.create("stone", 4), 3, 4, 0);

        WorldItemManager loaded = new WorldItemManager();
        loaded.importSaveData(items.exportSaveData());

        assertEquals(3, loaded.getItems("A").get(0).getItemStack().getQuantity());
        assertEquals(4, loaded.getItems("B").get(0).getItemStack().getQuantity());
    }
}
