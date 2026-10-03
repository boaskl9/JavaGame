package com.game.networking;

import com.esotericsoftware.kryo.Kryo;

/**
 * Every message exchanged between host and clients.
 *
 * Authority model:
 * - Each machine owns its own player's movement and sends {@link PlayerState}.
 * - The host owns everything else (enemies, breakables, items, damage) and replicates it.
 * - Clients ask the host to do things with request packets (attack, pickup, drop).
 *
 * World packets sent from the host carry the recipient's level epoch. A client drops
 * anything stamped with an older epoch, so late packets from a level it already left
 * can't leak into the new one.
 */
public final class Packets {

    private Packets() {
    }

    /**
     * Register all packet classes. Host and client must register in the same order.
     */
    public static void register(Kryo kryo) {
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(boolean[].class);
        kryo.register(String[].class);

        kryo.register(Hello.class);
        kryo.register(CharacterInfo.class);
        kryo.register(CharacterInfo[].class);
        kryo.register(CharacterList.class);
        kryo.register(ChooseCharacter.class);
        kryo.register(Welcome.class);
        kryo.register(PlayerLeft.class);
        kryo.register(PlayerState.class);
        kryo.register(LevelChange.class);
        kryo.register(EntitySpawn.class);
        kryo.register(EntitySpawn[].class);
        kryo.register(LevelSnapshot.class);
        kryo.register(EntityDespawn.class);
        kryo.register(EntityStateBatch.class);
        kryo.register(Effect.class);
        kryo.register(AttackRequest.class);
        kryo.register(PlayerHit.class);
        kryo.register(PickupRequest.class);
        kryo.register(ItemGrant.class);
        kryo.register(DropItem.class);
        kryo.register(InventorySync.class);
        kryo.register(PlaceFurnitureRequest.class);
        kryo.register(PlaceFurnitureResult.class);
        kryo.register(PickUpFurnitureRequest.class);
        kryo.register(ChestOpenRequest.class);
        kryo.register(ChestOpenResult.class);
        kryo.register(ChestContents.class);
        kryo.register(ChestClose.class);
        kryo.register(DayState.class);
        kryo.register(NewDay.class);
        kryo.register(Sleep.class);
        kryo.register(SleepStatus.class);
    }

    // ========== Join / leave ==========

    /**
     * Client → host, first message after connecting. The identity only marks which characters this
     * player played last; any guest can play any character that isn't in use.
     */
    public static class Hello {
        public String identityProvider; // e.g. "local", later "steam"
        public String identityId;       // Unique within the provider
    }

    /** One character saved in the host's world. */
    public static class CharacterInfo {
        public String id;
        public String name;
        public String levelId;          // Where they were last (nullable)
        public boolean inUse;           // Someone is playing this character right now
        public boolean lastPlayedByYou;
    }

    /**
     * Host → client, reply to {@link Hello}: pick a character. Sent again whenever the list changes
     * (someone joins or leaves) until the client has joined, and when a choice is refused.
     */
    public static class CharacterList {
        public CharacterInfo[] characters;
        public String message; // Why the last choice was refused (nullable)
    }

    /** Client → host: play an existing character (characterId), or create one (characterId null, newName). */
    public static class ChooseCharacter {
        public String characterId;
        public String newName;
    }

    /** Host → client, once a character was chosen. The client builds its level from this. */
    public static class Welcome {
        public int playerId;
        public String playerName;   // The character's name
        public String levelId;
        public float x, y;
        public String savedPlayerJson; // PlayerData from an earlier session (nullable)
    }

    /** Host → clients. */
    public static class PlayerLeft {
        public int playerId;
    }

    // ========== Players (owner-authoritative) ==========

    /** Owner → host (and relayed by the host to other clients). */
    public static class PlayerState {
        public int playerId;
        public String levelId;
        public long time; // Sender clock (ms), mapped to local time on arrival
        public float x, y, vx, vy;
        public String anim;
        public int dir;
        public boolean flipX;
        public int hp, maxHp;
        public String weaponId;
    }

    /** Client → host, sent right after the client has loaded a new level locally. */
    public static class LevelChange {
        public String levelId;
        public int epoch;
    }

    // ========== Replication (host-authoritative) ==========

