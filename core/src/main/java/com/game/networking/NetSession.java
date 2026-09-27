package com.game.networking;

import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.item.ItemStack;
import com.game.world.LevelInstance;

/**
 * A running multiplayer session, seen from the game screen.
 * Implemented by {@link HostSession} and {@link ClientSession}.
 */
public interface NetSession {

    boolean isHost();

    /** Called once per frame: handle received packets and send periodic updates. */
    void update(float delta);

    /** The local player was created (or re-created). */
    void onLocalPlayerCreated(PlayerEntity player);

    /** The local player moved into a different level instance. */
    void onLocalLevelChanged(LevelInstance newInstance);

    /** A new level instance was built on this machine. */
    void onInstanceCreated(LevelInstance instance);

    /** A level instance was unloaded on this machine. */
    void onInstanceDisposed(LevelInstance instance);

    /** Whether any remote player is in this level (the host keeps such levels simulating). */
    boolean isLevelOccupied(String levelId);

    /** The local player started an attack. */
    void onLocalAttack(float attackAngle, String weaponId);

    /**
     * The local player is touching an item.
     * @return true if the session handles the pickup (client asks the host), false to pick it up locally
     */
    boolean requestPickup(ItemPickupEntity item);

    /**
     * The local player dropped an item into the world.
     * @return true if the session handles it (client asks the host), false to spawn it locally
     */
    boolean dropItem(ItemStack stack, float x, float y);

    /** One-shot visual that happened in a level on the host, to be shown to players there. */
    void broadcastEffect(String levelId, Packets.Effect effect);

    /** Leave the session and close the connection(s). */
    void dispose();
}
