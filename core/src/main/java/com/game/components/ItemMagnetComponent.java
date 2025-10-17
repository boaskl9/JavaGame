package com.game.components;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.Component;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.inventory.InventoryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Component that attracts nearby items toward the owner.
 * Items accelerate toward the owner when within magnetism radius.
 */
public class ItemMagnetComponent implements Component {
    private GameObject owner;
    private List<GameObject> nearbyItems;
    private float magnetRadius;
    private float magnetAcceleration;
    private float maxSpeed;

    public ItemMagnetComponent() {
        this(InventoryConfig.ITEM_MAGNET_RADIUS,
             InventoryConfig.ITEM_MAGNET_ACCELERATION,
             InventoryConfig.ITEM_MAGNET_MAX_SPEED);
    }

    public ItemMagnetComponent(float magnetRadius, float magnetAcceleration, float maxSpeed) {
        this.magnetRadius = magnetRadius;
        this.magnetAcceleration = magnetAcceleration;
        this.maxSpeed = maxSpeed;
        this.nearbyItems = new ArrayList<>();
    }

    @Override
    public void onAttach() {
        // Component will need reference to owner GameObject for position
        // This will be set externally or we can get it from context
    }

    /**
     * Sets the owner of this component.
     * Should be called after attaching to a GameObject.
     * @param owner The GameObject that owns this component
     */
    public void setOwner(GameObject owner) {
        this.owner = owner;
    }

    @Override
    public void update(float delta) {
        if (owner == null) return;

        Transform ownerTransform = owner.getComponent(Transform.class);
        if (ownerTransform == null) return;

        Vector2 ownerPos = ownerTransform.getPosition();

        // Update all nearby items
        for (GameObject item : nearbyItems) {
            if (!item.isActive()) continue;

            Transform itemTransform = item.getComponent(Transform.class);
            VelocityComponent itemVelocity = item.getComponent(VelocityComponent.class);

            if (itemTransform == null || itemVelocity == null) continue;

            Vector2 itemPos = itemTransform.getPosition();

            // Calculate distance
            float distance = ownerPos.dst(itemPos);

            // If within magnet radius, apply attraction
            if (distance <= magnetRadius && distance > 0.1f) {
                // Direction from item to owner
                Vector2 direction = new Vector2(ownerPos).sub(itemPos).nor();

                // Apply acceleration toward owner
                Vector2 acceleration = direction.scl(magnetAcceleration * delta);
                itemVelocity.addVelocity(acceleration.x, acceleration.y);

                // Clamp velocity to max speed
                Vector2 velocity = itemVelocity.getVelocity();
                if (velocity.len() > maxSpeed) {
                    velocity.nor().scl(maxSpeed);
                    itemVelocity.setVelocity(velocity.x, velocity.y);
                }
            }
        }
    }

    /**
     * Registers an item to be affected by this magnet.
     * Should be called by WorldItemManager or similar system.
     * @param item The item GameObject
     */
    public void registerItem(GameObject item) {
        if (item != null && !nearbyItems.contains(item)) {
            nearbyItems.add(item);
        }
    }

    /**
     * Unregisters an item from this magnet.
     * @param item The item GameObject
     */
    public void unregisterItem(GameObject item) {
        nearbyItems.remove(item);
    }

    /**
     * Clears all registered items.
     */
    public void clearItems() {
        nearbyItems.clear();
    }

    /**
     * Gets all items currently in range of the magnet.
     * @return List of items within magnet radius
     */
    public List<GameObject> getItemsInRange() {
        if (owner == null) return new ArrayList<>();

        Transform ownerTransform = owner.getComponent(Transform.class);
        if (ownerTransform == null) return new ArrayList<>();

        Vector2 ownerPos = ownerTransform.getPosition();
        List<GameObject> inRange = new ArrayList<>();

        for (GameObject item : nearbyItems) {
            Transform itemTransform = item.getComponent(Transform.class);
            if (itemTransform != null) {
                float distance = ownerPos.dst(itemTransform.getPosition());
                if (distance <= magnetRadius) {
                    inRange.add(item);
                }
            }
        }

        return inRange;
    }

    public float getMagnetRadius() {
        return magnetRadius;
    }

    public void setMagnetRadius(float magnetRadius) {
        this.magnetRadius = magnetRadius;
    }

    public float getMagnetAcceleration() {
        return magnetAcceleration;
    }

    public void setMagnetAcceleration(float magnetAcceleration) {
        this.magnetAcceleration = magnetAcceleration;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public List<GameObject> getNearbyItems() {
        return new ArrayList<>(nearbyItems);
    }
}
