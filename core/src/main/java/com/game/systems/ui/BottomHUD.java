package com.game.systems.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.game.systems.inventory.BagInstance;
import com.game.systems.inventory.PlayerInventory;

/**
 * Bottom HUD bar that shows bag equipment slots and other persistent UI elements.
 * This is always visible and anchored to the bottom of the screen.
 */
public class BottomHUD extends Table {
    private final PlayerInventory playerInventory;
    private final ItemDragAndDropSystem dragAndDrop;
    private final Skin skin;

    private ItemSlotUI[] bagEquipmentSlots;

    public BottomHUD(PlayerInventory playerInventory, ItemDragAndDropSystem dragAndDrop, Skin skin) {
        super(skin);
        this.playerInventory = playerInventory;
        this.dragAndDrop = dragAndDrop;
        this.skin = skin;

        buildHUD();
    }

    private void buildHUD() {
        // Set background
        Drawable background = skin.getDrawable("window");
        setBackground(background);

        pad(10);

        // Add spacer to push bag slots to the right
        add().expandX();

        // Create bag equipment slots on the right side
        Table bagSlotsTable = new Table();
        int maxBagSlots = playerInventory.getMaxBagSlots();
        bagEquipmentSlots = new ItemSlotUI[maxBagSlots];

        for (int i = 0; i < maxBagSlots; i++) {
            ItemSlotUI bagSlot = new ItemSlotUI(
                i,
                ItemSlotUI.SlotType.BAG_EQUIPMENT,
                playerInventory,
                skin
            );
            bagEquipmentSlots[i] = bagSlot;
            dragAndDrop.registerSlot(bagSlot);

            bagSlotsTable.add(bagSlot).size(48, 48).pad(2);
        }

        add(bagSlotsTable).right();
    }

    /**
     * Refreshes the bag equipment slots from PlayerInventory.
     * Call refreshWithTextures() instead if you want to show bag icons.
     */
    public void refresh() {
        // Simple refresh - just clear icons
        for (int i = 0; i < bagEquipmentSlots.length; i++) {
            BagInstance bag = playerInventory.getBag(i);
            bagEquipmentSlots[i].setItemStack(null);
            bagEquipmentSlots[i].setItemIcon(null);
        }
    }

    /**
     * Refreshes the bag equipment slots with texture icons.
     * @param getBagTexture Function to get texture for a bag icon path
     */
    public void refreshWithTextures(java.util.function.Function<String, TextureRegion> getBagTexture) {
        for (int i = 0; i < bagEquipmentSlots.length; i++) {
            BagInstance bag = playerInventory.getBag(i);


            if (bag != null) {
                System.out.println("Bag: " + bag.toString());

                // Set the bag icon if available
                String iconPath = bag.getDefinition().getIconPath();
                if (iconPath != null && getBagTexture != null) {
                    TextureRegion icon = getBagTexture.apply(iconPath);
                    bagEquipmentSlots[i].setItemIcon(icon);
                } else {
                    bagEquipmentSlots[i].setItemIcon(null);
                }

                // Create a dummy ItemStack so the slot shows quantity = 1 (indicates bag is present)
                // This is a workaround until bags become proper items
                bagEquipmentSlots[i].setItemStack(new com.game.systems.item.ItemStack(
                    new com.game.systems.item.ItemDefinition(
                        bag.getDefinition().getId(),
                        bag.getDefinition().getName(),
                        bag.getDefinition().getDescription(),
                        com.game.systems.item.ItemType.RESOURCE,
                        1,
                        iconPath, false
                    ),
                    1
                ));
            } else {
                // Empty slot
                bagEquipmentSlots[i].setItemStack(null);
                bagEquipmentSlots[i].setItemIcon(null);
            }
        }
    }

    public ItemSlotUI[] getBagEquipmentSlots() {
        return bagEquipmentSlots;
    }
}
