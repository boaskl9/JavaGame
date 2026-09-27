package com.game.networking;

import com.game.integration.WorldManager;
import com.game.systems.audio.SoundSystem;
import com.game.systems.entity.Entity;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;
import com.game.world.LevelInstance;

import java.util.HashMap;
import java.util.Map;

import static com.game.systems.audio.SoundRegistry.*;

/**
 * The client side of a multiplayer session.
 *
 * The client simulates only its own player (and sends its state to the host). Everything
 * else in its level (enemies, breakables, items, other players) is a copy driven by the host.
 * Gameplay that affects the shared world (hits, pickups, drops) is requested from the host.
 */
public class ClientSession implements NetSession {
    private static final float PLAYER_STATE_INTERVAL = 1f / 30f;
    private static final float INVENTORY_SYNC_INTERVAL = 5f;
    private static final long PICKUP_RETRY_MS = 500;
    private static final int HOST_CLOCK = HostSession.HOST_PLAYER_ID;

    private final GameClient client;
    private final NetGameContext game;

    private int playerId = -1;
    private String levelId;
    private int epoch = 0;
    private boolean joining = false; // True while building the level from the Welcome packet

    private final Map<Integer, GameObject> entities = new HashMap<>();
    private final Map<Integer, ItemPickupEntity> items = new HashMap<>();
    private final Map<Integer, PlayerEntity> remotePlayers = new HashMap<>();
    private final Map<Integer, ClockSync> clocks = new HashMap<>();
    private final Map<Integer, Long> pickupRequestTimes = new HashMap<>();

    private float playerStateTimer = 0f;
    private float inventorySyncTimer = 0f;
    private boolean disposed = false;

    public ClientSession(GameClient client, NetGameContext game) {
        this.client = client;
        this.game = game;
    }

    @Override
    public boolean isHost() {
        return false;
    }

    public boolean isJoined() {
        return playerId >= 0;
    }

    // ========== Per-frame ==========

    @Override
    public void update(float delta) {
        PacketQueue.Incoming incoming;
        while (!disposed && (incoming = client.getQueue().poll()) != null) {
            handle(incoming.packet);
        }
        if (disposed || !isJoined()) return;

        playerStateTimer += delta;
        if (playerStateTimer >= PLAYER_STATE_INTERVAL) {
            playerStateTimer = 0f;
            PlayerEntity player = game.getLocalPlayer();
            if (player != null && levelId != null) {
                client.send(PlayerDataCodec.state(player, playerId, levelId));
            }
        }

        inventorySyncTimer += delta;
        if (inventorySyncTimer >= INVENTORY_SYNC_INTERVAL) {
            inventorySyncTimer = 0f;
            sendInventorySync();
        }
    }

    private void sendInventorySync() {
        PlayerEntity player = game.getLocalPlayer();
        if (player == null) return;
        Packets.InventorySync sync = new Packets.InventorySync();
        sync.playerJson = PlayerDataCodec.toJson(player);
        client.send(sync);
    }

    // ========== Packet handling ==========

    private void handle(Object packet) {
        if (packet == PacketQueue.DISCONNECTED) {
            if (!disposed) {
                game.onConnectionLost("Lost connection to the host.");
            }
        } else if (packet instanceof Packets.Welcome welcome) {
            onWelcome(welcome);
        } else if (packet instanceof Packets.PlayerState state) {
            onPlayerState(state);
        } else if (packet instanceof Packets.PlayerLeft left) {
            removeRemotePlayer(left.playerId);
        } else if (packet instanceof Packets.LevelSnapshot snapshot) {
            onLevelSnapshot(snapshot);
        } else if (packet instanceof Packets.EntitySpawn spawn) {
            if (spawn.epoch == epoch) spawnEntity(spawn);
        } else if (packet instanceof Packets.EntityDespawn despawn) {
            if (despawn.epoch == epoch) despawnEntity(despawn.netId);
        } else if (packet instanceof Packets.EntityStateBatch batch) {
            if (batch.epoch == epoch) onEntityStates(batch);
        } else if (packet instanceof Packets.Effect effect) {
            if (effect.epoch == epoch) onEffect(effect);
        } else if (packet instanceof Packets.PlayerHit hit) {
            PlayerEntity player = game.getLocalPlayer();
            if (player != null) {
                player.applyNetworkHit(hit.damage, hit.knockbackX, hit.knockbackY);
            }
        } else if (packet instanceof Packets.ItemGrant grant) {
            onItemGrant(grant);
        }
    }

