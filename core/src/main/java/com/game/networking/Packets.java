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
    }

    // ========== Join / leave ==========

    /** Client → host, first message after connecting. */
    public static class Hello {
        public String playerName;
    }

    /** Host → client, reply to {@link Hello}. The client builds its level from this. */
    public static class Welcome {
        public int playerId;
        public String playerName;   // Possibly de-duplicated by the host
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
}
