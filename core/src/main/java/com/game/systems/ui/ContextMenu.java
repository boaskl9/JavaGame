package com.game.systems.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.game.systems.item.ItemActionHelper;

/**
 * A right-click context menu for items.
 * Shows available actions (drop, equip, consume, etc.)
 */
public class ContextMenu extends Window {
    private List<String> optionsList;
    private ContextMenuListener listener;
    private ItemSlotUI targetSlot;

    public interface ContextMenuListener {
        void onMenuAction(String action, ItemSlotUI slot);
    }

    public ContextMenu(Skin skin) {
        super("", skin);
        System.out.println("ContextMenu: Constructor called");

        // Create options list
        optionsList = new List<>(skin);
        optionsList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String selectedOption = optionsList.getSelected();
                if (selectedOption != null && listener != null && targetSlot != null) {
                    listener.onMenuAction(selectedOption, targetSlot);
                    hide();
                }
            }
        });

        // Add to scroll pane for long menus
        ScrollPane scrollPane = new ScrollPane(optionsList, skin);
        scrollPane.setFadeScrollBars(false);

        add(scrollPane).width(120).height(100);
        pack();

        setVisible(false);
        setModal(true);
        setMovable(false);

        // Add listener to hide when clicking outside the menu
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Check if click is outside the menu bounds
                if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
                    hide();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Shows the context menu at the specified position for the given slot.
     * @param slot The item slot
     * @param x Screen x position
     * @param y Screen y position
     */
    public void show(ItemSlotUI slot, float x, float y) {
        System.out.println("ContextMenu.show() called at position (" + x + ", " + y + ")");
        this.targetSlot = slot;

        if (slot.getItemStack() != null) {
            com.game.systems.item.ItemDefinition itemDef = slot.getItemStack().getDefinition();
            com.game.systems.item.ItemType itemType = itemDef.getType();
            ItemSlotUI.SlotType slotType = slot.getSlotType();
            int quantity = slot.getItemStack().getQuantity();
            System.out.println("ContextMenu: Item: " + itemDef.getName() + ", Type: " + itemType + ", SlotType: " + slotType + ", Quantity: " + quantity);

            // Get all available actions from ItemActionHelper
            String[] actions = ItemActionHelper.getAvailableActions(itemType, itemDef, slotType);

            // Filter out "Split" if quantity is 1
            if (quantity <= 1) {
                java.util.ArrayList<String> filteredActions = new java.util.ArrayList<>();
                for (String action : actions) {
                    if (!action.equals("Split")) {
                        filteredActions.add(action);
                    }
                }
                actions = filteredActions.toArray(new String[0]);
            }

            // Set options
            optionsList.setItems(actions);
            System.out.println("ContextMenu: Set " + actions.length + " menu options: " + java.util.Arrays.toString(actions));
        } else {
            System.out.println("ContextMenu: Item stack is null!");
            optionsList.setItems(new String[0]);
        }

        // Position menu at mouse
        setPosition(x, y);
        System.out.println("ContextMenu: Position set to (" + getX() + ", " + getY() + ")");
        System.out.println("ContextMenu: Size is (" + getWidth() + " x " + getHeight() + ")");

        // Show
        setVisible(true);
        toFront();
        System.out.println("ContextMenu: Visible = " + isVisible());
        System.out.println("ContextMenu: ZIndex = " + getZIndex());
        System.out.println("ContextMenu: Stage = " + getStage());
    }

    /**
     * Gets the primary action for a given slot.
     * This delegates to ItemActionHelper for the actual logic.
     *
     * @param slot The item slot
     * @return The primary action string, or null if none
     */
    public String getPrimaryActionForSlot(ItemSlotUI slot) {
        if (slot == null || slot.getItemStack() == null) {
            return null;
        }
        return ItemActionHelper.getPrimaryAction(
            slot.getItemStack().getDefinition().getType(),
            slot.getItemStack().getDefinition(),
            slot.getSlotType()
        );
    }

    /**
     * Hides the context menu.
     */
    public void hide() {
        System.out.println("ContextMenu.hide() called");
        setVisible(false);
        targetSlot = null;
    }

    /**
     * Sets the listener for menu actions.
     * @param listener The listener
     */
    public void setListener(ContextMenuListener listener) {
        this.listener = listener;
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        // If clicking outside the menu, hide it
        Actor hit = super.hit(x, y, touchable);
        if (hit == null && isVisible()) {
            // Clicking outside - schedule hide for next frame
            // (can't hide immediately as it interferes with click detection)
            return null;
        }
        return hit;
    }
}
