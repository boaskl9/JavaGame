package com.game.systems.furniture;

import com.badlogic.gdx.graphics.Texture;
import com.game.systems.animation.AnimationBuilder;
import com.game.components.AnimationComponent;
import com.game.components.ColliderComponent;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Entity;
import com.game.systems.entity.Transform;
import com.game.systems.item.ItemDefinition;
import com.game.components.RenderComponent;

/**
 * Base class for all furniture entities that can be placed in the world.
 * Furniture are static world objects that can be interacted with.
 */
public abstract class FurnitureEntity extends GameObject {
    protected final String itemId;
    protected final ItemDefinition definition;
    protected Transform transform;
    protected ColliderComponent collider;
    protected RenderComponent render;
    protected AnimationComponent animation;

    /**
     * Create a new furniture entity.
     * @param itemId The item ID this furniture was created from
     * @param definition The item definition containing furniture properties
     * @param x World X position
     * @param y World Y position
     */
    public FurnitureEntity(String itemId, ItemDefinition definition, float x, float y) {
        super();
        this.itemId = itemId;
        this.definition = definition;

        // Add transform component
        transform = new Transform(x, y);
        addComponent(transform);

        // Add collision component
        // Use furniture dimensions from definition, defaulting to 16x16
        float width = 16f;  // Default
        float height = 16f; // Default

        collider = new ColliderComponent(width, height, 0, 0);
        addComponent(collider);

        // Add rendering components
        render = new RenderComponent(16, 16);
        addComponent(render);

        animation = new AnimationComponent();
        addComponent(animation);

        // Load sprite from item definition
        loadSprite();
    }

    /**
     * Load the furniture sprite from the item definition's icon path.
     */
    protected void loadSprite() {
        String iconPath = definition.getIconPath();
        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                Texture texture = new Texture(iconPath);
                AnimationBuilder.loadStatic(animation.getAnimator(), "idle", texture, 1, 1);
                // Set initial animation state to idle
                animation.setState("idle", 0, false);
                System.out.println("FurnitureEntity: Loaded sprite for " + itemId + " from " + iconPath);
            } catch (Exception e) {
                System.err.println("Failed to load furniture sprite: " + iconPath);
                e.printStackTrace();
            }
        }
    }

    /**
     * Called when an entity interacts with this furniture (press E).
     * Override in subclasses to implement specific interaction behavior.
     * @param entity The entity interacting with this furniture
     */
    public void onInteract(Entity entity) {
        // Override in subclasses
    }

    /**
     * Check if this furniture can be picked up.
     * Override in subclasses to add conditions (e.g., chest must be empty).
     * @return true if the furniture can be picked up
     */
    public boolean canPickup() {
        return true;
    }

    /**
     * Get the item ID this furniture represents.
     * Used when picking up furniture to return the correct item.
     * @return The item ID
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Get the item definition for this furniture.
     * @return The item definition
     */
    public ItemDefinition getDefinition() {
        return definition;
    }

    /**
     * Get the transform component.
     * @return The transform component
     */
    public Transform getTransform() {
        return transform;
    }

    /**
     * Get the collider component.
     * @return The collider component
     */
    public ColliderComponent getCollider() {
        return collider;
    }
}