    public static final String TYPE_ITEM = "item";

    /** Host → client: an entity exists. Types: "enemy:lizard", "breakable:pot", "item". */
    public static class EntitySpawn {
        public int epoch;
        public int netId;
        public String type;
        public float x, y;
        public float vx, vy; // Projectiles only
        public int hp, maxHp;
        public String itemId;
        public int quantity;
    }

    /** Host → client: every replicated entity in the level the client just entered. */
    public static class LevelSnapshot {
        public int epoch;
        public String levelId;
        public EntitySpawn[] entities;
    }

    public static class EntityDespawn {
        public int epoch;
        public int netId;
    }

    /** Host → clients in a level, ~20Hz: positions and animation of moving entities. */
    public static class EntityStateBatch {
        public int epoch;
        public long time;
        public int[] netIds;
        public float[] x, y, vx, vy;
        public String[] anims;
        public int[] dirs;
        public boolean[] flips;
        public int[] hp;
    }

    /** Host → clients in a level: one-shot visuals and sounds. */
    public static class Effect {
        public static final int DAMAGE_NUMBER = 1; // x, y, amount
        public static final int DEATH = 2;         // x, y
        public static final int BREAK = 3;         // netId
        public static final int ENEMY_ATTACK = 4;  // netId, angle
        public static final int PLAYER_ATTACK = 5; // playerId, angle, text = weaponId

        public int epoch;
        public int kind;
        public int netId;
        public int playerId;
        public float x, y, angle;
        public int amount;
        public String text;
    }

    // ========== Gameplay requests and results ==========

    /** Client → host: the client's player started an attack. The host resolves the hits. */
    public static class AttackRequest {
        public float angle;
        public String weaponId;
    }

    /** Host → owning client: your player was hit. */
    public static class PlayerHit {
        public int damage;
        public float knockbackX, knockbackY;
    }

    /** Client → host: my player is touching this item. */
    public static class PickupRequest {
        public int netId;
    }

    /** Host → client: add this to your inventory. */
    public static class ItemGrant {
        public String itemId;
        public int quantity;
    }

    /** Client → host: drop this into the world at my position. */
    public static class DropItem {
        public String itemId;
        public int quantity;
        public float x, y;
    }

    /** Client → host: full player data (inventory, equipment, health) so the host can save it. */
    public static class InventorySync {
        public String playerJson;
    }

    // ========== Furniture ==========

    /** Client → host: place a furniture item from my inventory here. */
    public static class PlaceFurnitureRequest {
        public int requestId;
        public String itemId;
        public float x, y;
    }

    /** Host → client: whether the placement happened (only then does the client use up the item). */
    public static class PlaceFurnitureResult {
        public int requestId;
        public boolean placed;
    }

    /** Client → host: pick this furniture up into my inventory (answered with ItemGrant). */
    public static class PickUpFurnitureRequest {
        public int netId;
    }

    /** Client → host: I want to use this chest. Only one player can use a chest at a time. */
    public static class ChestOpenRequest {
        public int netId;
    }

    /** Host → client: granted (with the current contents) or refused because someone else has it open. */
    public static class ChestOpenResult {
        public int netId;
        public boolean granted;
        public String[] itemIds; // One per slot, null = empty
        public int[] quantities;
    }

    /** Client → host: the chest I have open now contains this. */
    public static class ChestContents {
        public int netId;
        public String[] itemIds;
        public int[] quantities;
    }

    /** Client → host: I closed the chest. */
    public static class ChestClose {
        public int netId;
    }

    // ========== Days ==========

    /** Host → clients: the world's clock (before Welcome, then every second). */
    public static class DayState {
        public int day;
        public float elapsed;   // Real seconds into the day
        public float dayLength; // Real seconds per day
        public long worldSeed;  // Seeds per-day content such as dungeon layouts
    }

    /** Host → clients: the day ended (everyone slept, or time ran out); wake up at home. */
    public static class NewDay {
        public int day;
        public boolean passedOut;
    }

    /** Client → host: I went to bed (or got up again). The day ends when everyone is asleep. */
    public static class Sleep {
        public boolean asleep;
    }

    /** Host → clients: how many players are asleep. */
    public static class SleepStatus {
        public int asleep;
        public int total;
    }
}
