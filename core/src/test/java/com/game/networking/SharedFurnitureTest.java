package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.furniture.ChestEntity;
import com.game.systems.furniture.FurnitureManager;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import com.game.testsupport.LevelSpots;
import com.game.testsupport.MultiplayerRig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Furniture works for every player: visible, solid, placeable by guests, chests one-at-a-time.
 */
class SharedFurnitureTest extends GameTestBase {
    private MultiplayerRig rig;
    private TestWorld host;
    private TestWorld guest;
    private Vector2 spot; // Free floor next to both players (they start on the same spot)

    @BeforeEach
    void start() {
        rig = new MultiplayerRig(START_LEVEL);
        host = rig.host;
        guest = rig.join("Bob");
        PlayerEntity hostPlayer = host.world.getLocalPlayer();
        spot = LevelSpots.freeFurnitureSpot(host.world.getCurrentInstance().getWorld(),
            hostPlayer.getTransform().getX(), hostPlayer.getTransform().getY(), 20);
    }

    @AfterEach
    void stop() {
        rig.close();
    }

    @Test
    void guestsSeeTheHostsFurnitureAndCantWalkThroughIt() {
        assertTrue(placeAndWait(host, spot));

        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");
        assertFalse(guest.world.getCurrentInstance().getWorld().isPositionWalkable(spot.x, spot.y, 16, 16),
            "the chest blocks the guest");
    }

    @Test
    void guestsCanPlaceFurnitureForEveryone() {
        assertTrue(placeAndWait(guest, spot), "host accepts the guest's placement");

        rig.runUntil(() -> chestAt(host, spot) != null, "host has the guest's chest");
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees it too");
        assertEquals(1, FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).size(), "saved by the host");
    }

    @Test
    void theHostRejectsPlacementsInWalls() {
        Vector2 wall = LevelSpots.blockedSpot(host.world.getCurrentInstance().getWorld());
        assertFalse(placeAndWait(guest, wall));
        assertTrue(FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).isEmpty());
    }

    @Test
    void onlyOnePlayerCanUseAChestAtATime() {
        placeAndWait(host, spot);
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");

        assertTrue(openAndWait(guest, chestAt(guest, spot)), "guest may open it");
        assertFalse(openAndWait(host, chestAt(host, spot)), "host sees it's in use");

        guest.world.closeChest(chestAt(guest, spot));
        rig.runUntil(() -> openAndWait(host, chestAt(host, spot)), "host may open it once the guest closed it");
        assertFalse(openAndWait(guest, chestAt(guest, spot)), "and now the guest has to wait");
    }

    @Test
    void whatAGuestPutsInAChestEndsUpInTheHostsChest() {
        placeAndWait(host, spot);
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");
        ChestEntity guestView = chestAt(guest, spot);

        assertTrue(openAndWait(guest, guestView));
        guestView.getContainer().addItem(ItemFactory.create("wood", 9));

        rig.runUntil(() -> chestAt(host, spot).getContainer().countItem("wood") == 9, "host chest gets the wood");
    }

    @Test
    void guestsSeeTheCurrentContentsWhenTheyOpenAChest() {
        placeAndWait(host, spot);
        chestAt(host, spot).getContainer().addItem(ItemFactory.create("stone", 4));
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");

        assertTrue(openAndWait(guest, chestAt(guest, spot)));
        assertEquals(4, chestAt(guest, spot).getContainer().countItem("stone"));
    }

    @Test
    void aGuestLeavingGivesTheChestBack() {
        placeAndWait(host, spot);
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");
        assertTrue(openAndWait(guest, chestAt(guest, spot)));

        rig.disconnect(guest);
        rig.runUntil(() -> openAndWait(host, chestAt(host, spot)), "host can use the chest after the guest left");
    }

    @Test
    void guestsCanPickUpEmptyChests() {
        placeAndWait(host, spot);
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");

        assertTrue(guest.world.pickUpFurniture(chestAt(guest, spot)));

        PlayerEntity guestPlayer = guest.world.getLocalPlayer();
        rig.runUntil(() -> guestPlayer.getInventory().countItem("wooden_chest") == 1, "guest receives the chest item");
        rig.runUntil(() -> chestAt(host, spot) == null && chestAt(guest, spot) == null, "chest gone everywhere");
        assertTrue(FurnitureManager.getInstance().getFurnitureForLevel(START_LEVEL).isEmpty());
    }

    @Test
    void theHostCantPickUpAChestAGuestIsUsing() {
        placeAndWait(host, spot);
        rig.runUntil(() -> chestAt(guest, spot) != null, "guest sees the chest");
        assertTrue(openAndWait(guest, chestAt(guest, spot)));

        assertFalse(host.world.pickUpFurniture(chestAt(host, spot)));
        assertNotNull(chestAt(host, spot));
    }

    // ========== Helpers ==========

    private boolean placeAndWait(TestWorld who, Vector2 at) {
        AtomicReference<Boolean> result = new AtomicReference<>();
        who.world.requestPlaceFurniture("wooden_chest", at.x, at.y, result::set);
        rig.runUntil(() -> result.get() != null, "placement answered");
        return result.get();
    }

    private boolean openAndWait(TestWorld who, ChestEntity chest) {
        AtomicReference<Boolean> result = new AtomicReference<>();
        who.world.openChest(chest, result::set);
        rig.runUntil(() -> result.get() != null, "chest request answered");
        return result.get();
    }

    private static ChestEntity chestAt(TestWorld who, Vector2 at) {
        for (Object obj : who.world.getCurrentInstance().getWorld().getGameObjects()) {
            if (obj instanceof ChestEntity chest && chest.getTransform().getPosition().epsilonEquals(at, 0.01f)) {
                return chest;
            }
        }
        return null;
    }
}
