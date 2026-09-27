package com.game.networking;

import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.save.PlayerData;
import com.game.save.SaveManager;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.game.systems.item.ItemStack;
import com.game.world.LevelInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The host side of a multiplayer session.
 *
 * - Runs the authoritative simulation for every level with a player in it.
 * - Gives every enemy, breakable and world item a network ID and replicates it to the
 *   guests in the same level (spawn / state / despawn / effects).
 * - Keeps a network-controlled copy of each guest's player, positioned from the guest's
 *   own state packets, and resolves the guest's attacks and pickups.
 */
public class HostSession implements NetSession {
    public static final int HOST_PLAYER_ID = 0;
    private static final String FALLBACK_LEVEL = "Maps/prototype.tmx";
    private static final float PLAYER_STATE_INTERVAL = 1f / 30f;
    private static final float ENTITY_STATE_INTERVAL = 1f / 20f;
    private static final float MAX_PICKUP_DISTANCE = 64f; // Generous: magnet range + the guest's copy lagging behind the guest
    private static final float DROPPED_ITEM_GRACE = 1.5f;

    private final GameServer server;
    private final NetGameContext game;

    private final Map<Integer, Guest> guests = new HashMap<>(); // By connection ID
    private final Map<Integer, Tracked> tracked = new HashMap<>(); // By net ID
    private final WorldItemManager.Listener itemListener = new ItemReplicator();
    private int nextPlayerId = HOST_PLAYER_ID + 1;
    private int nextNetId = 1;

    private float playerStateTimer = 0f;
    private float entityStateTimer = 0f;
    private boolean disposed = false;

    private static final class Guest {
        final int connectionId;
        int playerId = -1;
        String name;
        String levelId;
        int epoch;
        PlayerEntity player; // Network-controlled copy on the host
        final ClockSync clock = new ClockSync();

        Guest(int connectionId) {
            this.connectionId = connectionId;
        }

        boolean isJoined() {
            return player != null;
        }
    }

    private record Tracked(String levelId, GameObject obj) {
    }

    public HostSession(NetGameContext game) {
        this.game = game;
        this.server = new GameServer();
    }

    /**
     * Start listening and begin replicating the levels that are already loaded.
     * @return false if the server could not start (e.g. port in use)
     */
    public boolean start() {
        if (!server.start()) {
            return false;
        }

        for (LevelInstance instance : game.getInstances()) {
            onInstanceCreated(instance);
        }

        WorldItemManager items = game.getWorldItemManager();
        for (String levelId : items.getLevelIds()) {
            for (ItemPickupEntity item : items.getItems(levelId)) {
                track(levelId, item);
            }
        }
        items.addListener(itemListener);

        PlayerEntity host = game.getLocalPlayer();
        if (host != null) {
            host.setPlayerId(HOST_PLAYER_ID);
        }

        System.out.println("HostSession: Hosting on port " + server.getPort());
        return true;
    }

    @Override
    public boolean isHost() {
        return true;
    }

    // ========== Per-frame ==========

    @Override
    public void update(float delta) {
        PacketQueue.Incoming incoming;
        while ((incoming = server.getQueue().poll()) != null) {
            handle(incoming.connectionId, incoming.packet);
        }

        playerStateTimer += delta;
        if (playerStateTimer >= PLAYER_STATE_INTERVAL) {
            playerStateTimer = 0f;
            sendHostPlayerState();
        }

        entityStateTimer += delta;
        if (entityStateTimer >= ENTITY_STATE_INTERVAL) {
            entityStateTimer = 0f;
            sendEntityStates();
        }
    }

    private void sendHostPlayerState() {
        PlayerEntity host = game.getLocalPlayer();
        LevelInstance level = game.getCurrentInstance();
        if (host == null || level == null) return;

        Packets.PlayerState state = PlayerDataCodec.state(host, HOST_PLAYER_ID, level.getLevelId());
        for (Guest guest : guests.values()) {
            if (guest.isJoined()) {
                server.send(guest.connectionId, state);
            }
        }
    }

