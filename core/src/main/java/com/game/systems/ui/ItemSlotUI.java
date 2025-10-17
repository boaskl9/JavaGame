package com.game.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.game.systems.item.ItemStack;

/**
 * A UI widget representing a single inventory slot using images instead of buttons.
 * Displays an item icon and quantity, supports drag-and-drop and tooltips.
 */
public class ItemSlotUI extends Image {
    private ItemStack itemStack;
    private TextureRegion itemIcon;
    private final int slotIndex;
    private final SlotType slotType;
    private final Object containerRef;
    private final Skin skin;

    private Drawable slotBackground;
    private Drawable slotHighlight;
    private BitmapFont font;
    private boolean highlighted = false;

    private SlotHoverListener hoverListener;
    private SlotRightClickListener rightClickListener;
    private SlotDoubleClickListener doubleClickListener;

    public enum SlotType {
        DEFAULT_INVENTORY,
        BAG_INVENTORY,
        BAG_EQUIPMENT
    }

    public interface SlotHoverListener {
        void onHoverEnter(ItemSlotUI slot, float x, float y);
        void onHoverExit(ItemSlotUI slot);
        void onMouseDown(ItemSlotUI slot); // Called when any mouse button is pressed
    }

    public interface SlotRightClickListener {
        void onRightClick(ItemSlotUI slot, float x, float y);
    }

    public interface SlotDoubleClickListener {
        void onDoubleClick(ItemSlotUI slot);
    }

    public ItemSlotUI(int slotIndex, SlotType slotType, Object containerRef, Skin skin) {
        super();
        this.slotIndex = slotIndex;
        this.slotType = slotType;
        this.containerRef = containerRef;
        this.skin = skin;

        // Get drawables from skin
        slotBackground = skin.getDrawable("textfield"); // Use textfield background as slot
        slotHighlight = skin.getDrawable("textfield-selected"); // Highlighted version
        font = skin.getFont("default");

        setDrawable(slotBackground);
        setSize(48, 48);

        // Add hover listener for tooltips
        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (pointer == -1 && hoverListener != null && itemStack != null) { // -1 means mouse hover, not drag
                    hoverListener.onHoverEnter(ItemSlotUI.this, event.getStageX(), event.getStageY());
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (pointer == -1 && hoverListener != null) {
                    hoverListener.onHoverExit(ItemSlotUI.this);
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                System.out.println("ItemSlotUI.InputListener.touchDown: button=" + button + ", pointer=" + pointer);
                // Notify on any mouse button press (left or right click)
                if (hoverListener != null) {
                    hoverListener.onMouseDown(ItemSlotUI.this);
                }
                System.out.println("ItemSlotUI.InputListener.touchDown: returning false (not consuming)");
                return false; // Don't consume the event, let other listeners handle it
            }
        });

        // Add right-click listener for context menu
        // ClickListener(int button) constructor - use button 1 for right-click
        addListener(new ClickListener(1) { // Listen specifically for right-click (button 1)
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("ItemSlotUI.ClickListener.clicked: Right-click event received!");
                System.out.println("ItemSlotUI.ClickListener.clicked: TapCount=" + getTapCount() + ", Button=" + getButton());

                if (rightClickListener != null) {
                    System.out.println("ItemSlotUI.ClickListener.clicked: Calling rightClickListener at (" + event.getStageX() + ", " + event.getStageY() + ")");
                    rightClickListener.onRightClick(ItemSlotUI.this, event.getStageX(), event.getStageY());
                } else {
                    System.out.println("ItemSlotUI.ClickListener.clicked: rightClickListener is null!");
                }
            }
        });

        // Add left-click listener for double-click primary action
        addListener(new ClickListener(0) { // Listen specifically for left-click (button 0)
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("ItemSlotUI.LeftClickListener.clicked: TapCount=" + getTapCount());
                if (getTapCount() == 2) { // Double-click
                    System.out.println("ItemSlotUI.LeftClickListener.clicked: Double-click detected!");
                    if (doubleClickListener != null && itemStack != null) {
                        System.out.println("ItemSlotUI.LeftClickListener.clicked: Calling doubleClickListener");
                        doubleClickListener.onDoubleClick(ItemSlotUI.this);
                    } else {
                        System.out.println("ItemSlotUI.LeftClickListener.clicked: doubleClickListener is null or no item");
                    }
                }
            }
        });
    }

    /**
     * Override hit detection to use a slightly larger area.
     * This makes drag-and-drop more forgiving when moving quickly.
     */
    @Override
    public com.badlogic.gdx.scenes.scene2d.Actor hit(float x, float y, boolean touchable) {
        // Expand hit area by 4 pixels on each side for more forgiving drag-drop
        float padding = 4f;
        if (x >= -padding && x < getWidth() + padding && y >= -padding && y < getHeight() + padding) {
            return this;
        }
        return null;
    }

    /**
     * Sets the item stack displayed in this slot.
     * @param itemStack The item stack, or null for empty slot
     */
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setItemIcon(TextureRegion icon) {
        this.itemIcon = icon;
    }

    public TextureRegion getItemIcon() { return this.itemIcon; }

    /**
     * Sets whether this slot is highlighted (for drag-drop feedback).
     */
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        setDrawable(highlighted ? slotHighlight : slotBackground);
    }

    /**
     * Sets the hover listener for tooltip display.
     * @param listener The hover listener
     */
    public void setHoverListener(SlotHoverListener listener) {
        this.hoverListener = listener;
    }

    /**
     * Sets the right-click listener for context menu.
     * @param listener The right-click listener
     */
    public void setRightClickListener(SlotRightClickListener listener) {
        this.rightClickListener = listener;
    }

    /**
     * Sets the double-click listener for primary action.
     * @param listener The double-click listener
     */
    public void setDoubleClickListener(SlotDoubleClickListener listener) {
        this.doubleClickListener = listener;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Draw slot background
        super.draw(batch, parentAlpha);

        // Draw item icon if present
        if (itemIcon != null && itemStack != null) {
            float iconSize = 32;
            float x = getX() + (getWidth() - iconSize) / 2;
            float y = getY() + (getHeight() - iconSize) / 2;

            Color oldColor = batch.getColor();
            batch.setColor(1, 1, 1, parentAlpha);
            batch.draw(itemIcon, x, y, iconSize, iconSize);
            batch.setColor(oldColor);
        }

        // Draw quantity text
        if (itemStack != null && itemStack.getQuantity() > 1) {
            String quantityText = String.valueOf(itemStack.getQuantity());
            font.setColor(1, 1, 1, parentAlpha);
            font.draw(batch, quantityText,
                getX() + getWidth() - font.getSpaceXadvance() * quantityText.length() - 4,
                getY() + font.getLineHeight());
        }
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public Object getContainerRef() {
        return containerRef;
    }

    public boolean isEmpty() {
        return itemStack == null;
    }

    /**
     * Creates a tooltip text for this slot.
     * @return The tooltip text, or null if empty
     */
    public String getTooltipText() {
        if (itemStack == null) {
            return null;
        }

        StringBuilder tooltip = new StringBuilder();
        tooltip.append(itemStack.getDefinition().getName());

        if (itemStack.getQuantity() > 1) {
            tooltip.append(" x").append(itemStack.getQuantity());
        }

        String description = itemStack.getDefinition().getDescription();
        if (description != null && !description.isEmpty()) {
            tooltip.append("\n").append(description);
        }

        tooltip.append("\n[").append(itemStack.getDefinition().getType()).append("]");

        return tooltip.toString();
    }
}
