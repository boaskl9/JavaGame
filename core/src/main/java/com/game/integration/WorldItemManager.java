package com.game.integration;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.inventory.InventoryConfig;
import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all item pickups in the world.
 * Handles spawning, despawning, persistence, and item limits.
 */
public class WorldItemManager {
    private final List<ItemPickupEntity> items;
    private final Map<String, TextureRegion> itemTextures;
    private int maxWorldItems;

    // Track items by level for save/load
    private final Map<String, List<ItemPickupEntity>> itemsByLevel;
    private String currentLevelId;

    public WorldItemManager() {
        this.items = new ArrayList<>();
        this.itemTextures = new HashMap<>();
        this.itemsByLevel = new HashMap<>();
        this.maxWorldItems = InventoryConfig.MAX_WORLD_ITEMS;
    }

    /**
     * Spawns an item in the world.
     * @param itemStack The item stack to spawn
     * @param x The x position
     * @param y The y position
     * @return The created ItemPickupEntity, or null if limit reached
     */
    public ItemPickupEntity spawnItem(ItemStack itemStack, float x, float y, float graceTimer) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }

        // Check item limit
        if (items.size() >= maxWorldItems) {
            System.err.println("World item limit reached! Cannot spawn: " + itemStack.toString());
            return null;
        }

        ItemPickupEntity pickup = new ItemPickupEntity(itemStack, x, y, graceTimer);

        // Set texture if available
        String iconPath = itemStack.getDefinition().getIconPath();
        if (iconPath != null && itemTextures.containsKey(iconPath)) {
            pickup.setTexture(itemTextures.get(iconPath));
        }

        items.add(pickup);
        return pickup;
    }

    /**
     * Spawns multiple items in a pile around a position.
     * @param itemStack The item stack to spawn
     * @param centerX Center x position
     * @param centerY Center y position
     */
    public void spawnItemPile(ItemStack itemStack, float centerX, float centerY) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        int quantity = itemStack.getQuantity();
        int maxStack = itemStack.getDefinition().getMaxStackSize();

        // Split into multiple stacks if needed
        while (quantity > 0) {
            int stackSize = Math.min(quantity, maxStack);
            ItemStack stack = new ItemStack(itemStack.getDefinition(), stackSize);

            // Random offset for pile effect
            float offsetX = (float) (Math.random() * InventoryConfig.ITEM_DROP_SPREAD * 2 - InventoryConfig.ITEM_DROP_SPREAD);
            float offsetY = (float) (Math.random() * InventoryConfig.ITEM_DROP_SPREAD * 2 - InventoryConfig.ITEM_DROP_SPREAD);

            spawnItem(stack, centerX + offsetX, centerY + offsetY, 0f);
            quantity -= stackSize;
        }
    }

    /**
     * Removes an item from the world.
     * @param item The item to remove
     */
    public void removeItem(ItemPickupEntity item) {
        items.remove(item);
    }

    /**
     * Updates all items in the world.
     * @param delta Time since last update
     */
    public void update(float delta) {
        // Update all items
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemPickupEntity item = items.get(i);
            if (!item.isActive()) {
                items.remove(i);
            } else {
                item.update(delta);
            }
        }
    }

    /**
     * Renders all items in the world.
     * @param batch The sprite batch
     */
    public void render(SpriteBatch batch) {
        for (ItemPickupEntity item : items) {
            item.render(batch);
        }
    }

    /**
     * Gets all items near a position.
     * @param position The center position
     * @param radius The search radius
     * @return List of nearby items
     */
    public List<ItemPickupEntity> getItemsNear(Vector2 position, float radius) {
        List<ItemPickupEntity> nearby = new ArrayList<>();
        float radiusSquared = radius * radius;

        for (ItemPickupEntity item : items) {
            if (item.hasComponent(com.game.systems.entity.Transform.class)) {
                Vector2 itemPos = item.getComponent(com.game.systems.entity.Transform.class).getPosition();
                float distSquared = position.dst2(itemPos);

                if (distSquared <= radiusSquared) {
                    nearby.add(item);
                }
            }
        }

        return nearby;
    }

    /**
     * Gets an item at a specific position (for collision detection).
     * @param position The position
     * @param tolerance Distance tolerance
     * @return The item, or null if none found
     */
    public ItemPickupEntity getItemAt(Vector2 position, float tolerance) {
        List<ItemPickupEntity> nearby = getItemsNear(position, tolerance);
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    /**
     * Registers a texture for an item.
     * @param iconPath The icon path (from ItemDefinition)
     * @param texture The texture region
     */
    public void registerTexture(String iconPath, TextureRegion texture) {
        itemTextures.put(iconPath, texture);
    }

    /**
     * Gets a texture for an item.
     * @param iconPath The icon path (from ItemDefinition)
     * @return The texture region, or null if not found
     */
    public TextureRegion getTexture(String iconPath) {
        return itemTextures.get(iconPath);
    }

    /**
     * Clears all items from the world.
     */
    public void clearAll() {
        items.clear();
    }

    public List<ItemPickupEntity> getAllItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public int getMaxWorldItems() {
        return maxWorldItems;
    }

    public void setMaxWorldItems(int maxWorldItems) {
        this.maxWorldItems = maxWorldItems;
    }

    /**
     * Set the current level ID for tracking items by level.
     * Call this when changing levels.
     */
    public void setCurrentLevel(String levelId) {
        // Save current items to their level before switching
        if (currentLevelId != null && !items.isEmpty()) {
            itemsByLevel.put(currentLevelId, new ArrayList<>(items));
        }

        this.currentLevelId = levelId;

        // Load items for new level
        items.clear();
        List<ItemPickupEntity> levelItems = itemsByLevel.get(levelId);
        if (levelItems != null) {
            items.addAll(levelItems);
        }
    }

    /**
     * Clear items for a specific level.
     * Used when entering dungeons (which don't persist items).
     */
    public void clearLevel(String levelId) {
        itemsByLevel.remove(levelId);
        if (levelId.equals(currentLevelId)) {
            items.clear();
        }
    }

    // ========== Save/Load Support ==========

    /**
     * Export dropped items data for saving.
     * @return Map of level ID -> list of dropped item data
     */
    public Map<String, List<com.game.save.DroppedItemData>> exportSaveData() {
        // Update current level's items before export
        if (currentLevelId != null && !items.isEmpty()) {
            itemsByLevel.put(currentLevelId, new ArrayList<>(items));
        }

        Map<String, List<com.game.save.DroppedItemData>> result = new HashMap<>();

        for (Map.Entry<String, List<ItemPickupEntity>> entry : itemsByLevel.entrySet()) {
            String levelId = entry.getKey();
            List<ItemPickupEntity> levelItems = entry.getValue();
            List<com.game.save.DroppedItemData> droppedItemDataList = new ArrayList<>();

            for (ItemPickupEntity pickup : levelItems) {
                ItemStack stack = pickup.getItemStack();
                com.game.systems.entity.Transform transform = pickup.getComponent(com.game.systems.entity.Transform.class);
                if (stack != null && transform != null) {
                    droppedItemDataList.add(new com.game.save.DroppedItemData(
                        stack.getDefinition().getId(),
                        stack.getQuantity(),
                        transform.getX(),
                        transform.getY()
                    ));
                }
            }

            result.put(levelId, droppedItemDataList);
        }

        return result;
    }

    /**
     * Import dropped items data from save.
     * Clears existing items and recreates from save data.
     * @param data Map of level ID -> list of dropped item data
     */
    public void importSaveData(Map<String, List<com.game.save.DroppedItemData>> data) {
        // Clear existing items
        items.clear();
        itemsByLevel.clear();

        // Recreate items from save data
        for (Map.Entry<String, List<com.game.save.DroppedItemData>> entry : data.entrySet()) {
            String levelId = entry.getKey();
            List<com.game.save.DroppedItemData> droppedItemDataList = entry.getValue();
            List<ItemPickupEntity> levelItems = new ArrayList<>();

            for (com.game.save.DroppedItemData itemData : droppedItemDataList) {
                // Look up item definition
                com.game.systems.item.ItemDefinition def = com.game.systems.item.ItemRegistry.get(itemData.itemId);
                if (def == null) {
                    System.err.println("WorldItemManager: Item not found in registry: " + itemData.itemId);
                    continue;
                }

                // Create item stack and pickup entity
                ItemStack stack = new ItemStack(def, itemData.quantity);
                ItemPickupEntity pickup = new ItemPickupEntity(stack, itemData.x, itemData.y, 0f);

                // Set texture if available
                String iconPath = def.getIconPath();
                if (iconPath != null && itemTextures.containsKey(iconPath)) {
                    pickup.setTexture(itemTextures.get(iconPath));
                }

                levelItems.add(pickup);
            }

            itemsByLevel.put(levelId, levelItems);
        }

        // Load items for current level if it was set
        if (currentLevelId != null) {
            List<ItemPickupEntity> levelItems = itemsByLevel.get(currentLevelId);
            if (levelItems != null) {
                items.addAll(levelItems);
            }
        }

        System.out.println("WorldItemManager: Imported dropped items for " + itemsByLevel.size() + " levels");
    }
}
