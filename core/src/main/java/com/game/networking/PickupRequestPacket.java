package com.game.networking;

/**
 * Client-to-server request to pick up an item.
 * Replaces immediate client-side pickup with server-validated request-confirm flow.
 *
 * Flow:
 * 1. Client detects collision with item
 * 2. Client sends PickupRequestPacket to server
 * 3. Server validates:
 *    - Item still exists (not already picked up)
 *    - Player has inventory space
 *    - Player is close enough to item
 * 4. If valid: Server adds to inventory, broadcasts ItemPickupEvent
 * 5. If invalid: Server sends failure event
 * 6. All clients remove item from world on success event
 *
 * This prevents race conditions where two players pick up the same item.
 */
public class PickupRequestPacket extends Packet {
    /**
     * Player requesting the pickup
     */
    public int playerId;

    /**
     * Global unique ID of the item entity to pick up
     */
    public int itemEntityId;

    /**
     * Client timestamp when request was made (for latency tracking)
     */
    public long clientTimestamp;

    /**
     * Default constructor for Kryo serialization
     */
    public PickupRequestPacket() {
    }

    /**
     * Create a pickup request
     * @param playerId Player making the request
     * @param itemEntityId Item to pick up
     */
    public PickupRequestPacket(int playerId, int itemEntityId) {
        this.playerId = playerId;
        this.itemEntityId = itemEntityId;
        this.clientTimestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PICKUP_REQUEST;
    }
}