    private void onWelcome(Packets.Welcome welcome) {
        playerId = welcome.playerId;
        epoch = 1;
        System.out.println("ClientSession: Joined as " + welcome.playerName + " (player " + playerId + ") in " + welcome.levelId);

        joining = true;
        try {
            game.startAsClient(playerId, welcome.levelId, welcome.x, welcome.y, welcome.savedPlayerJson);
        } finally {
            joining = false;
        }
    }

    private void onPlayerState(Packets.PlayerState state) {
        if (state.playerId == playerId) return;

        LevelInstance level = game.getCurrentInstance();
        PlayerEntity puppet = remotePlayers.get(state.playerId);

        if (level == null || !state.levelId.equals(levelId)) {
            // In another level: hide them
            if (puppet != null && puppet.getWorld() != null) {
                puppet.getWorld().removeGameObject(puppet);
            }
            return;
        }

        WorldManager world = level.getWorld();
        if (puppet == null) {
            puppet = game.createRemotePlayer(state.playerId, world, state.x, state.y);
            remotePlayers.put(state.playerId, puppet);
        }
        if (!world.contains(puppet)) {
            puppet.setWorld(world);
            puppet.clearSnapshotBuffer();
            puppet.getTransform().setPosition(state.x, state.y);
            world.addGameObject(puppet);
        }

        long localTime = clocks.computeIfAbsent(state.playerId, id -> new ClockSync()).toLocalTime(state.time);
        PlayerDataCodec.applyState(puppet, state, localTime);
    }

    private void removeRemotePlayer(int remoteId) {
        PlayerEntity puppet = remotePlayers.remove(remoteId);
        if (puppet == null) return;
        if (puppet.getWorld() != null) {
            puppet.getWorld().removeGameObject(puppet);
        }
        game.removeRemotePlayer(puppet);
        clocks.remove(remoteId);
    }

    private void onLevelSnapshot(Packets.LevelSnapshot snapshot) {
        if (snapshot.epoch != epoch) return;

        clearReplicatedEntities();
        for (Packets.EntitySpawn spawn : snapshot.entities) {
            spawnEntity(spawn);
        }
    }

    private void spawnEntity(Packets.EntitySpawn spawn) {
        LevelInstance level = game.getCurrentInstance();
        if (level == null || entities.containsKey(spawn.netId) || items.containsKey(spawn.netId)) return;

        if (Packets.TYPE_ITEM.equals(spawn.type)) {
            ItemStack stack = ItemFactory.create(spawn.itemId, spawn.quantity);
            if (stack == null) return;
            ItemPickupEntity item = game.getWorldItemManager().spawnItem(levelId, stack, spawn.x, spawn.y, 0f);
            if (item != null) {
                item.setNetId(spawn.netId);
                items.put(spawn.netId, item);
            }
            return;
        }

        GameObject obj = ReplicatedEntities.createPuppet(spawn, level.getWorld());
        if (obj != null) {
            entities.put(spawn.netId, obj);
            level.getWorld().addGameObject(obj);
        }
    }

    private void despawnEntity(int netId) {
        GameObject obj = entities.remove(netId);
        LevelInstance level = game.getCurrentInstance();
        if (obj != null && level != null) {
            level.getWorld().removeGameObject(obj);
        }

        ItemPickupEntity item = items.remove(netId);
        if (item != null) {
            game.getWorldItemManager().removeItem(item);
        }
        pickupRequestTimes.remove(netId);
    }

    private void clearReplicatedEntities() {
        for (Integer netId : new java.util.ArrayList<>(entities.keySet())) {
            despawnEntity(netId);
        }
        for (Integer netId : new java.util.ArrayList<>(items.keySet())) {
            despawnEntity(netId);
        }
    }

    private void onEntityStates(Packets.EntityStateBatch batch) {
        long localTime = clocks.computeIfAbsent(HOST_CLOCK, id -> new ClockSync()).toLocalTime(batch.time);
        for (int i = 0; i < batch.netIds.length; i++) {
            if (!(entities.get(batch.netIds[i]) instanceof Entity entity)) continue;
            entity.enqueueSnapshot(new EntitySnapshot(
                localTime, batch.x[i], batch.y[i], batch.vx[i], batch.vy[i],
                batch.anims[i], batch.dirs[i], batch.flips[i], batch.hp[i], entity.getMaxHealth()
            ));
            entity.setHealth(batch.hp[i]);
        }
    }

