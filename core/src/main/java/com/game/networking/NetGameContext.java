package com.game.networking;

import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.systems.entity.PlayerManager;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.world.LevelInstance;

import java.util.Collection;

/**
 * What a network session needs from the game. Implemented by GameScreen.
 */
public interface NetGameContext {

    PlayerEntity getLocalPlayer();

    PlayerManager getPlayerManager();

    WorldItemManager getWorldItemManager();

    /** The level the local player is in (null while a client is still connecting). */
    LevelInstance getCurrentInstance();

    /** Every level instance loaded on this machine. */
    Collection<LevelInstance> getInstances();

    /** Get a loaded level, or build it (host only). */
    LevelInstance getOrCreateInstance(String levelId);

    /** Create another machine's player as a network-controlled copy and register it. */
    PlayerEntity createRemotePlayer(int playerId, WorldManager world, float x, float y);

    /** Remove a remote player's copy from the game entirely. */
    void removeRemotePlayer(PlayerEntity player);

    /** Client: the host told us where to start. Build that level and create the local player. */
    void startAsClient(int playerId, String levelId, float x, float y, String savedPlayerJson);

    /** Place furniture in a level (authoritative). @return the furniture, or null if it can't go there */
    com.game.systems.furniture.FurnitureEntity placeFurniture(String levelId, String itemId, float x, float y);

    /** Remove furniture from a level and from the saved furniture. */
    void removeFurniture(String levelId, com.game.systems.furniture.FurnitureEntity furniture);

    void showDamageNumber(float x, float y, int amount);

    void showDeathAnimation(float x, float y);

    void onInventoryChanged();

    void onConnectionLost(String reason);
}
