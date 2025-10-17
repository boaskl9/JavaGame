package com.game.systems.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Handles drag-and-drop functionality for inventory items using the new image-based slots.
 */
public class ItemDragAndDropSystem extends DragAndDrop {
    private final Skin skin;
    private ItemDropListener dropListener;

    public interface ItemDropListener {
        /**
         * Called when an item is dropped on a valid target.
         * @param sourceSlot The source slot
         * @param targetSlot The target slot
         * @return true if the drop was successful
         */
        boolean onItemDrop(ItemSlotUI sourceSlot, ItemSlotUI targetSlot);

        /**
         * Called when an item is dropped outside any valid target (to drop on ground).
         * @param sourceSlot The source slot
         */
        void onItemDropToWorld(ItemSlotUI sourceSlot);
    }

    public ItemDragAndDropSystem(Skin skin) {
        this.skin = skin;
        setKeepWithinStage(false); // Allow dragging outside windows
    }

    /**
     * Registers a slot as a drag source and drop target.
     * @param slot The item slot UI
     */
    public void registerSlot(ItemSlotUI slot) {
        // Add as drag source
        addSource(new Source(slot) {
            private TextureRegion dragIcon;

            @Override
            public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (slot.isEmpty()) {
                    return null; // Can't drag empty slot
                }

                Payload payload = new Payload();
                payload.setObject(slot);

                // Create drag visual using item icon
                dragIcon = slot.getItemStack() != null ?
                    slot.getItemIcon() : null;

                if (dragIcon != null) {
                    Image dragImage = new Image(dragIcon);
                    dragImage.setSize(32, 32);
                    payload.setDragActor(dragImage);
                } else {
                    // Fallback: create a simple colored square
                    Image dragImage = new Image(skin, "white");
                    dragImage.setSize(32, 32);
                    dragImage.setColor(0.5f, 0.5f, 0.5f, 0.8f);
                    payload.setDragActor(dragImage);
                }

                return payload;
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer, Payload payload, Target target) {
                if (target == null && dropListener != null) {
                    // Dropped outside any valid target - drop to world
                    dropListener.onItemDropToWorld(slot);
                }
            }
        });

        // Add as drop target
        addTarget(new Target(slot) {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
                // Highlight this slot as valid drop target
                slot.setHighlighted(true);
                return true;
            }

            @Override
            public void reset(Source source, Payload payload) {
                // Remove highlight
                slot.setHighlighted(false);
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer) {
                ItemSlotUI sourceSlot = (ItemSlotUI) payload.getObject();
                ItemSlotUI targetSlot = slot;

                // Notify listener
                if (dropListener != null) {
                    dropListener.onItemDrop(sourceSlot, targetSlot);
                }

                // Remove highlight
                slot.setHighlighted(false);
            }
        });
    }

    /**
     * Sets the listener for drop events.
     * @param listener The drop listener
     */
    public void setDropListener(ItemDropListener listener) {
        this.dropListener = listener;
    }
}
