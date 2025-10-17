package com.game.systems.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles drag-and-drop functionality for inventory items using the new image-based slots.
 */
public class ItemDragAndDropSystem extends DragAndDrop {
    private final Skin skin;
    private ItemDropListener dropListener;
    private List<ItemSlotUI> registeredSlots = new ArrayList<>();
    private static final float SNAP_DISTANCE = 60f; // Distance threshold for snapping to nearest slot

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
        // Keep track of all registered slots for snap-to-nearest feature
        registeredSlots.add(slot);

        // Add as drag source
        addSource(new Source(slot) {
            private TextureRegion dragIcon;

            @Override
            public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                System.out.println("ItemDragAndDropSystem: dragStart called with button=" + event.getButton() + ", pointer=" + pointer);

                // Only allow dragging with left mouse button (button 0)
                // Right click (button 1) should be ignored to allow context menu


                if (slot.isEmpty()) {
                    System.out.println("ItemDragAndDropSystem: Slot is empty, no drag");
                    return null; // Can't drag empty slot
                }

                System.out.println("ItemDragAndDropSystem: Starting drag for item: " + slot.getItemStack().getDefinition().getName());
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
                if (target == null) {
                    // No target detected - try to find nearest slot within snap distance
                    ItemSlotUI nearestSlot = findNearestSlot(event.getStageX(), event.getStageY(), slot);

                    if (nearestSlot != null && dropListener != null) {
                        // Snap to nearest slot
                        System.out.println("Quick drag detected - snapping to nearest slot");
                        dropListener.onItemDrop(slot, nearestSlot);
                    } else if (dropListener != null) {
                        // Dropped outside any valid target - drop to world
                        dropListener.onItemDropToWorld(slot);
                    }
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

    /**
     * Finds the nearest slot to the given position within SNAP_DISTANCE.
     * @param stageX X coordinate in stage space
     * @param stageY Y coordinate in stage space
     * @param sourceSlot The source slot (to exclude from search)
     * @return The nearest slot, or null if none within range
     */
    private ItemSlotUI findNearestSlot(float stageX, float stageY, ItemSlotUI sourceSlot) {
        ItemSlotUI nearest = null;
        float nearestDistance = SNAP_DISTANCE;

        for (ItemSlotUI slot : registeredSlots) {
            // Skip the source slot and invisible/removed slots
            if (slot == sourceSlot || !slot.isVisible() || slot.getStage() == null) {
                continue;
            }

            // Calculate center position of the slot in stage coordinates
            Vector2 slotCenter = slot.localToStageCoordinates(new Vector2(
                slot.getWidth() / 2f,
                slot.getHeight() / 2f
            ));

            // Calculate distance
            float dx = stageX - slotCenter.x;
            float dy = stageY - slotCenter.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Update nearest if this is closer
            if (distance < nearestDistance) {
                nearest = slot;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}