    /**
     * Send positions of all moving entities, per level, to the guests in that level.
     */
    private void sendEntityStates() {
        Map<String, Packets.EntityStateBatch> batches = new HashMap<>();

        for (Guest guest : guests.values()) {
            if (!guest.isJoined()) continue;

            Packets.EntityStateBatch batch = batches.computeIfAbsent(guest.levelId, this::buildStateBatch);
            if (batch == null || batch.netIds.length == 0) continue;

            batch.epoch = guest.epoch;
            server.send(guest.connectionId, batch); // Serialized immediately, so reusing the batch is safe
        }
    }

    private Packets.EntityStateBatch buildStateBatch(String levelId) {
        LevelInstance instance = findInstance(levelId);
        if (instance == null) return null;

        List<EnemyEntity> enemies = new ArrayList<>();
        for (GameObject obj : instance.getWorld().getGameObjects()) {
            if (obj instanceof EnemyEntity enemy && enemy.getNetId() != 0 && enemy.isActive()) {
                enemies.add(enemy);
            }
        }

        int n = enemies.size();
        Packets.EntityStateBatch batch = new Packets.EntityStateBatch();
        batch.time = System.currentTimeMillis();
        batch.netIds = new int[n];
        batch.x = new float[n];
        batch.y = new float[n];
        batch.vx = new float[n];
        batch.vy = new float[n];
        batch.anims = new String[n];
        batch.dirs = new int[n];
        batch.flips = new boolean[n];
        batch.hp = new int[n];
        for (int i = 0; i < n; i++) {
            ReplicatedEntities.writeState(enemies.get(i), batch, i);
        }
        return batch;
    }

    // ========== Packet handling ==========

    private void handle(int connectionId, Object packet) {
        if (packet == PacketQueue.CONNECTED) {
            guests.put(connectionId, new Guest(connectionId));
            return;
        }

        Guest guest = guests.get(connectionId);
        if (guest == null) return;

        if (packet == PacketQueue.DISCONNECTED) {
            onGuestLeft(guest);
        } else if (packet instanceof Packets.Hello hello) {
            onHello(guest, hello);
        } else if (!guest.isJoined()) {
            return; // Ignore anything else until the guest has joined
        } else if (packet instanceof Packets.PlayerState state) {
            onGuestState(guest, state);
        } else if (packet instanceof Packets.LevelChange change) {
            onGuestLevelChange(guest, change);
        } else if (packet instanceof Packets.AttackRequest attack) {
            onGuestAttack(guest, attack);
        } else if (packet instanceof Packets.PickupRequest pickup) {
            onGuestPickup(guest, pickup);
        } else if (packet instanceof Packets.DropItem drop) {
            onGuestDrop(guest, drop);
        } else if (packet instanceof Packets.InventorySync sync) {
            PlayerData data = PlayerDataCodec.fromJson(sync.playerJson);
            if (data != null) {
                SaveManager.getInstance().putGuestData(guest.name, data);
            }
        }
    }

    private void onHello(Guest guest, Packets.Hello hello) {
        if (guest.isJoined()) return;

        guest.playerId = nextPlayerId++;
        guest.name = uniqueName(hello.playerName);

        // Join the host's level at the host's position; generated dungeons can't be shared yet
        LevelInstance level = game.getCurrentInstance();
        PlayerEntity host = game.getLocalPlayer();
        float x;
        float y;
        if (level != null && level.isShareable() && host != null) {
            x = host.getTransform().getX();
            y = host.getTransform().getY();
        } else {
            level = game.getOrCreateInstance(FALLBACK_LEVEL);
            com.badlogic.gdx.math.Vector2 spawn = level.getSpawnPosition(null);
            x = spawn.x;
            y = spawn.y;
        }

        guest.levelId = level.getLevelId();
        guest.epoch = 1;
        guest.player = createGuestPlayer(guest, level.getWorld(), x, y);

        Packets.Welcome welcome = new Packets.Welcome();
        welcome.playerId = guest.playerId;
        welcome.playerName = guest.name;
        welcome.levelId = guest.levelId;
        welcome.x = x;
        welcome.y = y;
        welcome.savedPlayerJson = PlayerDataCodec.toJson(SaveManager.getInstance().getGuestData(guest.name));
        server.send(guest.connectionId, welcome);
        sendLevelSnapshot(guest);

        System.out.println("HostSession: " + guest.name + " joined as player " + guest.playerId + " in " + guest.levelId);
    }

