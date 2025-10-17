package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.integration.WorldItemManager;
import com.game.systems.inventory.InventoryContainer;
import com.game.systems.inventory.PlayerInventory;
import com.game.systems.inventory.BagInstance;
import com.game.systems.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Redesigned UI Manager with bottom HUD bar and proper styling.
 */
public class UIManagerNew {
    private Stage stage;
    private Skin skin;
    private PlayerInventory playerInventory;
    private ItemDragAndDropSystem dragAndDrop;
    private WorldItemManager worldItemManager;

    private BottomHUD bottomHUD;
    private ContainerWindow inventoryWindow;
    private Map<BagInstance, ContainerWindow> bagWindows;
    private Map<BagInstance, Integer> bagSlotIndices; // Track which slot each bag is in
    private boolean inventoryOpen;

    private ItemDropCallback itemDropCallback;

    public interface ItemDropCallback {
        void onDropItemToWorld(ItemStack itemStack);
    }

    public UIManagerNew(PlayerInventory playerInventory, WorldItemManager worldItemManager) {
        this.playerInventory = playerInventory;
        this.worldItemManager = worldItemManager;
        this.bagWindows = new HashMap<>();
        this.bagSlotIndices = new HashMap<>();
        this.inventoryOpen = false;

        // Create stage with screen viewport
        stage = new Stage(new ScreenViewport());
        System.out.println("UIManagerNew: Stage created");

        // Load UI skin
        skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"));

        // Create drag and drop handler
        dragAndDrop = new ItemDragAndDropSystem(skin);
        setupDragAndDropListener();

        // Create bottom HUD (always visible)
        bottomHUD = new BottomHUD(playerInventory, dragAndDrop, skin);

        // Position at bottom of screen with specific height
        float hudHeight = 60; // Height of the bottom bar
        bottomHUD.setSize(Gdx.graphics.getWidth(), hudHeight);
        bottomHUD.setPosition(0, 0); // Bottom-left corner

        stage.addActor(bottomHUD);

        // Create inventory window (hidden by default)
        inventoryWindow = new ContainerWindow(
            "Inventory",
            playerInventory.getDefaultInventory(),
            ItemSlotUI.SlotType.DEFAULT_INVENTORY,
            dragAndDrop,
            skin
        );
        inventoryWindow.setVisible(false);
        stage.addActor(inventoryWindow);

        System.out.println("UIManagerNew: Window movable = " + inventoryWindow.isMovable());
        System.out.println("UIManagerNew: Window visible = " + inventoryWindow.isVisible());

        // Position inventory window
        positionInventoryWindow();
    }

    private void positionInventoryWindow() {
        // Position at bottom right corner
        float hudHeight = 60;
        float padding = 10;
        float windowX = Gdx.graphics.getWidth() - inventoryWindow.getWidth() - padding;
        float windowY = hudHeight + padding; // Just above the bottom HUD
        inventoryWindow.setPosition(windowX, windowY);
    }

    private void setupDragAndDropListener() {
        dragAndDrop.setDropListener(new ItemDragAndDropSystem.ItemDropListener() {
            @Override
            public boolean onItemDrop(ItemSlotUI sourceSlot, ItemSlotUI targetSlot) {
                return handleItemTransfer(sourceSlot, targetSlot);
            }

            @Override
            public void onItemDropToWorld(ItemSlotUI sourceSlot) {
                handleItemDropToWorld(sourceSlot);
            }
        });
    }

