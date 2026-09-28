package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.networking.identity.PlayerIdentity;
import com.game.save.PlayerData;
import com.game.save.SaveManager;
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
 * Guest characters belong to the host's world (like Stardew Valley farmhands): a joining player
 * picks any character nobody is playing, or creates a new one. Characters keep their inventory,
 * level and position across sessions.
 */
class GuestCharacterTest extends GameTestBase {
    private static final PlayerIdentity MACHINE_A = new PlayerIdentity("test", "machine-a", null);
    private static final PlayerIdentity MACHINE_B = new PlayerIdentity("test", "machine-b", null);

    private MultiplayerRig rig;

    @BeforeEach
    void startHost() {
        rig = new MultiplayerRig(START_LEVEL);
    }

    @AfterEach
    void stop() {
        rig.close();
    }

    // ========== Picking and creating ==========

    @Test
    void anEmptyWorldListsNoCharacters() {
        TestWorld guest = rig.connect(MACHINE_A);
        assertEquals(0, guest.presenter.characters.length);
        assertNull(guest.world.getLocalPlayer(), "nobody joins before choosing");
    }

    @Test
    void aNewCharacterIsSavedInTheWorldUnderItsName() {
        rig.join(MACHINE_A, "Alice");

        PlayerData saved = MultiplayerRig.savedCharacter("Alice");
        assertNotNull(saved, "listed right away, before any save");
        assertEquals("test:machine-a", saved.lastPlayedBy);
    }

    @Test
    void anyMachineCanPlayACharacterNobodyIsUsing() {
        TestWorld alice = rig.join(MACHINE_A, "Alice");
        alice.world.getLocalPlayer().getInventory().addItem(ItemFactory.create("wood", 3));
        leave(alice);

        TestWorld fromOtherMachine = rig.join(MACHINE_B, "Alice");
        assertEquals(3, fromOtherMachine.world.getLocalPlayer().getInventory().countItem("wood"));
    }

    @Test
    void oneMachineCanKeepSeveralCharacters() {
        TestWorld alice = rig.join(MACHINE_A, "Alice");
        alice.world.getLocalPlayer().getInventory().addItem(ItemFactory.create("wood", 3));
        leave(alice);

        TestWorld bob = rig.join(MACHINE_A, "Bob");
        assertEquals(0, bob.world.getLocalPlayer().getInventory().countItem("wood"), "Bob is a new character");
        leave(bob);

        TestWorld aliceAgain = rig.join(MACHINE_A, "Alice");
        assertEquals(3, aliceAgain.world.getLocalPlayer().getInventory().countItem("wood"));
    }

    @Test
    void theListShowsWhichCharactersAreInUseAndWhichAreYours() {
        rig.join(MACHINE_A, "Alice");
        leave(rig.join(MACHINE_B, "Bob"));

        TestWorld chooser = rig.connect(MACHINE_B);
        Packets.CharacterInfo alice = chooser.presenter.character("Alice");
        Packets.CharacterInfo bob = chooser.presenter.character("Bob");
        assertTrue(alice.inUse);
        assertFalse(alice.lastPlayedByYou);
        assertFalse(bob.inUse);
        assertTrue(bob.lastPlayedByYou);
        assertEquals("Bob", chooser.presenter.characters[0].name, "your characters come first");
    }

    @Test
    void aCharacterInUseCannotBePicked() {
        rig.join(MACHINE_A, "Alice");
        TestWorld chooser = rig.connect(MACHINE_B);
        int lists = chooser.presenter.characterLists;

        chooser.world.playCharacter(chooser.presenter.character("Alice").id);
        rig.runUntil(() -> chooser.presenter.characterLists > lists, "refusal");

        assertEquals("Alice is already being played.", chooser.presenter.characterMessage);
        assertNull(chooser.world.getLocalPlayer());
    }

    @Test
    void newCharacterNamesMustBeUnique() {
        leave(rig.join(MACHINE_A, "Alice"));
        TestWorld chooser = rig.connect(MACHINE_B);
        int lists = chooser.presenter.characterLists;

        chooser.world.createCharacter(" alice ");
        rig.runUntil(() -> chooser.presenter.characterLists > lists, "refusal");

        assertEquals("There is already a character named alice.", chooser.presenter.characterMessage);
        assertNull(chooser.world.getLocalPlayer());
        assertEquals(1, SaveManager.getInstance().getGuestCharacters().size());
    }