    private String uniqueName(String requested) {
        String base = (requested == null || requested.isBlank()) ? "Player" : requested.trim();
        String name = base;
        int suffix = 2;
        while (isNameTaken(name)) {
            name = base + " " + suffix++;
        }
        return name;
    }

    private boolean isNameTaken(String name) {
        for (Guest other : guests.values()) {
            if (other.isJoined() && name.equals(other.name)) return true;
        }
        return false;
    }

    private PlayerEntity createGuestPlayer(Guest guest, WorldManager world, float x, float y) {
        PlayerEntity player = game.createRemotePlayer(guest.playerId, world, x, y);
        player.setResolveRemoteAttacks(true);
        player.setRemoteHitHandler((hitPlayer, damage, knockbackX, knockbackY) -> {
            Packets.PlayerHit hit = new Packets.PlayerHit();
            hit.damage = damage;
            hit.knockbackX = knockbackX;
            hit.knockbackY = knockbackY;
            server.send(guest.connectionId, hit);
        });
        world.addGameObject(player);
        return player;
    }

    private void onGuestLeft(Guest guest) {
        guests.remove(guest.connectionId);
        if (!guest.isJoined()) return;

        System.out.println("HostSession: " + guest.name + " (player " + guest.playerId + ") left");
        guest.player.getWorld().removeGameObject(guest.player);
        game.removeRemotePlayer(guest.player);

        Packets.PlayerLeft left = new Packets.PlayerLeft();
        left.playerId = guest.playerId;
        for (Guest other : guests.values()) {
            if (other.isJoined()) {
                server.send(other.connectionId, left);
            }
        }
    }

    private void onGuestState(Guest guest, Packets.PlayerState state) {
        state.playerId = guest.playerId; // Never trust the client's claim of who it is

        // States from before a level change describe the old level; ignore them
        if (guest.levelId.equals(state.levelId)) {
            PlayerDataCodec.applyState(guest.player, state, guest.clock.toLocalTime(state.time));
        }

        // Relay to everyone else; they decide whether it's in their level
        for (Guest other : guests.values()) {
            if (other != guest && other.isJoined()) {
                server.send(other.connectionId, state);
            }
        }
    }

    private void onGuestLevelChange(Guest guest, Packets.LevelChange change) {
        LevelInstance target;
        try {
            target = game.getOrCreateInstance(change.levelId);
        } catch (Exception e) {
            System.err.println("HostSession: " + guest.name + " asked for unknown level " + change.levelId);
            server.kick(guest.connectionId);
            return;
        }

        WorldManager oldWorld = guest.player.getWorld();
        if (oldWorld != null) {
            oldWorld.removeGameObject(guest.player);
        }

        // Park the copy at the level's spawn until the guest's next state arrives
        com.badlogic.gdx.math.Vector2 spawn = target.getSpawnPosition(null);
        guest.player.getTransform().setPosition(spawn.x, spawn.y);
        guest.player.clearSnapshotBuffer();
        guest.player.setWorld(target.getWorld());
        target.getWorld().addGameObject(guest.player);

        guest.levelId = target.getLevelId();
        guest.epoch = change.epoch;
        sendLevelSnapshot(guest);

        System.out.println("HostSession: " + guest.name + " moved to " + guest.levelId);
    }

