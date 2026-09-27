package com.game.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Save files are written and read with libGDX Json; make sure everything survives the trip.
 */
class SaveDataTest extends GameTestBase {

    @Test
    void worldPlayerAndGuestsSurviveJson() {
        List<ItemStackData> slots = new ArrayList<>();
        slots.add(new ItemStackData("wood", 5));
        slots.add(null);
        InventoryData inventory = new InventoryData(slots, new ArrayList<>(), new HashMap<>());

        Map<String, List<DroppedItemData>> dropped = new HashMap<>();
        dropped.put("Maps/prototype.tmx", new ArrayList<>(List.of(new DroppedItemData("stone", 2, 10, 20))));
        Map<String, List<FurnitureData>> furniture = new HashMap<>();
        furniture.put("Maps/prototype.tmx", new ArrayList<>(List.of(new FurnitureData("wooden_chest", 32, 48, new ArrayList<>()))));

        SaveData save = new SaveData("test", 120,
            new PlayerData(1, 2, 20, 28, inventory),
            new WorldData("Maps/prototype.tmx", "tiled_map", furniture, dropped));
        save.guestPlayers = new HashMap<>();
        save.guestPlayers.put("Bob", new PlayerData(3, 4, 10, 28, inventory));

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        SaveData loaded = json.fromJson(SaveData.class, json.toJson(save));

        assertEquals("test", loaded.saveName);
        assertEquals(20, loaded.player.currentHealth);
        assertEquals("wood", loaded.player.inventory.defaultSlots.get(0).itemId);
        assertNull(loaded.player.inventory.defaultSlots.get(1), "empty slots are preserved");
        assertEquals(2, loaded.world.droppedItemsByLevel.get("Maps/prototype.tmx").get(0).quantity);
        assertEquals("wooden_chest", loaded.world.furnitureByLevel.get("Maps/prototype.tmx").get(0).itemId);
        assertEquals(10, loaded.guestPlayers.get("Bob").currentHealth);
    }

    @Test
    void oldSavesWithoutGuestsStillLoad() {
        String oldSave = "{\"version\":\"1.0.0\",\"saveName\":\"old\",\"playtimeSeconds\":5,"
            + "\"player\":{\"x\":1,\"y\":2,\"currentHealth\":28,\"maxHealth\":28},"
            + "\"world\":{\"currentLevelId\":\"Maps/prototype.tmx\",\"levelType\":\"tiled_map\"}}";

        SaveData loaded = new Json().fromJson(SaveData.class, oldSave);

        assertEquals("old", loaded.saveName);
        assertNull(loaded.guestPlayers);
    }

    @Test
    void guestCharactersAreRestoredFromASave() {
        SaveData save = new SaveData("s", 0, new PlayerData(), new WorldData());
        save.guestPlayers = new HashMap<>();
        save.guestPlayers.put("Bob", new PlayerData(7, 8, 5, 28, null));

        SaveManager.getInstance().applySaveData(save);

        assertEquals(7, SaveManager.getInstance().getGuestData("Bob").x);
        assertNull(SaveManager.getInstance().getGuestData("Alice"));
    }
}
