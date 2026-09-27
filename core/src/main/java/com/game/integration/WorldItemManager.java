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
 * Manages all item pickups in the world, for every level at once.
 * Handles spawning, despawning, persistence, and item limits.
 *
 * Items are always stored per level. The "active level" is the level that calls without an
 * explicit level ID operate on (spawning loot, rendering, pickups). The host switches it while
 * simulating each loaded level, so loot always lands in the level it dropped in.
 */
public class WorldItemManager {
    private final Map<String, List<ItemPickupEntity>> itemsByLevel;
    private final Map<String, TextureRegion> itemTextures;
    private final List<Listener> listeners;
    private int maxWorldItems;
    private String activeLevelId;

    /**
     * Notified when items appear in or disappear from any level.
     */
    public interface Listener {
        void onItemSpawned(String levelId, ItemPickupEntity item);
        void onItemRemoved(String levelId, ItemPickupEntity item);
    }

    public WorldItemManager() {
        this.itemsByLevel = new HashMap<>();
        this.itemTextures = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.maxWorldItems = InventoryConfig.MAX_WORLD_ITEMS;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private List<ItemPickupEntity> itemsFor(String levelId) {
        return itemsByLevel.computeIfAbsent(levelId != null ? levelId : "unknown", k -> new ArrayList<>());
    }

    /**
     * Spawns an item in the active level.
     * @return The created ItemPickupEntity, or null if limit reached
     */
    public ItemPickupEntity spawnItem(ItemStack itemStack, float x, float y, float graceTimer) {
        return spawnItem(activeLevelId, itemStack, x, y, graceTimer);
    }

    /**
     * Spawns an item in a specific level.
     * @return The created ItemPickupEntity, or null if limit reached
     */
    public ItemPickupEntity spawnItem(String levelId, ItemStack itemStack, float x, float y, float graceTimer) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }

        List<ItemPickupEntity> items = itemsFor(levelId);
        if (items.size() >= maxWorldItems) {
            System.err.println("World item limit reached! Cannot spawn: " + itemStack.toString());
            return null;
        }

        ItemPickupEntity pickup = createPickup(itemStack, x, y, graceTimer);
        items.add(pickup);