    private void onGuestAttack(Guest guest, Packets.AttackRequest attack) {
        guest.player.startRemoteAttack(attack.angle, attack.weaponId);
        LevelInstance hostLevel = game.getCurrentInstance();
        if (hostLevel != null && hostLevel.getLevelId().equals(guest.levelId)) {
            com.game.systems.audio.SoundSystem.getInstance().playSound(com.game.systems.audio.SoundRegistry.SWORD_SWING);
        }

        Packets.Effect effect = new Packets.Effect();
        effect.kind = Packets.Effect.PLAYER_ATTACK;
        effect.playerId = guest.playerId;
        effect.angle = attack.angle;
        effect.text = attack.weaponId;
        sendEffect(guest.levelId, effect, guest);
    }

    private void onGuestPickup(Guest guest, Packets.PickupRequest request) {
        Tracked entry = tracked.get(request.netId);
        if (entry == null || !(entry.obj instanceof ItemPickupEntity item)) return;
        if (!entry.levelId.equals(guest.levelId) || !item.isActive() || !item.canPickup()) return;

        Transform itemTransform = item.getComponent(Transform.class);
        if (itemTransform.getPosition().dst(guest.player.getTransform().getPosition()) > MAX_PICKUP_DISTANCE) {
            return;
        }

        ItemStack stack = item.getItemStack();
        item.onPickup();
        game.getWorldItemManager().removeItem(item); // Despawns it for everyone

        Packets.ItemGrant grant = new Packets.ItemGrant();
        grant.itemId = stack.getDefinition().getId();
        grant.quantity = stack.getQuantity();
        server.send(guest.connectionId, grant);
    }

    private void onGuestDrop(Guest guest, Packets.DropItem drop) {
        ItemDefinition def = ItemRegistry.get(drop.itemId);
        if (def == null || drop.quantity <= 0) return;
        game.getWorldItemManager().spawnItem(guest.levelId, new ItemStack(def, drop.quantity), drop.x, drop.y, DROPPED_ITEM_GRACE);
    }

    private void sendLevelSnapshot(Guest guest) {
        List<Packets.EntitySpawn> spawns = new ArrayList<>();
        for (Tracked entry : tracked.values()) {
            if (!entry.levelId.equals(guest.levelId) || !entry.obj.isActive()) continue;
            spawns.add(describe(entry.obj));
        }

        Packets.LevelSnapshot snapshot = new Packets.LevelSnapshot();
        snapshot.epoch = guest.epoch;
        snapshot.levelId = guest.levelId;
        snapshot.entities = spawns.toArray(new Packets.EntitySpawn[0]);
        server.send(guest.connectionId, snapshot);
    }

    // ========== Replication ==========

    @Override
    public void onInstanceCreated(LevelInstance instance) {
        String levelId = instance.getLevelId();
        for (GameObject obj : instance.getWorld().getGameObjects()) {
            if (ReplicatedEntities.isReplicated(obj)) {
                track(levelId, obj);
            }
        }
        instance.getWorld().addListener(new WorldReplicator(levelId));
    }

    @Override
    public void onInstanceDisposed(LevelInstance instance) {
        tracked.values().removeIf(entry -> entry.levelId.equals(instance.getLevelId()));
    }

    private void track(String levelId, GameObject obj) {
        if (obj.getNetId() == 0) {
            obj.setNetId(nextNetId++);
        } else {
            nextNetId = Math.max(nextNetId, obj.getNetId() + 1); // Keep IDs from an earlier session unique
        }
        tracked.put(obj.getNetId(), new Tracked(levelId, obj));

        if (obj instanceof EnemyEntity enemy) {
            enemy.setAttackStartListener((attacker, angle) -> {
                Packets.Effect effect = new Packets.Effect();
                effect.kind = Packets.Effect.ENEMY_ATTACK;
                effect.netId = attacker.getNetId();
                effect.angle = angle;
                broadcastEffect(levelId, effect);
            });
        }
    }

    private Packets.EntitySpawn describe(GameObject obj) {
        return obj instanceof ItemPickupEntity item
            ? ReplicatedEntities.describeItem(item)
            : ReplicatedEntities.describe(obj);
    }