    @Test
    void guestsStillChoosingSeeCharactersBecomeFreeAndTaken() {
        TestWorld alice = rig.join(MACHINE_A, "Alice");
        TestWorld chooser = rig.connect(MACHINE_B);
        assertTrue(chooser.presenter.character("Alice").inUse);

        leave(alice);
        rig.runUntil(() -> !chooser.presenter.character("Alice").inUse, "Alice shown as free");

        rig.join(MACHINE_A, "Carol");
        rig.runUntil(() -> chooser.presenter.character("Carol") != null
            && chooser.presenter.character("Carol").inUse, "Carol shown as taken");
    }

    @Test
    void charactersFromOlderSavesAreListed() {
        PlayerData byName = savedAt(null, 0, 0); // Saved before names were stored: keyed by name
        byName.inventory = inventoryWith("stone", 5);
        SaveManager.getInstance().putGuestData("Alice", byName);
        PlayerData byIdentity = savedAt(null, 0, 0); // Saved per machine
        byIdentity.displayName = "Zed";
        SaveManager.getInstance().putGuestData("local:3f2a", byIdentity);

        TestWorld chooser = rig.connect(MACHINE_A);
        assertNotNull(chooser.presenter.character("Zed"));
        chooser.world.playCharacter(chooser.presenter.character("Alice").id);
        rig.runUntil(() -> chooser.world.getLocalPlayer() != null, "joined as Alice");

        assertEquals(5, chooser.world.getLocalPlayer().getInventory().countItem("stone"));
    }

    // ========== Where a returning character starts ==========

    @Test
    void aReturningCharacterContinuesInTheirLevelAtTheirPosition() {
        TestWorld alice = rig.join(MACHINE_A, "Alice");
        alice.world.changeLevel(OTHER_LEVEL, null);
        walkRight(alice, 0.4f);
        Vector2 leftAt = alice.world.getLocalPlayer().getTransform().getPosition().cpy();
        rig.runUntil(() -> hostCopyPosition() != null && hostCopyPosition().dst(leftAt) < 0.5f, "host copy caught up");

        leave(alice);

        TestWorld again = rig.join(MACHINE_B, "Alice");
        assertEquals(OTHER_LEVEL, again.world.getCurrentInstance().getLevelId());
        assertEquals(0, again.world.getLocalPlayer().getTransform().getPosition().dst(leftAt), 0.5f);
    }

    @Test
    void aBlockedSavedPositionFallsBackToTheLevelsSpawnPoint() {
        Vector2 wall = findBlockedSpot(OTHER_LEVEL);
        SaveManager.getInstance().putGuestData("character:alice", named("Alice", savedAt(OTHER_LEVEL, wall.x, wall.y)));

        TestWorld alice = rig.join(MACHINE_A, "Alice");

        LevelInstance level = alice.world.getCurrentInstance();
        assertEquals(OTHER_LEVEL, level.getLevelId());
        assertEquals(level.getSpawnPosition(null), alice.world.getLocalPlayer().getTransform().getPosition());
    }

    @Test
    void anUnknownSavedLevelFallsBackToJoiningTheHost() {
        SaveManager.getInstance().putGuestData("character:alice", named("Alice", savedAt("Maps/no_such_level.tmx", 10, 10)));

        TestWorld alice = rig.join(MACHINE_A, "Alice");

        assertEquals(START_LEVEL, alice.world.getCurrentInstance().getLevelId());
        assertEquals(rig.host.world.getLocalPlayer().getTransform().getPosition(),
            alice.world.getLocalPlayer().getTransform().getPosition());
    }

    @Test
    void theHostStoppingRecordsWhereConnectedGuestsWere() {
        TestWorld alice = rig.join(MACHINE_A, "Alice");
        walkRight(alice, 0.3f);
        Vector2 at = alice.world.getLocalPlayer().getTransform().getPosition().cpy();
        rig.runUntil(() -> hostCopyPosition() != null && hostCopyPosition().dst(at) < 0.5f, "host copy caught up");

        rig.host.world.stopMultiplayer();

        PlayerData saved = MultiplayerRig.savedCharacter("Alice");
        assertEquals(START_LEVEL, saved.levelId);
        assertEquals(at.x, saved.x, 0.5f);
    }

    // ========== Helpers ==========

    private void leave(TestWorld guest) {
        int before = guestCopies();
        rig.disconnect(guest);
        rig.runUntil(() -> guestCopies() < before, "guest left");
    }

    private int guestCopies() {
        return (int) rig.host.players.getAllPlayers().stream().filter(PlayerEntity::isNetworkControlled).count();
    }

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

    private static PlayerData named(String name, PlayerData data) {
        data.displayName = name;
        return data;
    }

    private static com.game.save.InventoryData inventoryWith(String itemId, int quantity) {
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
}
