package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.networking.identity.PlayerIdentity;
import com.game.save.PlayerData;
import com.game.save.SaveManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.MultiplayerRig;
import com.game.world.LevelInstance;
import com.game.world.LevelInstanceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guests keep their character (inventory, level, position) across sessions, keyed by identity.
 */
class GuestCharacterTest extends GameTestBase {
    private static final PlayerIdentity ALICE = new PlayerIdentity("test", "alice-id", "Alice");

    private MultiplayerRig rig;

    @BeforeEach
    void startHost() {
        rig = new MultiplayerRig(START_LEVEL);
    }

    @AfterEach
    void stop() {
        rig.close();
    }

    @Test
    void aReturningGuestContinuesInTheirLevelAtTheirPosition() {
        TestWorld alice = rig.join(ALICE);
        alice.world.changeLevel(OTHER_LEVEL, null);
        walkRight(alice, 0.4f);
        Vector2 leftAt = alice.world.getLocalPlayer().getTransform().getPosition().cpy();
        rig.runUntil(() -> hostCopyPosition() != null && hostCopyPosition().dst(leftAt) < 0.5f, "host copy caught up");

        rig.disconnect(alice);
        rig.runUntil(() -> hostCopyPosition() == null, "Alice left");

        TestWorld again = rig.join(ALICE);
        assertEquals(OTHER_LEVEL, again.world.getCurrentInstance().getLevelId());
        assertEquals(0, again.world.getLocalPlayer().getTransform().getPosition().dst(leftAt), 0.5f);
    }

    @Test
    void aBlockedSavedPositionFallsBackToTheLevelsSpawnPoint() {
        Vector2 wall = findBlockedSpot(OTHER_LEVEL);
        SaveManager.getInstance().putGuestData(ALICE.key(), savedAt(OTHER_LEVEL, wall.x, wall.y));

        TestWorld alice = rig.join(ALICE);

        LevelInstance level = alice.world.getCurrentInstance();
        assertEquals(OTHER_LEVEL, level.getLevelId());
        assertEquals(level.getSpawnPosition(null), alice.world.getLocalPlayer().getTransform().getPosition());
    }

    @Test
    void anUnknownSavedLevelFallsBackToJoiningTheHost() {
        SaveManager.getInstance().putGuestData(ALICE.key(), savedAt("Maps/no_such_level.tmx", 10, 10));

        TestWorld alice = rig.join(ALICE);

        assertEquals(START_LEVEL, alice.world.getCurrentInstance().getLevelId());
        assertEquals(rig.host.world.getLocalPlayer().getTransform().getPosition(),
            alice.world.getLocalPlayer().getTransform().getPosition());
    }

    @Test
    void theIdentityNotTheNameDecidesWhichCharacterYouGet() {
        TestWorld alice = rig.join(ALICE);
        alice.world.getLocalPlayer().getInventory().addItem(ItemFactory.create("wood", 3));
        rig.disconnect(alice);
        rig.runUntil(() -> hostCopyPosition() == null, "Alice left");

        TestWorld renamed = rig.join(new PlayerIdentity("test", "alice-id", "Ally"));
        assertEquals(3, renamed.world.getLocalPlayer().getInventory().countItem("wood"), "same ID, new name: same character");
        rig.disconnect(renamed);
        rig.runUntil(() -> hostCopyPosition() == null, "Ally left");

        TestWorld impostor = rig.join(new PlayerIdentity("test", "someone-else", "Alice"));
        assertEquals(0, impostor.world.getLocalPlayer().getInventory().countItem("wood"), "same name, other ID: new character");
    }

    @Test
    void charactersSavedByNameBeforeIdentitiesExistedAreAdopted() {
        PlayerData legacy = savedAt(null, 0, 0);
        SaveManager.getInstance().putGuestData("Alice", legacy);
        TestWorld temp = new TestWorld(false);
        legacy.inventory = inventoryWith(temp, "stone", 5);

        TestWorld alice = rig.join(ALICE);

        assertEquals(5, alice.world.getLocalPlayer().getInventory().countItem("stone"));
        assertNull(SaveManager.getInstance().getGuestData("Alice"), "legacy entry is moved, not copied");
        assertNotNull(SaveManager.getInstance().getGuestData(ALICE.key()));
    }

    @Test
    void twoCopiesWithTheSameIdentityGetSeparateCharacters() {
        TestWorld first = rig.join(ALICE);
        TestWorld second = rig.join(ALICE);
        assertNotEquals(first.world.getLocalPlayer().getPlayerId(), second.world.getLocalPlayer().getPlayerId());

        rig.disconnect(first);
        rig.disconnect(second);
        rig.runUntil(() -> SaveManager.getInstance().getGuestData(ALICE.key() + "#2") != null, "second copy saved separately");
        assertNotNull(SaveManager.getInstance().getGuestData(ALICE.key()));
    }

    @Test
    void theHostStoppingRecordsWhereConnectedGuestsWere() {
        TestWorld alice = rig.join(ALICE);
        walkRight(alice, 0.3f);
        Vector2 at = alice.world.getLocalPlayer().getTransform().getPosition().cpy();
        rig.runUntil(() -> hostCopyPosition() != null && hostCopyPosition().dst(at) < 0.5f, "host copy caught up");

        rig.host.world.stopMultiplayer();

        PlayerData saved = SaveManager.getInstance().getGuestData(ALICE.key());
        assertEquals(START_LEVEL, saved.levelId);
        assertEquals(at.x, saved.x, 0.5f);
        assertEquals("Alice", saved.displayName);
    }

    // ========== Helpers ==========

    private void walkRight(TestWorld guest, float seconds) {
        guest.presenter.input.move(1, 0);
        rig.runFor(seconds);
        guest.presenter.input.move(0, 0);
        rig.runFor(0.05f);
    }

    /** Position of the (single) guest's copy on the host, in any level; null if none. */
    private Vector2 hostCopyPosition() {
        for (PlayerEntity player : rig.host.players.getAllPlayers()) {
            if (player.isNetworkControlled()) return player.getTransform().getPosition();
        }
        return null;
    }

    private static PlayerData savedAt(String levelId, float x, float y) {
        PlayerData data = new PlayerData(x, y, 20, 28, null);
        data.levelId = levelId;
        return data;
    }

    private static com.game.save.InventoryData inventoryWith(TestWorld scratch, String itemId, int quantity) {
        com.game.systems.inventory.PlayerInventory inventory = new com.game.systems.inventory.PlayerInventory();
        inventory.addItem(ItemFactory.create(itemId, quantity));
        return inventory.exportSaveData();
    }

    /** A spot in the level where a player can't stand (inside a wall). */
    private static Vector2 findBlockedSpot(String levelId) {
        LevelInstance level = LevelInstanceFactory.create(levelId, LevelInstanceFactory.Mode.REPLICA);
        int w = level.getWorld().getWorldWidth() * 16;
        int h = level.getWorld().getWorldHeight() * 16;
        for (int y = 16; y < h - 16; y += 16) {
            for (int x = 16; x < w - 16; x += 16) {
                if (!level.getWorld().isPositionWalkable(x + 4, y, 8, 4)) {
                    return new Vector2(x, y);
                }
            }
        }
        throw new AssertionError(levelId + " has no walls?");
    }

    @SuppressWarnings("unused")
    private static int count(LevelInstance level, Class<? extends GameObject> type) {
        return (int) level.getWorld().getGameObjects().stream().filter(type::isInstance).count();
    }
}
