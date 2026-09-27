package com.game.systems.inventory;

import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest extends GameTestBase {

    @Test
    void matchingItemsStackUpToTheirLimit() {
        InventoryContainer container = new InventoryContainer(3);
        assertNull(container.addItem(ItemFactory.create("wood", 40)));
        assertNull(container.addItem(ItemFactory.create("wood", 40)));

        assertEquals(80, container.countItem("wood"));
        assertEquals(64, container.getItem(0).getQuantity(), "first stack is filled to the max (64)");
        assertEquals(16, container.getItem(1).getQuantity());
    }

    @Test
    void whatDoesNotFitIsReturned() {
        InventoryContainer container = new InventoryContainer(1);
        container.addItem(ItemFactory.create("wood", 60));
        ItemStack remaining = container.addItem(ItemFactory.create("wood", 10));

        assertNotNull(remaining);
        assertEquals(6, remaining.getQuantity());
        assertTrue(container.isFull());
    }

    @Test
    void aStackNeverHoldsMoreThanTheItemsMaximum() {
        assertEquals(64, ItemFactory.create("wood", 100).getQuantity());
    }

    @Test
    void addingDoesNotChangeTheCallersStack() {
        InventoryContainer container = new InventoryContainer(4);
        ItemStack stack = ItemFactory.create("wood", 5);
        container.addItem(stack);
        assertEquals(5, stack.getQuantity(), "a network grant or world item must not be modified by adding it");
    }

    @Test
    void inventorySaveDataRoundTrip() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.addItem(ItemFactory.create("wood", 30));
        inventory.addItem(ItemFactory.create("stone", 2));
        inventory.getEquipment().equipItem(EquipmentSlot.WEAPON, ItemFactory.create("wooden_sword", 1));

        PlayerInventory loaded = new PlayerInventory();
        loaded.importSaveData(inventory.exportSaveData());

        assertEquals(30, loaded.countItem("wood"));
        assertEquals(2, loaded.countItem("stone"));
        assertEquals("wooden_sword", loaded.getEquipment().getEquipped(EquipmentSlot.WEAPON).getDefinition().getId());
    }
}