    private boolean handleItemTransfer(ItemSlotUI sourceSlot, ItemSlotUI targetSlot) {
        if (sourceSlot == targetSlot) {
            return false;
        }

        ItemStack sourceStack = sourceSlot.getItemStack();
        ItemStack targetStack = targetSlot.getItemStack();

        if (sourceStack == null) {
            return false;
        }

        // Special handling for dragging FROM bag equipment slots (unequipping)
        if (sourceSlot.getSlotType() == ItemSlotUI.SlotType.BAG_EQUIPMENT) {
            return handleBagUnequip(sourceSlot, targetSlot);
        }

        // Special handling for dragging TO bag equipment slots (equipping)
        if (targetSlot.getSlotType() == ItemSlotUI.SlotType.BAG_EQUIPMENT) {
            return handleBagEquip(sourceSlot, targetSlot);
        }

        // Simple swap or merge logic
        if (targetStack == null) {
            // Move to empty slot
            targetSlot.setItemStack(sourceStack);
            sourceSlot.setItemStack(null);
            updateBackingData(sourceSlot, null);
            updateBackingData(targetSlot, sourceStack);
            refreshAllWindows();
            return true;
        } else if (targetStack.canMergeWith(sourceStack)) {
            // Merge stacks
            int overflow = targetStack.add(sourceStack.getQuantity());
            if (overflow > 0) {
                sourceStack.setQuantity(overflow);
            } else {
                sourceSlot.setItemStack(null);
                updateBackingData(sourceSlot, null);
            }
            updateBackingData(targetSlot, targetStack);
            refreshAllWindows();
            return true;
        } else {
            // Swap items
            targetSlot.setItemStack(sourceStack);
            sourceSlot.setItemStack(targetStack);
            updateBackingData(sourceSlot, targetStack);
            updateBackingData(targetSlot, sourceStack);
            refreshAllWindows();
            return true;
        }
    }

    /**
     * Handles unequipping a bag from a bag equipment slot.
     */
    private boolean handleBagUnequip(ItemSlotUI sourceSlot, ItemSlotUI targetSlot) {
        int sourceBagSlotIndex = sourceSlot.getSlotIndex();
        BagInstance sourceBag = playerInventory.getBag(sourceBagSlotIndex);

        if (sourceBag == null) {
            System.out.println("Cannot unequip: No bag in slot");
            return false;
        }

        // Check if bag is empty
        if (!sourceBag.isEmpty()) {
            System.out.println("Cannot unequip: Bag must be empty first");
            return false;
        }

        // Handle bag-to-bag slot swapping
        if (targetSlot.getSlotType() == ItemSlotUI.SlotType.BAG_EQUIPMENT) {
            int targetBagSlotIndex = targetSlot.getSlotIndex();
            BagInstance targetBag = playerInventory.getBag(targetBagSlotIndex);

            // Check if target bag is also empty (or doesn't exist)
            if (targetBag != null && !targetBag.isEmpty()) {
                System.out.println("Cannot swap: Target bag must be empty first");
                return false;
            }

            // Unequip source bag
            playerInventory.unequipBag(sourceBagSlotIndex);

            // Close source bag window
            ContainerWindow sourceBagWindow = bagWindows.get(sourceBag);
            if (sourceBagWindow != null) {
                sourceBagWindow.remove();
                bagWindows.remove(sourceBag);
                bagSlotIndices.remove(sourceBag);
            }

            // If target slot has a bag, unequip it too
            if (targetBag != null) {
                playerInventory.unequipBag(targetBagSlotIndex);

                // Close target bag window
                ContainerWindow targetBagWindow = bagWindows.get(targetBag);
                if (targetBagWindow != null) {
                    targetBagWindow.remove();
                    bagWindows.remove(targetBag);
                    bagSlotIndices.remove(targetBag);
                }

                // Equip target bag to source slot
                playerInventory.equipBag(targetBag, sourceBagSlotIndex);
            }

            // Equip source bag to target slot
            playerInventory.equipBag(sourceBag, targetBagSlotIndex);

            // Reopen bags FIRST, then refresh
            closeAllBagWindows();
            openBagWindows();
            refreshAllWindows();

            System.out.println("Swapped bags between slots " + sourceBagSlotIndex + " and " + targetBagSlotIndex);
            return true;
        }

        // Allow moving to regular inventory slots or bag inventory
        if (targetSlot.getSlotType() != ItemSlotUI.SlotType.DEFAULT_INVENTORY &&
            targetSlot.getSlotType() != ItemSlotUI.SlotType.BAG_INVENTORY) {
            System.out.println("Cannot unequip: Can only move bags to inventory or other bag slots");
            return false;
        }

        // Check if target slot is empty
        if (targetSlot.getItemStack() != null) {
            System.out.println("Cannot unequip: Target slot must be empty");
            return false;
        }

        // Create a bag item
        com.game.systems.item.ItemDefinition bagItemDef = com.game.systems.item.ItemRegistry.get("bag");
        if (bagItemDef == null) {
            System.out.println("Cannot unequip: Bag item definition not found");
            return false;
        }

        ItemStack bagItem = new ItemStack(bagItemDef, 1);

        // Unequip the bag
        playerInventory.unequipBag(sourceBagSlotIndex);

        // Close the bag window
        ContainerWindow bagWindow = bagWindows.get(sourceBag);
        if (bagWindow != null) {
            bagWindow.remove();
            bagWindows.remove(sourceBag);
            bagSlotIndices.remove(sourceBag);
        }

        // Add bag item to target slot
        targetSlot.setItemStack(bagItem);
        updateBackingData(targetSlot, bagItem);

        // Clear the equipment slot display
        sourceSlot.setItemStack(null);
        sourceSlot.setItemIcon(null);

        refreshAllWindows();

        // Reposition all remaining bag windows
        positionAllBagWindows();

        System.out.println("Unequipped bag from slot " + sourceBagSlotIndex);
        return true;
    }

