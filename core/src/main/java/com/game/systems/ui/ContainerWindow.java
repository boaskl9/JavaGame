package com.game.systems.ui;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.game.systems.inventory.BagInstance;
import com.game.systems.inventory.InventoryContainer;
import com.game.systems.item.ItemStack;

/**
 * Unified window for displaying any inventory container (default inventory or bags).
 * This eliminates code duplication between inventory and bag windows.
 */
public class ContainerWindow extends Window {
    private final InventoryContainer container;
    private final ItemSlotUI.SlotType slotType;
    private final ItemDragAndDropSystem dragAndDrop;
    private final Skin skin;

    private ItemSlotUI[] slots;

    /**
     * Creates a window for a container (inventory or bag).
     * @param title Window title
     * @param container The inventory container to display
     * @param slotType The type of slots (DEFAULT_INVENTORY or BAG_INVENTORY)
     * @param dragAndDrop Drag and drop system
     * @param skin UI skin
     */
    public ContainerWindow(String title, InventoryContainer container, ItemSlotUI.SlotType slotType,
                          ItemDragAndDropSystem dragAndDrop, Skin skin) {
        super(title, skin, "default");
        this.container = container;
        this.slotType = slotType;
        this.dragAndDrop = dragAndDrop;
        this.skin = skin;

        setMovable(false);
        setResizable(false);

        buildUI();
        refresh();
    }

    private void buildUI() {
        Table content = new Table();
        content.pad(10);

        // Create slots grid
        Table slotsTable = new Table();
        int slotCount = container.getSize();
        slots = new ItemSlotUI[slotCount];

        int columns = 8; // 8 slots per row
        for (int i = 0; i < slotCount; i++) {
            ItemSlotUI slot = new ItemSlotUI(
                i,
                slotType,
                container,
                skin
            );
            slots[i] = slot;
            dragAndDrop.registerSlot(slot);

            slotsTable.add(slot).size(48, 48).pad(2);
            if ((i + 1) % columns == 0) {
                slotsTable.row();
            }
        }

        content.add(slotsTable).row();

        add(content);
        pack();
    }

    /**
     * Refreshes the display from the container data.
     */
    public void refresh() {
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = container.getItem(i);
            slots[i].setItemStack(stack);
        }
    }

    public ItemSlotUI[] getSlots() {
        return slots;
    }

    public InventoryContainer getContainer() {
        return container;
    }
}
