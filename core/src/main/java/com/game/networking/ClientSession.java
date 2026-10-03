package com.game.networking;

import com.game.integration.WorldManager;
import com.game.systems.audio.SoundSystem;
import com.game.systems.entity.Entity;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.furniture.ChestEntity;
import com.game.systems.furniture.FurnitureEntity;
import com.game.systems.furniture.FurnitureFactory;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;
import com.game.world.LevelInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    // Furniture
    private final Map<Integer, Consumer<Boolean>> pendingPlacements = new HashMap<>(); // By request ID
    private final Map<Integer, Consumer<Boolean>> pendingChestOpens = new HashMap<>(); // By chest net ID
    private int nextRequestId = 1;
    private ChestEntity openChest;         // Chest this player is using (the host holds the lock for us)
    private String openChestSignature;     // Contents last sent to the host

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

        syncOpenChest();
    }

    /**
     * Send the open chest's contents to the host whenever the player changed them.
     */
    private void syncOpenChest() {
        if (openChest == null) return;
        String signature = FurnitureFactory.signature(openChest.getContainer());
        if (signature.equals(openChestSignature)) return;

        openChestSignature = signature;
        Packets.ChestContents contents = new Packets.ChestContents();
        contents.netId = openChest.getNetId();
        contents.itemIds = FurnitureFactory.contentIds(openChest.getContainer());
        contents.quantities = FurnitureFactory.contentQuantities(openChest.getContainer());
        client.send(contents);
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
        } else if (packet instanceof Packets.CharacterList list) {
            if (!isJoined()) {
                game.chooseCharacter(list.characters, list.message);
            }
        } else if (packet instanceof Packets.DayState state) {
            game.getDayCycle().set(state.day, state.elapsed, state.dayLength, state.worldSeed);
        } else if (packet instanceof Packets.NewDay newDay) {
            if (isJoined()) game.onNewDay(newDay.day, newDay.passedOut);
        } else if (packet instanceof Packets.SleepStatus status) {
            game.onSleepStatus(status.asleep, status.total);
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
        } else if (packet instanceof Packets.PlaceFurnitureResult result) {
            Consumer<Boolean> callback = pendingPlacements.remove(result.requestId);
            if (callback != null) callback.accept(result.placed);
        } else if (packet instanceof Packets.ChestOpenResult result) {
            onChestOpenResult(result);
        }
    }

    /** Play an existing character from the host's list. */
    public void playCharacter(String characterId) {
        Packets.ChooseCharacter choice = new Packets.ChooseCharacter();
        choice.characterId = characterId;
        client.send(choice);
    }

    /** Create a new character in the host's world. */
    public void createCharacter(String name) {
        Packets.ChooseCharacter choice = new Packets.ChooseCharacter();
        choice.newName = name;
        client.send(choice);
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
        if (PlayerDataCodec.applyState(puppet, state, localTime)) {
            game.showDeathAnimation(state.x, state.y);
        }
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

    // ========== Furniture ==========

    private void onChestOpenResult(Packets.ChestOpenResult result) {
        Consumer<Boolean> callback = pendingChestOpens.remove(result.netId);
        ChestEntity chest = entities.get(result.netId) instanceof ChestEntity c ? c : null;

        boolean granted = result.granted && chest != null;
        if (granted) {
            FurnitureFactory.setContents(chest.getContainer(), result.itemIds, result.quantities);
            openChest = chest;
            openChestSignature = FurnitureFactory.signature(chest.getContainer());
        } else if (result.granted) {
            // Chest vanished locally meanwhile: give the lock back
            Packets.ChestClose close = new Packets.ChestClose();
            close.netId = result.netId;
            client.send(close);
        }
        if (callback != null) callback.accept(granted);
    }

    @Override
    public boolean placeFurniture(String itemId, float x, float y, Consumer<Boolean> onResult) {
        Packets.PlaceFurnitureRequest request = new Packets.PlaceFurnitureRequest();
        request.requestId = nextRequestId++;
        request.itemId = itemId;
        request.x = x;
        request.y = y;
        pendingPlacements.put(request.requestId, onResult);
        client.send(request);
        return true;
    }

    @Override
    public boolean pickUpFurniture(FurnitureEntity furniture) {
        if (furniture.getNetId() == 0) return true;
        Packets.PickUpFurnitureRequest request = new Packets.PickUpFurnitureRequest();
        request.netId = furniture.getNetId();
        client.send(request);
        return true;
    }

    @Override
    public void openChest(ChestEntity chest, Consumer<Boolean> onResult) {
        if (chest.getNetId() == 0 || pendingChestOpens.containsKey(chest.getNetId())) {
            return;
        }
        pendingChestOpens.put(chest.getNetId(), onResult);
        Packets.ChestOpenRequest request = new Packets.ChestOpenRequest();
        request.netId = chest.getNetId();
        client.send(request);
    }

    @Override
    public void closeChest(ChestEntity chest) {
        if (openChest != chest) return;
        syncOpenChest(); // Make sure the host has the final contents
        Packets.ChestClose close = new Packets.ChestClose();
        close.netId = chest.getNetId();
        client.send(close);
        openChest = null;
        openChestSignature = null;
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
        pendingChestOpens.clear();
        openChest = null;
        openChestSignature = null;
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
    public void onLocalSleepChanged(boolean asleep) {
        Packets.Sleep sleep = new Packets.Sleep();
        sleep.asleep = asleep;
        client.send(sleep);
    }

    @Override
    public void onDayEnded(boolean passedOut) {
        // Only the host ends days
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (isJoined()) {
            syncOpenChest();
            sendInventorySync(); // So the host can save our character
        }
        client.disconnect();
    }
}
