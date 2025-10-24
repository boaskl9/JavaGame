package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.game.integration.WorldItemManager;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.game.systems.item.ItemStack;

import java.util.List;

/**
 * Debug window that displays all registered items in the game.
 * Items can be dragged to inventory or dropped to world.
 */
public class ItemBrowserWindow extends Window {
    private final ItemDragAndDropSystem dragAndDrop;
    private final WorldItemManager worldItemManager;
    private final Table itemGrid;
    private final ScrollPane scrollPane;
    private ItemSpawnListener spawnListener;
    private TooltipLabel tooltip;

    public interface ItemSpawnListener {
        void onItemSpawn(ItemStack itemStack);
    }

    public ItemBrowserWindow(ItemDragAndDropSystem dragAndDrop, WorldItemManager worldItemManager,
                            TooltipLabel tooltip, Skin skin) {
        super("Item Browser (Debug)", skin);

        this.dragAndDrop = dragAndDrop;
        this.worldItemManager = worldItemManager;
        this.tooltip = tooltip;
        this.padTop(20);

        setMovable(true);
        setResizable(false);

        // Create grid for items (4 columns)
        itemGrid = new Table();
        itemGrid.defaults().size(48, 48).pad(2);

        // Create scroll pane
        scrollPane = new ScrollPane(itemGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // Only vertical scrolling

        // Add scroll pane to window
        add(scrollPane).size(220, 400).pad(10);

        // Populate with items
        populateItems(skin);

        pack();
    }

    public void setSpawnListener(ItemSpawnListener listener) {
        this.spawnListener = listener;
    }

    /**
     * Populates the grid with all registered items.
     */
    private void populateItems(Skin skin) {
        java.util.Collection<ItemDefinition> allItems = ItemRegistry.getAll();

        int column = 0;
        for (ItemDefinition itemDef : allItems) {
            // Create a pseudo-slot for the item
            ItemSlotUI slot = createItemSlot(itemDef, skin);

            // Add to grid
            itemGrid.add(slot);

            column++;
            if (column >= 4) {
                itemGrid.row();
                column = 0;
            }
        }

        System.out.println("ItemBrowserWindow: Populated with " + allItems.size() + " items");
    }

    /**
     * Creates a special item slot for the browser that spawns new items on drag.
     */
    private ItemSlotUI createItemSlot(ItemDefinition itemDef, Skin skin) {
        // Create a normal slot but mark it as a browser slot
        ItemSlotUI slot = new ItemSlotUI(0, ItemSlotUI.SlotType.DEFAULT_INVENTORY, null, skin);

        // Set the item
        ItemStack stack = new ItemStack(itemDef, 1);
        slot.setItemStack(stack);

        // Set icon if available
        String iconPath = itemDef.getIconPath();
        if (iconPath != null && !iconPath.isEmpty()) {
            TextureRegion icon = worldItemManager.getTexture(iconPath);
            slot.setItemIcon(icon);
        }

        // Set up hover listener for tooltips
        slot.setHoverListener(new ItemSlotUI.SlotHoverListener() {
            @Override
            public void onHoverEnter(ItemSlotUI slot, float x, float y) {
                String tooltipText = slot.getTooltipText();
                if (tooltipText != null && tooltip != null) {
                    tooltip.show(tooltipText, x, y);
                }
            }

            @Override
            public void onHoverExit(ItemSlotUI slot) {
                if (tooltip != null) {
                    tooltip.hide();
                }
            }

            @Override
            public void onMouseDown(ItemSlotUI slot) {
                // Hide tooltip on mouse down
                if (tooltip != null) {
                    tooltip.hide();
                }
            }
        });

        // Set up double-click listener to spawn items to world
        slot.setDoubleClickListener(new ItemSlotUI.SlotDoubleClickListener() {
            @Override
            public void onDoubleClick(ItemSlotUI slot) {
                if (spawnListener != null && slot.getItemStack() != null) {
                    // Create a fresh stack to spawn
                    ItemStack spawnStack = new ItemStack(itemDef, 1);
                    spawnListener.onItemSpawn(spawnStack);
                    System.out.println("ItemBrowserWindow: Spawned " + itemDef.getName() + " to world");
                }
            }
        });

        // Register with drag & drop system
        dragAndDrop.registerSlot(slot);

        return slot;
    }

    /**
     * Refreshes all browser slots to restore their items after drag.
     * Called after items are dragged from the browser.
     */
    public void refreshBrowserSlots() {
        // Re-populate all slots
        itemGrid.clearChildren();
        populateItems(getSkin());
    }

    /**
     * Positions the window in the center of the screen.
     */
    public void centerOnScreen() {
        float x = (Gdx.graphics.getWidth() - getWidth()) / 2f;
        float y = (Gdx.graphics.getHeight() - getHeight()) / 2f;
        setPosition(x, y);
    }
}