    /**
     * Handles equipping a bag item to a bag equipment slot.
     */
    private boolean handleBagEquip(ItemSlotUI sourceSlot, ItemSlotUI targetSlot) {
        ItemStack sourceStack = sourceSlot.getItemStack();
        if (sourceStack == null) {
            return false;
        }

        // Check if the item is a bag
        if (!sourceStack.getDefinition().isBag()) {
            System.out.println("Cannot equip: Item is not a bag");
            return false;
        }

        int bagSlotIndex = targetSlot.getSlotIndex();

        // Check if slot is already occupied
        if (playerInventory.getBag(bagSlotIndex) != null) {
            System.out.println("Cannot equip: Bag slot already occupied");
            return false;
        }

        // Get bag size from item definition
        Integer bagSize = sourceStack.getDefinition().getBagSize();
        if (bagSize == null || bagSize <= 0) {
            System.out.println("Cannot equip: Bag has invalid size");
            return false;
        }

        // Create a BagInstance from the item definition
        com.game.systems.inventory.BagDefinition bagDef = new com.game.systems.inventory.BagDefinition(
            sourceStack.getDefinition().getId(),
            sourceStack.getDefinition().getName(),
            sourceStack.getDefinition().getDescription(),
            bagSize,
            com.game.systems.inventory.ItemFilter.allowAll(),
            sourceStack.getDefinition().getIconPath()
        );
        BagInstance newBag = new BagInstance(bagDef);



        // Equip the bag
        if (playerInventory.equipBag(newBag, bagSlotIndex)) {
            // Remove item from source slot
            sourceSlot.setItemStack(null);
            updateBackingData(sourceSlot, null);

            // Open the bag window if inventory is open (BEFORE refreshing)
            if (inventoryOpen) {
                // Close all bag windows first
                closeAllBagWindows();
                // Reopen all bags (including the new one) with proper positioning
                openBagWindows();
            }

            // Refresh UI to show the equipped bag (AFTER opening windows)
            refreshAllWindows();

            System.out.println("Equipped bag to slot " + bagSlotIndex);
            return true;
        }

        return false;
    }

    private void updateBackingData(ItemSlotUI slot, ItemStack newStack) {
        int slotIndex = slot.getSlotIndex();

        switch (slot.getSlotType()) {
            case DEFAULT_INVENTORY:
            case BAG_INVENTORY:
                // Both use InventoryContainer now (unified approach)
                InventoryContainer container = (InventoryContainer) slot.getContainerRef();
                container.setItem(slotIndex, newStack);
                break;

            case BAG_EQUIPMENT:
                // Bag equipment is handled specially in handleBagEquip()
                // This case should not be reached for bag equipping
                System.out.println("Warning: updateBackingData called for BAG_EQUIPMENT slot");
                break;
        }
    }

