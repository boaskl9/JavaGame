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
import com.game.systems.furniture.ChestEntity;
import com.game.systems.furniture.FurnitureEntity;
import com.game.systems.furniture.FurnitureFactory;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.game.systems.item.ItemStack;
import com.game.networking.identity.PlayerIdentity;
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
    private static final String CHARACTER_ID_PREFIX = "character:";
    private static final int MAX_NAME_LENGTH = 16;
    private static final float PLAYER_STATE_INTERVAL = 1f / 30f;
    private static final float ENTITY_STATE_INTERVAL = 1f / 20f;
    private static final float MAX_PICKUP_DISTANCE = 64f; // Generous: magnet range + the guest's copy lagging behind the guest
    private static final float DROPPED_ITEM_GRACE = 1.5f;
    private static final float MAX_FURNITURE_REACH = 48f; // Generous: the guest's copy lags behind the guest

    private final GameServer server;
    private final NetGameContext game;

    private final Map<Integer, Guest> guests = new HashMap<>(); // By connection ID
    private final Map<Integer, Tracked> tracked = new HashMap<>(); // By net ID
    private final Map<Integer, Integer> chestLocks = new HashMap<>(); // Chest net ID -> player ID using it
    private final WorldItemManager.Listener itemListener = new ItemReplicator();
    private int nextPlayerId = HOST_PLAYER_ID + 1;
    private int nextNetId = 1;

    private float playerStateTimer = 0f;
    private float entityStateTimer = 0f;
    private boolean disposed = false;

    private static final class Guest {
        final int connectionId;
        int playerId = -1;
        String identityKey; // Set by Hello; the guest is then picking a character
        String name;        // The character's name
        String characterId; // Where the character is saved
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

        boolean isChoosingCharacter() {
            return identityKey != null && !isJoined();
        }
    }

    private record Tracked(String levelId, GameObject obj) {
    }

    public HostSession(NetGameContext game) {
        this(game, GameServer.DEFAULT_PORT);
    }

    public HostSession(NetGameContext game, int port) {
        this.game = game;
        this.server = new GameServer(port);
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
        } else if (packet instanceof Packets.ChooseCharacter choice) {
            onChooseCharacter(guest, choice);
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
        } else if (packet instanceof Packets.PlaceFurnitureRequest place) {
            onGuestPlaceFurniture(guest, place);
        } else if (packet instanceof Packets.PickUpFurnitureRequest pickUp) {
            onGuestPickUpFurniture(guest, pickUp);
        } else if (packet instanceof Packets.ChestOpenRequest open) {
            onGuestOpenChest(guest, open);
        } else if (packet instanceof Packets.ChestContents contents) {
            ChestEntity chest = chestHeldBy(guest.playerId, contents.netId);
            if (chest != null) {
                FurnitureFactory.setContents(chest.getContainer(), contents.itemIds, contents.quantities);
            }
        } else if (packet instanceof Packets.ChestClose close) {
            chestLocks.remove(close.netId, guest.playerId);
        } else if (packet instanceof Packets.InventorySync sync) {
            PlayerData data = PlayerDataCodec.fromJson(sync.playerJson);
            if (data != null) {
                data.levelId = guest.levelId; // The host knows for sure which level they're in
                data.displayName = guest.name;
                data.lastPlayedBy = guest.identityKey;
                SaveManager.getInstance().putGuestData(guest.characterId, data);
            }
        }
    }

    // ========== Joining: pick or create a character ==========

    private void onHello(Guest guest, Packets.Hello hello) {
        if (guest.identityKey != null) return;

        boolean hasIdentity = hello.identityProvider != null && !hello.identityProvider.isBlank()
            && hello.identityId != null && !hello.identityId.isBlank();
        guest.identityKey = hasIdentity
            ? PlayerIdentity.key(hello.identityProvider, hello.identityId)
            : "anonymous:" + guest.connectionId;
        sendCharacterList(guest, null);
    }

    /**
     * Every character saved in this world, for a guest to pick from (theirs first, then by name).
     */
    private void sendCharacterList(Guest guest, String message) {
        List<Packets.CharacterInfo> characters = new ArrayList<>();
        for (Map.Entry<String, PlayerData> entry : SaveManager.getInstance().getGuestCharacters().entrySet()) {
            Packets.CharacterInfo info = new Packets.CharacterInfo();
            info.id = entry.getKey();
            info.name = characterName(entry.getKey(), entry.getValue());
            info.levelId = entry.getValue().levelId;
            info.inUse = isCharacterInUse(entry.getKey());
            info.lastPlayedByYou = guest.identityKey.equals(entry.getValue().lastPlayedBy);
            characters.add(info);
        }
        characters.sort(java.util.Comparator
            .comparing((Packets.CharacterInfo info) -> !info.lastPlayedByYou)
            .thenComparing(info -> info.name, String.CASE_INSENSITIVE_ORDER));

        Packets.CharacterList list = new Packets.CharacterList();
        list.characters = characters.toArray(new Packets.CharacterInfo[0]);
        list.message = message;
        server.send(guest.connectionId, list);
    }

    /** Characters became free or taken: refresh the lists of guests who are still choosing. */
    private void sendCharacterListsToChoosingGuests() {
        for (Guest guest : guests.values()) {
            if (guest.isChoosingCharacter()) {
                sendCharacterList(guest, null);
            }
        }
    }

    private void onChooseCharacter(Guest guest, Packets.ChooseCharacter choice) {
        if (!guest.isChoosingCharacter()) return;

        SaveManager saves = SaveManager.getInstance();
        String characterId;
        String name;
        PlayerData saved;
        if (choice.characterId != null) {
            saved = saves.getGuestData(choice.characterId);
            if (saved == null) {
                sendCharacterList(guest, "That character no longer exists.");
                return;
            }
            characterId = choice.characterId;
            name = characterName(characterId, saved);
            if (isCharacterInUse(characterId)) {
                sendCharacterList(guest, name + " is already being played.");
                return;
            }
        } else {
            name = choice.newName == null ? "" : choice.newName.trim();
            if (name.length() > MAX_NAME_LENGTH) {
                name = name.substring(0, MAX_NAME_LENGTH).trim();
            }
            if (name.isEmpty()) {
                sendCharacterList(guest, "Enter a name for your character.");
                return;
            }
            if (isCharacterNameTaken(name)) {
                sendCharacterList(guest, "There is already a character named " + name + ".");
                return;
            }
            characterId = CHARACTER_ID_PREFIX + java.util.UUID.randomUUID();
            saved = null;
        }

        joinAsCharacter(guest, characterId, name, saved);
        sendCharacterListsToChoosingGuests(); // This character is now in use
    }

    private void joinAsCharacter(Guest guest, String characterId, String name, PlayerData saved) {
        guest.playerId = nextPlayerId++;
        guest.characterId = characterId;
        guest.name = name;

        // Continue where they left off if possible; otherwise join the host
        LevelInstance level = null;
        com.badlogic.gdx.math.Vector2 position = null;
        if (saved != null && saved.levelId != null) {
            level = tryGetShareableLevel(saved.levelId);
            if (level != null) {
                position = isStandable(level, saved.x, saved.y)
                    ? new com.badlogic.gdx.math.Vector2(saved.x, saved.y)
                    : level.getSpawnPosition(null);
            }
        }
        if (level == null) {
            level = game.getCurrentInstance();
            PlayerEntity host = game.getLocalPlayer();
            if (level != null && level.isShareable() && host != null) {
                position = new com.badlogic.gdx.math.Vector2(host.getTransform().getX(), host.getTransform().getY());
            } else {
                // Generated dungeons can't be shared yet
                level = game.getOrCreateInstance(com.game.world.GameWorld.START_LEVEL);
                position = level.getSpawnPosition(null);
            }
        }

        guest.levelId = level.getLevelId();
        guest.epoch = 1;
        guest.player = createGuestPlayer(guest, level.getWorld(), position.x, position.y);

        Packets.Welcome welcome = new Packets.Welcome();
        welcome.playerId = guest.playerId;
        welcome.playerName = guest.name;
        welcome.levelId = guest.levelId;
        welcome.x = position.x;
        welcome.y = position.y;
        welcome.savedPlayerJson = PlayerDataCodec.toJson(saved);
        server.send(guest.connectionId, welcome);
        sendLevelSnapshot(guest);

        recordCharacter(guest); // A new character is listed (and its name taken) right away

        System.out.println("HostSession: " + guest.name + " [" + guest.characterId + "] joined as player "
            + guest.playerId + " in " + guest.levelId + (saved != null ? " (returning)" : " (new character)"));
    }

    /**
     * A character's name. Characters saved before names were stored are keyed by the name itself
     * (or "name:Bob").
     */
    private static String characterName(String characterId, PlayerData data) {
        if (data.displayName != null && !data.displayName.isBlank()) {
            return data.displayName;
        }
        return characterId.substring(characterId.lastIndexOf(':') + 1);
    }

    private boolean isCharacterInUse(String characterId) {
        for (Guest other : guests.values()) {
            if (other.isJoined() && characterId.equals(other.characterId)) return true;
        }
        return false;
    }

    private boolean isCharacterNameTaken(String name) {
        for (Map.Entry<String, PlayerData> entry : SaveManager.getInstance().getGuestCharacters().entrySet()) {
            if (characterName(entry.getKey(), entry.getValue()).equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private LevelInstance tryGetShareableLevel(String levelId) {
        try {
            LevelInstance level = game.getOrCreateInstance(levelId);
            return level.isShareable() ? level : null;
        } catch (Exception e) {
            System.err.println("HostSession: Saved level " + levelId + " can't be loaded: " + e.getMessage());
            return null;
        }
    }

    /** Whether a player's feet fit at this position (matches PlayerEntity's environment collider). */
    private static boolean isStandable(LevelInstance level, float x, float y) {
        return level.getWorld().isPositionWalkable(x + 4, y, 8, 4);
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
        recordCharacter(guest);
        releaseChestsHeldBy(guest.playerId);
        guest.player.getWorld().removeGameObject(guest.player);
        game.removeRemotePlayer(guest.player);

        Packets.PlayerLeft left = new Packets.PlayerLeft();
        left.playerId = guest.playerId;
        for (Guest other : guests.values()) {
            if (other.isJoined()) {
                server.send(other.connectionId, left);
            }
        }
        sendCharacterListsToChoosingGuests(); // Their character is free again
    }

    /**
     * Record the guest's character: name, who played it, latest level and position
     * (inventory comes from their last sync). A guest who left while knocked out is recorded
     * as already respawned: at the start level's spawn with full health.
     */
    private void recordCharacter(Guest guest) {
        SaveManager saves = SaveManager.getInstance();
        PlayerData data = saves.getGuestData(guest.characterId);
        if (data == null) {
            data = new PlayerData();
            data.maxHealth = guest.player.getMaxHealth();
            data.currentHealth = guest.player.getHealth();
        }
        if (guest.player.isAlive()) {
            data.levelId = guest.levelId;
            data.x = guest.player.getTransform().getX();
            data.y = guest.player.getTransform().getY();
        } else {
            LevelInstance home = game.getOrCreateInstance(com.game.world.GameWorld.START_LEVEL);
            com.badlogic.gdx.math.Vector2 spawn = home.getSpawnPosition(null);
            data.levelId = home.getLevelId();
            data.x = spawn.x;
            data.y = spawn.y;
            data.currentHealth = data.maxHealth > 0 ? data.maxHealth : guest.player.getMaxHealth();
        }
        data.displayName = guest.name;
        data.lastPlayedBy = guest.identityKey;
        saves.putGuestData(guest.characterId, data);
    }

    private void onGuestState(Guest guest, Packets.PlayerState state) {
        state.playerId = guest.playerId; // Never trust the client's claim of who it is

        // States from before a level change describe the old level; ignore them
        if (guest.levelId.equals(state.levelId)) {
            boolean died = PlayerDataCodec.applyState(guest.player, state, guest.clock.toLocalTime(state.time));
            LevelInstance hostLevel = game.getCurrentInstance();
            if (died && hostLevel != null && hostLevel.getLevelId().equals(guest.levelId)) {
                game.showDeathAnimation(state.x, state.y);
            }
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

        releaseChestsHeldBy(guest.playerId); // Chests stay behind in the old level

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

    // ========== Furniture ==========

    private void onGuestPlaceFurniture(Guest guest, Packets.PlaceFurnitureRequest request) {
        FurnitureEntity placed = game.placeFurniture(guest.levelId, request.itemId, request.x, request.y);

        Packets.PlaceFurnitureResult result = new Packets.PlaceFurnitureResult();
        result.requestId = request.requestId;
        result.placed = placed != null;
        server.send(guest.connectionId, result);
    }

    private void onGuestPickUpFurniture(Guest guest, Packets.PickUpFurnitureRequest request) {
        FurnitureEntity furniture = furnitureNearGuest(guest, request.netId);
        if (furniture == null || !furniture.canPickup() || chestLocks.containsKey(request.netId)) {
            return;
        }

        game.removeFurniture(guest.levelId, furniture); // Despawns it for everyone

        Packets.ItemGrant grant = new Packets.ItemGrant();
        grant.itemId = furniture.getItemId();
        grant.quantity = 1;
        server.send(guest.connectionId, grant);
    }

    private void onGuestOpenChest(Guest guest, Packets.ChestOpenRequest request) {
        Packets.ChestOpenResult result = new Packets.ChestOpenResult();
        result.netId = request.netId;

        FurnitureEntity furniture = furnitureNearGuest(guest, request.netId);
        Integer holder = chestLocks.get(request.netId);
        if (furniture instanceof ChestEntity chest && (holder == null || holder == guest.playerId)) {
            chestLocks.put(request.netId, guest.playerId);
            result.granted = true;
            result.itemIds = FurnitureFactory.contentIds(chest.getContainer());
            result.quantities = FurnitureFactory.contentQuantities(chest.getContainer());
        }
        server.send(guest.connectionId, result);
    }

    /** Furniture in the guest's level within reach of the guest's copy, or null. */
    private FurnitureEntity furnitureNearGuest(Guest guest, int netId) {
        Tracked entry = tracked.get(netId);
        if (entry == null || !entry.levelId.equals(guest.levelId) || !(entry.obj instanceof FurnitureEntity furniture)) {
            return null;
        }
        float distance = furniture.getTransform().getPosition().dst(guest.player.getTransform().getPosition());
        return distance <= MAX_FURNITURE_REACH ? furniture : null;
    }

    private ChestEntity chestHeldBy(int playerId, int netId) {
        Integer holder = chestLocks.get(netId);
        Tracked entry = tracked.get(netId);
        return holder != null && holder == playerId && entry != null && entry.obj instanceof ChestEntity chest ? chest : null;
    }

    private void releaseChestsHeldBy(int playerId) {
        chestLocks.values().removeIf(holder -> holder == playerId);
    }

    @Override
    public boolean placeFurniture(String itemId, float x, float y, java.util.function.Consumer<Boolean> onResult) {
        return false; // The host places directly; replication follows automatically
    }

    @Override
    public boolean pickUpFurniture(FurnitureEntity furniture) {
        Integer holder = chestLocks.get(furniture.getNetId());
        return holder != null && holder != HOST_PLAYER_ID; // Refuse while a guest is using it
    }

    @Override
    public void openChest(ChestEntity chest, java.util.function.Consumer<Boolean> onResult) {
        Integer holder = chestLocks.get(chest.getNetId());
        if (holder != null && holder != HOST_PLAYER_ID) {
            onResult.accept(false);
            return;
        }
        chestLocks.put(chest.getNetId(), HOST_PLAYER_ID);
        onResult.accept(true);
    }

    @Override
    public void closeChest(ChestEntity chest) {
        chestLocks.remove(chest.getNetId(), HOST_PLAYER_ID);
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
        chestLocks.remove(obj.getNetId());
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
                recordCharacter(guest); // So the host's save has everyone's latest location
                guest.player.getWorld().removeGameObject(guest.player);
                game.removeRemotePlayer(guest.player);
            }
        }
        guests.clear();
        tracked.clear();
        chestLocks.clear();
        server.stop();
    }
}
