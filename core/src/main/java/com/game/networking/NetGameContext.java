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

    /**
     * Client: the host's characters. Let the player pick one or create one
     * (then call ClientSession.playCharacter / createCharacter).
     * @param message why the previous choice was refused, or null
     */
    void chooseCharacter(Packets.CharacterInfo[] characters, String message);

    /** Client: the host told us where to start. Build that level and create the local player. */
    void startAsClient(int playerId, String levelId, float x, float y, String savedPlayerJson);

    // ========== Days ==========

    com.game.world.DayCycle getDayCycle();

    boolean isLocalPlayerSleeping();

    /** Host: everyone is asleep; start the next day. */
    void endDay(boolean passedOut);

    /** Client: the host started a new day. */
    void onNewDay(int day, boolean passedOut);

    /** How many players are asleep (shown while sleeping). */
    void onSleepStatus(int asleep, int total);

    /** Host: a guest left this level; unload it if nobody needs it any more (e.g. a dungeon). */
    void releaseLevelIfUnused(String levelId);

    /** Place furniture in a level (authoritative). @return the furniture, or null if it can't go there */
    com.game.systems.furniture.FurnitureEntity placeFurniture(String levelId, String itemId, float x, float y);

    /** Remove furniture from a level and from the saved furniture. */
    void removeFurniture(String levelId, com.game.systems.furniture.FurnitureEntity furniture);

    void showDamageNumber(float x, float y, int amount);

    void showDeathAnimation(float x, float y);

    void onInventoryChanged();

    void onConnectionLost(String reason);
}