    private void handleItemDropToWorld(ItemSlotUI sourceSlot) {
        // Special handling for bag equipment slots
        if (sourceSlot.getSlotType() == ItemSlotUI.SlotType.BAG_EQUIPMENT) {
            int bagSlotIndex = sourceSlot.getSlotIndex();
            BagInstance bag = playerInventory.getBag(bagSlotIndex);

            if (bag == null) {
                System.out.println("Cannot drop: No bag in slot");
                return;
            }

            // Check if bag is empty
            if (!bag.isEmpty()) {
                System.out.println("Cannot drop: Bag must be empty first");
                return;
            }

            // Create a bag item to drop
            com.game.systems.item.ItemDefinition bagItemDef = com.game.systems.item.ItemRegistry.get("bag");
            if (bagItemDef == null) {
                System.out.println("Cannot drop: Bag item definition not found");
                return;
            }

            ItemStack bagItem = new ItemStack(bagItemDef, 1);

            // Unequip the bag
            playerInventory.unequipBag(bagSlotIndex);

            // Close the bag window
            ContainerWindow bagWindow = bagWindows.get(bag);
            if (bagWindow != null) {
                bagWindow.remove();
                bagWindows.remove(bag);
                bagSlotIndices.remove(bag);
            }

            // Clear the equipment slot display
            sourceSlot.setItemStack(null);
            sourceSlot.setItemIcon(null);

            refreshAllWindows();
            positionAllBagWindows();

            // Notify callback to drop the bag item
            if (itemDropCallback != null) {
                itemDropCallback.onDropItemToWorld(bagItem);
            }

            System.out.println("Dropped equipped bag to world from slot " + bagSlotIndex);
            return;
        }

        // Normal item drop handling
        ItemStack stack = sourceSlot.getItemStack();
        if (stack == null) return;

        // Remove from slot
        sourceSlot.setItemStack(null);
        updateBackingData(sourceSlot, null);
        refreshAllWindows();

        // Notify callback
        if (itemDropCallback != null) {
            itemDropCallback.onDropItemToWorld(stack);
        }

        System.out.println("Dropped to world: " + stack.toString());
    }

    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        inventoryWindow.setVisible(inventoryOpen);

        System.out.println("UIManagerNew: Inventory toggled. Open = " + inventoryOpen);
        System.out.println("UIManagerNew: Window visible = " + inventoryWindow.isVisible());
        System.out.println("UIManagerNew: Window position = (" + inventoryWindow.getX() + ", " + inventoryWindow.getY() + ")");

        if (inventoryOpen) {
            // Reset inventory window to default position
            positionInventoryWindow();

            // Open bag windows for equipped bags FIRST (before refreshing)
            openBagWindows();

            // Now refresh all windows (including the newly opened bag windows)
            refreshAllWindows();

            // Bring window to front
            inventoryWindow.toFront();
            System.out.println("UIManagerNew: Window brought to front");
        } else {
            // Close all bag windows
            closeAllBagWindows();
        }

