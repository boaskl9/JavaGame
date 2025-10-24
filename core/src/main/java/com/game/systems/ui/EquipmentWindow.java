package com.game.systems.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.game.integration.WorldItemManager;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.inventory.PlayerEquipment;
import com.game.systems.inventory.PlayerInventory;
import com.game.systems.item.ItemStack;

/**
 * Window displaying player equipment slots.
 * Shows 6 equipment slots in a vertical list with labels.
 */
public class EquipmentWindow extends Window {
    private final PlayerInventory playerInventory;
    private final ItemSlotUI[] slots;
    private final ItemDragAndDropSystem dragAndDrop;

    public EquipmentWindow(PlayerInventory playerInventory, ItemDragAndDropSystem dragAndDrop, Skin skin) {
        super("Equipment", skin);
        this.padTop(20);

        this.playerInventory = playerInventory;
        this.dragAndDrop = dragAndDrop;
        this.slots = new ItemSlotUI[EquipmentSlot.values().length];

        buildUI(skin);

        setMovable(true);
        setResizable(false);
        pack();
    }

    private void buildUI(Skin skin) {
        // Clear any existing content
        clear();

        Table contentTable = new Table();
        contentTable.pad(10);

        // Create a slot for each equipment type
        EquipmentSlot[] equipmentSlots = EquipmentSlot.getDisplayOrder();

        for (int i = 0; i < equipmentSlots.length; i++) {
            EquipmentSlot equipSlot = equipmentSlots[i];

            // Create label for the slot
            Label slotLabel = new Label(equipSlot.getDisplayName(), skin);
            slotLabel.setFontScale(0.7f);

            // Create the item slot UI
            // Use slot index = ordinal of EquipmentSlot enum
            ItemSlotUI slot = new ItemSlotUI(equipSlot.ordinal(), ItemSlotUI.SlotType.EQUIPMENT,
                                             playerInventory.getEquipment(), skin);
            slots[i] = slot;

            // Register slot with drag & drop system
            dragAndDrop.registerSlot(slot);

            // Add label and slot to content table
            contentTable.add(slotLabel).left().padRight(5);
            contentTable.add(slot).size(48, 48).row();
        }

        add(contentTable);
    }

    /**
     * Refresh the equipment window with current equipped items.
     * Updates item icons from the WorldItemManager.
     */
    public void refresh(WorldItemManager worldItemManager) {
        PlayerEquipment equipment = playerInventory.getEquipment();
        EquipmentSlot[] equipmentSlots = EquipmentSlot.getDisplayOrder();

        for (int i = 0; i < equipmentSlots.length; i++) {
            EquipmentSlot equipSlot = equipmentSlots[i];
            ItemStack equipped = equipment.getEquipped(equipSlot);

            ItemSlotUI slotUI = slots[i];

            if (equipped != null) {
                // Get texture from WorldItemManager
                String iconPath = equipped.getDefinition().getIconPath();
                TextureRegion icon = worldItemManager.getTexture(iconPath);

                slotUI.setItemStack(equipped);
                slotUI.setItemIcon(icon);
            } else {
                slotUI.setItemStack(null);
                slotUI.setItemIcon(null);
            }
        }
    }

    /**
     * Get all equipment slot UI widgets.
     */
    public ItemSlotUI[] getSlots() {
        return slots;
    }

    /**
     * Get a specific equipment slot UI by equipment slot type.
     */
    public ItemSlotUI getSlot(EquipmentSlot equipmentSlot) {
        return slots[equipmentSlot.ordinal()];
    }
}
