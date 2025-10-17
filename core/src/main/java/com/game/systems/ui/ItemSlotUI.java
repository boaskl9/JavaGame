package com.game.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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

    public enum SlotType {
        DEFAULT_INVENTORY,
        BAG_INVENTORY,
        BAG_EQUIPMENT
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

        // Add right-click listener for context menu
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (getTapCount() == 1) {
                    int button = event.getButton();
                    if (button == 1) { // Right click
                        onRightClick();
                    }
                }
            }
        });
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

    private void onRightClick() {
        if (itemStack != null) {
            // TODO: Show context menu
            System.out.println("Right-clicked: " + itemStack.toString());
        }
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
