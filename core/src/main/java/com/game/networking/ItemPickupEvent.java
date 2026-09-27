package com.game.networking;

/**
 * Server-to-all broadcast when a player picks up an item.
 * Sent after server validates pickup request.
 *
 * Flow:
 * 1. Server receives PickupRequestPacket
 * 2. Server validates pickup (item exists, player has space, etc.)
 * 3. Server adds item to player's inventory
 * 4. Server removes item from world
 * 5. Server broadcasts ItemPickupEvent to all clients
 * 6. All clients remove item from their local world
 * 7. Picking client updates inventory UI
 *
 * This ensures all clients see consistent item state.
 */
public class ItemPickupEvent extends Packet {
    /**
     * Player who picked up the item
     */
    public int playerId;

    /**
     * Global unique ID of the item entity that was picked up
     */
    public int itemEntityId;

    /**
     * Item ID (e.g., "health_potion")
     * Used for logging and client-side feedback
     */
    public String itemId;

    /**
     * Quantity that was picked up
     */
    public int quantity;

    /**
     * Whether the pickup succeeded
     * false = failed (item already gone, inventory full, etc.)
     */
    public boolean success;

    /**
     * Reason for failure (if success = false)
     * Examples: "Item already picked up", "Inventory full", "Too far away"
     */
    public String failureReason;

    /**
     * Default constructor for Kryo serialization
     */
    public ItemPickupEvent() {
    }

    /**
     * Create a successful pickup event
     * @param playerId Player who picked up
     * @param itemEntityId Item entity ID
     * @param itemId Item definition ID
     * @param quantity Quantity picked up
     * @return Success event
     */
    public static ItemPickupEvent success(int playerId, int itemEntityId, String itemId, int quantity) {
        ItemPickupEvent event = new ItemPickupEvent();
        event.playerId = playerId;
        event.itemEntityId = itemEntityId;
        event.itemId = itemId;
        event.quantity = quantity;
        event.success = true;
        return event;
    }

    /**
     * Create a failed pickup event
     * @param playerId Player who tried to pick up
     * @param itemEntityId Item entity ID
     * @param failureReason Reason for failure
     * @return Failure event
     */
    public static ItemPickupEvent failure(int playerId, int itemEntityId, String failureReason) {
        ItemPickupEvent event = new ItemPickupEvent();
        event.playerId = playerId;
        event.itemEntityId = itemEntityId;
        event.success = false;
        event.failureReason = failureReason;
        return event;
    }

    @Override
    public PacketType getType() {
        return PacketType.ITEM_PICKUP_EVENT;
    }
}