    private void onEffect(Packets.Effect effect) {
        switch (effect.kind) {
            case Packets.Effect.DAMAGE_NUMBER -> {
                game.showDamageNumber(effect.x, effect.y, effect.amount);
                SoundSystem.getInstance().playSound(SWORD_HIT, 0.8f);
            }
            case Packets.Effect.DEATH -> {
                game.showDeathAnimation(effect.x, effect.y);
                SoundSystem.getInstance().playSound(ENEMY_DEATH, 0.9f);
            }
            case Packets.Effect.BREAK -> {
                if (entities.get(effect.netId) instanceof BreakableEntity breakable) {
                    breakable.playBreakVisual();
                }
            }
            case Packets.Effect.ENEMY_ATTACK -> {
                if (entities.get(effect.netId) instanceof EnemyEntity enemy) {
                    enemy.playAttackVisual(effect.angle);
                }
            }
            case Packets.Effect.PLAYER_ATTACK -> {
                PlayerEntity puppet = remotePlayers.get(effect.playerId);
                if (puppet != null) {
                    puppet.startRemoteAttack(effect.angle, effect.text);
                    SoundSystem.getInstance().playSound(SWORD_SWING);
                }
            }
            default -> {
            }
        }
    }

    private void onItemGrant(Packets.ItemGrant grant) {
        PlayerEntity player = game.getLocalPlayer();
        ItemStack stack = ItemFactory.create(grant.itemId, grant.quantity);
        if (player == null || stack == null) return;

        ItemStack remaining = player.getInventory().addItem(stack);
        if (remaining != null && remaining.getQuantity() > 0) {
            // Didn't fit (inventory changed since the request): put the rest back
            dropItem(remaining, player.getTransform().getX(), player.getTransform().getY());
        }
        SoundSystem.getInstance().playSound(COIN_PICKUP, 0.6f);
        game.onInventoryChanged();
    }

    // ========== Local player ==========

    @Override
    public void onLocalPlayerCreated(PlayerEntity player) {
        player.setPlayerId(playerId);
        player.setResolveHits(false); // The host decides what our attacks hit
    }

    @Override
    public void onLocalLevelChanged(LevelInstance newInstance) {
        // Everything replicated belonged to the old level (whose instance is gone)
        entities.clear();
        items.clear();
        pickupRequestTimes.clear();
        for (PlayerEntity puppet : remotePlayers.values()) {
            if (puppet.getWorld() != null) {
                puppet.getWorld().removeGameObject(puppet);
            }
        }

        levelId = newInstance.getLevelId();
        if (joining) return; // The host already put us in this level

        epoch++;
        Packets.LevelChange change = new Packets.LevelChange();
        change.levelId = levelId;
        change.epoch = epoch;
        client.send(change);
        playerStateTimer = PLAYER_STATE_INTERVAL; // Send our new position right away
    }

    @Override
    public void onInstanceCreated(LevelInstance instance) {
        // Clients build replica levels; the host fills them
    }

    @Override
    public void onInstanceDisposed(LevelInstance instance) {
    }

    @Override
    public boolean isLevelOccupied(String levelId) {
        return false;
    }

    @Override
    public void onLocalAttack(float attackAngle, String weaponId) {
        Packets.AttackRequest request = new Packets.AttackRequest();
        request.angle = attackAngle;
        request.weaponId = weaponId;
        client.send(request);
    }

    @Override
    public boolean requestPickup(ItemPickupEntity item) {
        int netId = item.getNetId();
        if (netId == 0) return true;

        long now = System.currentTimeMillis();
        Long last = pickupRequestTimes.get(netId);
        if (last == null || now - last >= PICKUP_RETRY_MS) {
            pickupRequestTimes.put(netId, now);
            Packets.PickupRequest request = new Packets.PickupRequest();
            request.netId = netId;
            client.send(request);
        }
        return true;
    }

    @Override
    public boolean dropItem(ItemStack stack, float x, float y) {
        Packets.DropItem drop = new Packets.DropItem();
        drop.itemId = stack.getDefinition().getId();
        drop.quantity = stack.getQuantity();
        drop.x = x;
        drop.y = y;
        client.send(drop);
        return true;
    }

    @Override
    public void broadcastEffect(String levelId, Packets.Effect effect) {
        // Only the host produces effects
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (isJoined()) {
            sendInventorySync(); // So the host can save our character
        }
        client.disconnect();
    }
}
