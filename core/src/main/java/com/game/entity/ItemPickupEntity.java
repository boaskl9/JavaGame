package com.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.components.VelocityComponent;
import com.game.systems.entity.Entity;
import com.game.systems.entity.Transform;
import com.game.systems.inventory.InventoryConfig;
import com.game.systems.item.ItemStack;
import com.game.world.WorldObject;

/**
 * Represents an item pickup in the game world.
 * Can be picked up by the player when colliding.
 * Affected by ItemMagnetComponent.
 */
public class ItemPickupEntity extends WorldObject {
    private ItemStack itemStack;
    private TextureRegion texture;
    private float bounceTime;
    private boolean canPickup;
    private float graceTimer;

    public ItemPickupEntity(ItemStack itemStack, float x, float y) {
        super("item_pickup");
        this.itemStack = itemStack;
        this.bounceTime = 0f;
        this.canPickup = true;
        this.graceTimer = 0;

        // Add components
        addComponent(new Transform(x, y));
        addComponent(new VelocityComponent());

        // TODO: Load texture based on item definition's iconPath
        // For now, texture is null - will need to be set externally
    }

    public ItemPickupEntity(ItemStack itemStack, float x, float y, float graceTimer) {
        super("item_pickup");
        this.itemStack = itemStack;
        this.bounceTime = 0f;
        this.graceTimer = graceTimer;

        this.canPickup = !(graceTimer > 0);

        // Add components
        addComponent(new Transform(x, y));
        addComponent(new VelocityComponent());

        // TODO: Load texture based on item definition's iconPath
        // For now, texture is null - will need to be set externally
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        // Update bounce animation
        bounceTime += delta * InventoryConfig.ITEM_BOUNCE_SPEED;

        // Apply velocity to position (for magnetism)
        VelocityComponent velocity = getComponent(VelocityComponent.class);
        Transform transform = getComponent(Transform.class);

        if (graceTimer > 0) {
            graceTimer -= delta;
            if (graceTimer <= 0) {
                setCanPickup(true); // Only set once when timer expires
                velocity.setVelocity(0, 0);
            }
        }
        else if (velocity != null && transform != null) {
            transform.translate(
                velocity.getVelocity().x * delta,
                velocity.getVelocity().y * delta
            );

            // Apply friction to slow down over time
            velocity.setVelocity(
                velocity.getVelocity().x * 0.95f,
                velocity.getVelocity().y * 0.95f
            );
        }
    }

    /**
     * Renders the item pickup.
     * @param batch The sprite batch to render with
     */
    public void render(SpriteBatch batch) {
        if (texture == null || !isActive()) return;

        Transform transform = getComponent(Transform.class);
        if (transform == null) return;

        float x = transform.getX();
        float y = transform.getY();

        // Calculate bounce offset
        float bounceOffset = (float) Math.sin(bounceTime) * InventoryConfig.ITEM_BOUNCE_HEIGHT;

        // Render texture with bounce and scale
        float scale = InventoryConfig.ITEM_PICKUP_SCALE;
        float width = texture.getRegionWidth() * scale;
        float height = texture.getRegionHeight() * scale;

        batch.draw(
            texture,
            x - width / 2,
            y - height / 2 + bounceOffset,
            width,
            height
        );
    }

    /**
     * Called when the item is picked up by a player.
     * @return true if pickup was successful
     */
    public boolean onPickup() {
        if (!canPickup) return false;

        canPickup = false;
        setActive(false);
        return true;
    }

    /**
     * Gets the item stack this pickup represents.
     * @return The item stack
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Sets the texture for this item pickup.
     * @param texture The texture region to render
     */
    public void setTexture(TextureRegion texture) {
        this.texture = texture;
    }

    /**
     * Gets the texture for this item pickup.
     * @return The texture region
     */
    public TextureRegion getTexture() {
        return texture;
    }

    public boolean canPickup() {
        return canPickup;
    }

    public void setCanPickup(boolean canPickup) {
        this.canPickup = canPickup;
    }

    @Override
    public String toString() {
        return "ItemPickup{" + itemStack.toString() + "}";
    }
}