        // Always set input processor when UI exists (for HUD)
        // Stage handles both HUD and inventory window
        Gdx.input.setInputProcessor(stage);
        System.out.println("UIManagerNew: Input processor set to stage");
        System.out.println("UIManagerNew: Current input processor = " + Gdx.input.getInputProcessor());
    }


    /**
     * Call this whenever inventory contents change to update the UI.
     */
    public void notifyInventoryChanged() {
        if (inventoryOpen) {
            refreshAllWindows();
        }
    }

    public void update(float delta) {
        stage.act(delta);
    }

    public void render() {
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Resize bottom HUD to match screen width
        float hudHeight = 60;
        bottomHUD.setSize(width, hudHeight);
        bottomHUD.setPosition(0, 0);

        positionInventoryWindow();
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public Stage getStage() {
        return stage;
    }

    public void setItemDropCallback(ItemDropCallback callback) {
        this.itemDropCallback = callback;
    }

    /**
     * Opens bag windows for all equipped bags.
     */
    private void openBagWindows() {
        // Open windows for all equipped bags, preserving slot order
        for (int slotIndex = 0; slotIndex < playerInventory.getMaxBagSlots(); slotIndex++) {
            BagInstance bag = playerInventory.getBag(slotIndex);
            if (bag != null && !bagWindows.containsKey(bag)) {
                openBagWindow(bag, slotIndex);
            }
        }

        // Position all bag windows intelligently
        positionAllBagWindows();
    }

    /**
     * Opens a window for a specific bag.
     * @param bag The bag instance
     * @param slotIndex The bag slot index (0, 1, 2, etc.)
     */
    private void openBagWindow(BagInstance bag, int slotIndex) {
        ContainerWindow bagWindow = new ContainerWindow(
            bag.getDefinition().getName(),
            bag.getContainer(),
            ItemSlotUI.SlotType.BAG_INVENTORY,
            dragAndDrop,
            skin
        );
        bagWindows.put(bag, bagWindow);
        bagSlotIndices.put(bag, slotIndex); // Track which slot this bag is in
        stage.addActor(bagWindow);
        bagWindow.setVisible(true);

        System.out.println("UIManagerNew: Opened bag window for " + bag.getDefinition().getName() + " in slot " + slotIndex);
    }

    /**
     * Intelligently positions all bag windows in columns, maintaining slot order.
     * Bags are ordered by slot index (0 at bottom, higher slots above).
     * Fills vertical space first, then creates new columns to the left.
     */
    private void positionAllBagWindows() {
        if (bagWindows.isEmpty()) return;

        float padding = 10;
        float screenHeight = Gdx.graphics.getHeight();

        // Starting position: bottom right, above inventory
        float startY = inventoryWindow.getY() + inventoryWindow.getHeight() + padding;
        float currentX = Gdx.graphics.getWidth() - padding;
        float currentY = startY;

        int columnIndex = 0;
        float maxWindowWidthInColumn = 0;

        // Sort bags by slot index to maintain consistent order
        java.util.List<java.util.Map.Entry<BagInstance, ContainerWindow>> sortedBags =
            new java.util.ArrayList<>(bagWindows.entrySet());
        sortedBags.sort((a, b) -> {
            int slotA = bagSlotIndices.getOrDefault(a.getKey(), 999);
            int slotB = bagSlotIndices.getOrDefault(b.getKey(), 999);
            return Integer.compare(slotA, slotB);
        });

        for (java.util.Map.Entry<BagInstance, ContainerWindow> entry : sortedBags) {
            ContainerWindow bagWindow = entry.getValue();
            float windowHeight = bagWindow.getHeight();
            float windowWidth = bagWindow.getWidth();

            // Check if this window would go off the top of the screen
            if (currentY + windowHeight > screenHeight - padding) {
                // Start a new column to the left
                columnIndex++;
                currentX -= (maxWindowWidthInColumn + padding);
                currentY = startY;
                maxWindowWidthInColumn = 0;
            }

            // Position the window
            float windowX = currentX - windowWidth;
            bagWindow.setPosition(windowX, currentY);

            // Track the widest window in this column for next column positioning
            maxWindowWidthInColumn = Math.max(maxWindowWidthInColumn, windowWidth);

            // Move up for next window in column
            currentY += windowHeight + padding;
        }
    }

    /**
     * Positions a bag window above the inventory.
     * @param bagWindow The bag window to position
     * @param bagIndex The index of the bag (0, 1, 2, etc.)
     * @deprecated Use positionAllBagWindows() instead for intelligent multi-column layout
     */
    @Deprecated
    private void positionBagWindow(ContainerWindow bagWindow, int bagIndex) {
        float padding = 10;
        // Position at bottom right, aligned with inventory
        float windowX = Gdx.graphics.getWidth() - bagWindow.getWidth() - padding;
        // Stack above inventory window, with each bag offset upward
        float windowY = inventoryWindow.getY() + inventoryWindow.getHeight() + padding + (bagIndex * (bagWindow.getHeight() + padding));
        bagWindow.setPosition(windowX, windowY);
    }

    /**
     * Closes all bag windows.
     */
    private void closeAllBagWindows() {
        for (ContainerWindow window : bagWindows.values()) {
            window.remove();
        }
        bagWindows.clear();
        bagSlotIndices.clear();
        System.out.println("UIManagerNew: Closed all bag windows");
    }

    public void refreshAllWindows() {
        refreshContainerWindowWithIcons(inventoryWindow);
        refreshBottomHUDWithIcons();

        // Refresh all bag windows
        for (ContainerWindow window : bagWindows.values()) {
            refreshContainerWindowWithIcons(window);
        }
    }

    /**
     * Refreshes a container window and sets item icons from WorldItemManager.
     * Works for both inventory and bag windows.
     */
    private void refreshContainerWindowWithIcons(ContainerWindow window) {
        window.refresh();

        // Set item icons for all slots
        for (ItemSlotUI slot : window.getSlots()) {
            ItemStack stack = slot.getItemStack();
            if (stack != null && stack.getDefinition().getIconPath() != null) {
                TextureRegion icon = worldItemManager.getTexture(stack.getDefinition().getIconPath());
                slot.setItemIcon(icon);
            } else {
                slot.setItemIcon(null);
            }
        }
    }

    /**
     * Refreshes bottom HUD and sets bag icons from WorldItemManager.
     */
    private void refreshBottomHUDWithIcons() {
        bottomHUD.refreshWithTextures(iconPath -> worldItemManager.getTexture(iconPath));
    }
}
