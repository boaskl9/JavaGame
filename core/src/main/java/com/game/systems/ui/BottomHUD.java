package com.game.systems.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.game.components.HealthComponent;
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

    private ProgressBar healthBar;
    private HealthComponent playerHealth;

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

        // Create health bar with custom height
        ProgressBar.ProgressBarStyle customStyle = createCustomHeightProgressBarStyle(30);
        healthBar = new ProgressBar(0, 100, 1, false, customStyle);
        healthBar.setValue(100); // Start at full health visually
        healthBar.setAnimateDuration(.25f);
        add(healthBar).width(250).left().padRight(10);

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

    /**
     * Sets the player's health component to display.
     */
    public void setPlayerHealth(HealthComponent health) {
        // Remove listener from old health component if exists
        if (this.playerHealth != null) {
            this.playerHealth.removeListener(this::onHealthChanged);
        }

        this.playerHealth = health;

        // Add listener to new health component
        if (this.playerHealth != null) {
            this.playerHealth.addListener(this::onHealthChanged);
            updateHealthBarRange(health.getMaxHealth());
        }

        updateHealthDisplay();
    }

    /**
     * Called when player health changes.
     */
    private void onHealthChanged(int currentHealth, int maxHealth) {
        if (healthBar.getMaxValue() != maxHealth) {
            updateHealthBarRange(maxHealth);
        }
        updateHealthDisplay();
    }

    private void updateHealthBarRange(int maxHealth) {
        healthBar.setRange(0, maxHealth);
    }

    /**
     * Updates the health display based on current player health.
     * Call this whenever health changes.
     */
    public void updateHealthDisplay() {
        if (playerHealth == null) {
            return;
        }

        healthBar.setValue(playerHealth.getCurrentHealth());
    }

    /**
     * Creates a custom ProgressBar style with overridden height.
     * Works around LibGDX's ProgressBar using drawable's intrinsic height.
     *
     * @param height Desired height in pixels
     * @return Custom ProgressBarStyle with specified height
     */
    private ProgressBar.ProgressBarStyle createCustomHeightProgressBarStyle(int height) {
        // Copy the default style
        ProgressBar.ProgressBarStyle customStyle = new ProgressBar.ProgressBarStyle(
            skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class)
        );

        // Wrap the background drawable to override its minimum height
        Drawable originalBackground = customStyle.background;
        customStyle.background = new com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable(originalBackground) {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float x, float y, float width, float height) {
                originalBackground.draw(batch, x, y, width, height);
            }

            @Override
            public float getMinHeight() {
                return height;
            }
        };

        // Wrap the knobBefore drawable to match
        Drawable originalKnob = customStyle.knobBefore;
        customStyle.knobBefore = new com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable(originalKnob) {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float x, float y, float width, float height) {
                originalKnob.draw(batch, x, y, width, height);
            }

            @Override
            public float getMinHeight() {
                return height;
            }
        };

        return customStyle;
    }
}