    private void spawnForGuests(String levelId, GameObject obj) {
        if (disposed) return;
        track(levelId, obj);
        Packets.EntitySpawn spawn = describe(obj);
        for (Guest guest : guests.values()) {
            if (guest.isJoined() && levelId.equals(guest.levelId)) {
                spawn.epoch = guest.epoch;
                server.send(guest.connectionId, spawn);
            }
        }
    }

    private void despawnForGuests(String levelId, GameObject obj) {
        if (disposed || tracked.remove(obj.getNetId()) == null) return;
        Packets.EntityDespawn despawn = new Packets.EntityDespawn();
        despawn.netId = obj.getNetId();
        for (Guest guest : guests.values()) {
            if (guest.isJoined() && levelId.equals(guest.levelId)) {
                despawn.epoch = guest.epoch;
                server.send(guest.connectionId, despawn);
            }
        }
    }

    private class WorldReplicator implements WorldManager.Listener {
        private final String levelId;

        WorldReplicator(String levelId) {
            this.levelId = levelId;
        }

        @Override
        public void onObjectAdded(GameObject obj) {
            if (ReplicatedEntities.isReplicated(obj)) {
                spawnForGuests(levelId, obj);
            }
        }

        @Override
        public void onObjectRemoved(GameObject obj) {
            if (obj.getNetId() != 0 && ReplicatedEntities.isReplicated(obj)) {
                despawnForGuests(levelId, obj);
            }
        }
    }

    private class ItemReplicator implements WorldItemManager.Listener {
        @Override
        public void onItemSpawned(String levelId, ItemPickupEntity item) {
            spawnForGuests(levelId, item);
        }

        @Override
        public void onItemRemoved(String levelId, ItemPickupEntity item) {
            if (item.getNetId() != 0) {
                despawnForGuests(levelId, item);
            }
        }
    }

    // ========== Local player / effects ==========

    @Override
    public void onLocalPlayerCreated(PlayerEntity player) {
        player.setPlayerId(HOST_PLAYER_ID);
    }

    @Override
    public void onLocalLevelChanged(LevelInstance newInstance) {
        sendHostPlayerState(); // Let guests see the host leave/arrive right away
    }

    @Override
    public boolean isLevelOccupied(String levelId) {
        for (Guest guest : guests.values()) {
            if (guest.isJoined() && levelId.equals(guest.levelId)) return true;
        }
        return false;
    }

    @Override
    public void onLocalAttack(float attackAngle, String weaponId) {
        LevelInstance level = game.getCurrentInstance();
        if (level == null) return;

        Packets.Effect effect = new Packets.Effect();
        effect.kind = Packets.Effect.PLAYER_ATTACK;
        effect.playerId = HOST_PLAYER_ID;
        effect.angle = attackAngle;
        effect.text = weaponId;
        broadcastEffect(level.getLevelId(), effect);
    }

    @Override
    public boolean requestPickup(ItemPickupEntity item) {
        return false; // The host picks up directly
    }

    @Override
    public boolean dropItem(ItemStack stack, float x, float y) {
        return false; // The host spawns directly; replication follows automatically
    }

    @Override
    public void broadcastEffect(String levelId, Packets.Effect effect) {
        sendEffect(levelId, effect, null);
    }

    private void sendEffect(String levelId, Packets.Effect effect, Guest except) {
        for (Guest guest : guests.values()) {
            if (guest != except && guest.isJoined() && levelId.equals(guest.levelId)) {
                effect.epoch = guest.epoch;
                server.send(guest.connectionId, effect);
            }
        }
    }

    private LevelInstance findInstance(String levelId) {
        for (LevelInstance instance : game.getInstances()) {
            if (instance.getLevelId().equals(levelId)) return instance;
        }
        return null;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;

        game.getWorldItemManager().removeListener(itemListener);
        for (Guest guest : guests.values()) {
            if (guest.isJoined()) {
                guest.player.getWorld().removeGameObject(guest.player);
                game.removeRemotePlayer(guest.player);
            }
        }
        guests.clear();
        tracked.clear();
        server.stop();
    }
}