        for (Listener listener : new ArrayList<>(listeners)) {
            listener.onItemSpawned(levelId, pickup);
        }
        return pickup;
    }

    private ItemPickupEntity createPickup(ItemStack itemStack, float x, float y, float graceTimer) {
        ItemPickupEntity pickup = new ItemPickupEntity(itemStack, x, y, graceTimer);
        String iconPath = itemStack.getDefinition().getIconPath();
        if (iconPath != null && itemTextures.containsKey(iconPath)) {
            pickup.setTexture(itemTextures.get(iconPath));
        }
        return pickup;
    }

    /**
     * Spawns multiple items in a pile around a position (in the active level).
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
     * Removes an item from whichever level it is in.
     */
    public void removeItem(ItemPickupEntity item) {
        for (Map.Entry<String, List<ItemPickupEntity>> entry : itemsByLevel.entrySet()) {
            if (entry.getValue().remove(item)) {
                notifyRemoved(entry.getKey(), item);
                return;
            }
        }
    }

    private void notifyRemoved(String levelId, ItemPickupEntity item) {
        for (Listener listener : new ArrayList<>(listeners)) {
            listener.onItemRemoved(levelId, item);
        }
    }

    /**
     * Updates all items in the active level.
     */
    public void update(float delta) {
        update(activeLevelId, delta);
    }

    /**
     * Updates all items in a level, removing any that became inactive (picked up).
     */
    public void update(String levelId, float delta) {
        List<ItemPickupEntity> items = itemsByLevel.get(levelId);
        if (items == null) return;

        for (int i = items.size() - 1; i >= 0; i--) {
            ItemPickupEntity item = items.get(i);
            if (!item.isActive()) {
                items.remove(i);
                notifyRemoved(levelId, item);
            } else {
                item.update(delta);
            }
        }
    }

    /**
     * Renders all items in the active level.
     */
    public void render(SpriteBatch batch) {
        List<ItemPickupEntity> items = itemsByLevel.get(activeLevelId);
        if (items == null) return;
        for (ItemPickupEntity item : items) {
            item.render(batch);
        }
    }

    /**
     * Gets all items near a position in the active level.
     */
    public List<ItemPickupEntity> getItemsNear(Vector2 position, float radius) {
        List<ItemPickupEntity> nearby = new ArrayList<>();
        List<ItemPickupEntity> items = itemsByLevel.get(activeLevelId);
        if (items == null) return nearby;

        float radiusSquared = radius * radius;
        for (ItemPickupEntity item : items) {
            if (item.hasComponent(com.game.systems.entity.Transform.class)) {
                Vector2 itemPos = item.getComponent(com.game.systems.entity.Transform.class).getPosition();
                if (position.dst2(itemPos) <= radiusSquared) {
                    nearby.add(item);
                }
            }
        }

        return nearby;
    }

    /**
     * Gets an item at a specific position in the active level.
     */
    public ItemPickupEntity getItemAt(Vector2 position, float tolerance) {
        List<ItemPickupEntity> nearby = getItemsNear(position, tolerance);
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    /**
     * Registers a texture for an item.
     */
    public void registerTexture(String iconPath, TextureRegion texture) {
        itemTextures.put(iconPath, texture);
    }

    /**
     * Gets a texture for an item.
     */
    public TextureRegion getTexture(String iconPath) {
        return itemTextures.get(iconPath);
    }

    /**
     * Clears all items in all levels (without notifying listeners).
     */
    public void clearAll() {
        itemsByLevel.clear();
    }

    /**
     * Items in the active level.
     */
    public List<ItemPickupEntity> getAllItems() {
        return getItems(activeLevelId);
    }

    public List<ItemPickupEntity> getItems(String levelId) {
        List<ItemPickupEntity> items = itemsByLevel.get(levelId);
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    /**
     * All levels that have (or had) items.
     */
    public List<String> getLevelIds() {
        return new ArrayList<>(itemsByLevel.keySet());
    }

    public int getItemCount() {
        return getAllItems().size();
    }

    public int getMaxWorldItems() {
        return maxWorldItems;
    }

    public void setMaxWorldItems(int maxWorldItems) {
        this.maxWorldItems = maxWorldItems;
    }

    /**
     * Set the level that level-less calls operate on.
     */
    public void setCurrentLevel(String levelId) {
        this.activeLevelId = levelId;
    }

    public String getCurrentLevel() {
        return activeLevelId;
    }

    /**
     * Clear items for a specific level (without notifying listeners).
     */
    public void clearLevel(String levelId) {
        itemsByLevel.remove(levelId);
    }

    // ========== Save/Load Support ==========

    /**
     * Export dropped items data for saving.
     * @return Map of level ID -> list of dropped item data
     */
    public Map<String, List<com.game.save.DroppedItemData>> exportSaveData() {
        Map<String, List<com.game.save.DroppedItemData>> result = new HashMap<>();

        for (Map.Entry<String, List<ItemPickupEntity>> entry : itemsByLevel.entrySet()) {
            List<com.game.save.DroppedItemData> droppedItemDataList = new ArrayList<>();

            for (ItemPickupEntity pickup : entry.getValue()) {
                ItemStack stack = pickup.getItemStack();
                com.game.systems.entity.Transform transform = pickup.getComponent(com.game.systems.entity.Transform.class);
                if (stack != null && transform != null && pickup.isActive()) {
                    droppedItemDataList.add(new com.game.save.DroppedItemData(
                        stack.getDefinition().getId(),
                        stack.getQuantity(),
                        transform.getX(),
                        transform.getY()
                    ));
                }
            }

            result.put(entry.getKey(), droppedItemDataList);
        }

        return result;
    }

    /**
     * Import dropped items data from save.
     * Clears existing items and recreates from save data.
     */
    public void importSaveData(Map<String, List<com.game.save.DroppedItemData>> data) {
        itemsByLevel.clear();

        for (Map.Entry<String, List<com.game.save.DroppedItemData>> entry : data.entrySet()) {
            List<ItemPickupEntity> levelItems = new ArrayList<>();

            for (com.game.save.DroppedItemData itemData : entry.getValue()) {
                com.game.systems.item.ItemDefinition def = com.game.systems.item.ItemRegistry.get(itemData.itemId);
                if (def == null) {
                    System.err.println("WorldItemManager: Item not found in registry: " + itemData.itemId);
                    continue;
                }

                levelItems.add(createPickup(new ItemStack(def, itemData.quantity), itemData.x, itemData.y, 0f));
            }

            itemsByLevel.put(entry.getKey(), levelItems);
        }

        System.out.println("WorldItemManager: Imported dropped items for " + itemsByLevel.size() + " levels");
    }
}
