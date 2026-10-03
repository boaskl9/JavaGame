package com.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.components.ColliderComponent;
import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.networking.ClientSession;
import com.game.networking.GameClient;
import com.game.networking.GameServer;
import com.game.networking.HostSession;
import com.game.networking.NetGameContext;
import com.game.networking.NetSession;
import com.game.networking.Packets;
import com.game.networking.PlayerDataCodec;
import com.game.systems.audio.SoundSystem;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.PlayerManager;
import com.game.systems.entity.Transform;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.GatewayEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.entities.ProjectileEntity;
import com.game.systems.furniture.ChestEntity;
import com.game.systems.furniture.FurnitureEntity;
import com.game.systems.furniture.FurnitureFactory;
import com.game.systems.furniture.FurnitureManager;
import com.game.systems.input.InputSource;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;
import com.game.systems.dungeon.DungeonLevelSource;
import com.game.systems.dungeon.assembly.DungeonPopulator;
import com.game.systems.level.LevelSource;
import com.game.systems.level.TiledMapLevelSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.game.systems.audio.SoundRegistry.COIN_PICKUP;

/**
 * The game simulation seen from one machine, without any rendering or UI:
 * loaded levels, the local player, other players' copies, item pickups, gateways
 * and the multiplayer session.
 *
 * GameScreen owns one of these and draws it; tests can run it directly, headless.
 * Anything visual or UI-related is delegated to a {@link Presenter}.
 */
public class GameWorld implements NetGameContext {
    /** Where new games start and where knocked-out players wake up (until players have a home). */
    public static final String START_LEVEL = "Maps/prototype.tmx";
    /** How long a knocked-out player stays down before respawning. */
    public static final float RESPAWN_DELAY = 2f;
    private static final float PICKUP_RADIUS = 16f;

    /**
     * Everything the simulation needs from the screen (or from a test).
     */
    public interface Presenter {
        /** Input for the local player (keyboard in the game; null or scripted in tests). */
        InputSource createLocalInput(PlayerEntity player);

        /** The local player now stands in this level (rebuild map renderers etc.). */
        void onLevelEntered(LevelInstance instance);

        /** Called before the local player leaves the current level (close level-bound UI). */
        void onLeavingLevel();

        /** The local player exists for the first time (build the UI). */
        void onLocalPlayerCreated(PlayerEntity player);

        /** The local player's inventory/health were replaced from saved data. */
        void onLocalPlayerDataRestored(PlayerEntity player);

        void onInventoryChanged();

        void showDamageNumber(float x, float y, int amount);

        void showDeathAnimation(float x, float y);

        void showParticles(float x, float y, String particleType);

        void onConnectionLost(String reason);

        /** The local player was knocked out; they respawn after {@link #RESPAWN_DELAY} seconds. */
        void onLocalPlayerDied();

        /** The local player woke up at the start level with full health. */
        void onLocalPlayerRespawned();

        /** Tell the local player something (e.g. "Day 2", or why a door won't open). */
        void showMessage(String text);

        /**
         * Joining a host: show its characters. The player answers with {@link #playCharacter}
         * or {@link #createCharacter}. Called again when the list changes or a choice was refused.
         * @param message why the previous choice was refused, or null
         */
        void chooseCharacter(Packets.CharacterInfo[] characters, String message);
    }

    // Levels loaded on this machine, by level ID. The host keeps every level that has a player
    // in it simulating; single-player and clients only simulate the current one.
    private final Map<String, LevelInstance> instances = new LinkedHashMap<>();
    private final WorldItemManager worldItemManager;
    private final PlayerManager playerManager;
    private final Presenter presenter;
    private final boolean clientMode; // Joined someone else's game: levels are replicas filled by the host

    private LevelInstance currentInstance;
    private PlayerEntity localPlayer;
    private NetSession session; // Null in single-player
    private GatewayEntity pendingGateway;
    private float respawnTimer = -1f; // Counts down while the local player is knocked out
    private GatewayEntity ignoredGateway; // The gateway we arrived on (or were refused at): ignored until we step off it

    // Days
    private final DayCycle dayCycle = new DayCycle(new java.util.Random().nextLong());
    private boolean sleeping = false;
    private int playersAsleep = 0;
    private int playersTotal = 1;

    // Dungeons
    private final java.util.Set<String> exhaustedDungeons = new java.util.HashSet<>(); // "dungeon:cave": died there today
    private String dungeonReturnLevel; // Where the dungeon's exit leads
    private String dungeonReturnSpawn;

    public GameWorld(boolean clientMode, WorldItemManager worldItemManager, PlayerManager playerManager, Presenter presenter) {
        this.clientMode = clientMode;
        this.worldItemManager = worldItemManager;
        this.playerManager = playerManager;
        this.presenter = presenter;
        if (!clientMode) {
            com.game.save.SaveManager.getInstance().setDayCycle(dayCycle); // The host's clock is the one saved
        }
    }

    // ========== Per-frame ==========

    /**
     * Handle network traffic. Call once per frame, before {@link #update}.
     */
    public void updateNetwork(float delta) {
        if (session != null) {
            session.update(delta);
        }
    }

    /**
     * Advance the simulation: pending level change, levels, item pickups, gateways.
     */
    public void update(float delta) {
        if (currentInstance == null) return;

        // Guests' clocks run too (for display), but only the host ends the day
        if (dayCycle.advance(delta) && !clientMode) {
            endDay(true);
        }

        if (isLocalPlayerDead()) {
            respawnTimer -= delta;
            if (respawnTimer <= 0f) {
                respawnLocalPlayer();
            }
        }

        if (pendingGateway != null) {
            GatewayEntity gateway = pendingGateway;
            pendingGateway = null;
            useGateway(gateway);
        }

        updateLevels(delta);
        updateItemMagnetism();
        checkItemPickups();
        checkGatewayCollisions();
    }

    /**
     * Simulate the current level, plus (on the host) every level a guest is in.
     */
    private void updateLevels(float delta) {
        for (LevelInstance instance : new ArrayList<>(instances.values())) {
            boolean occupied = instance == currentInstance
                || (session != null && session.isLevelOccupied(instance.getLevelId()));
            if (occupied) {
                instance.update(delta, worldItemManager);
            }
        }
    }

    /**
     * Registers nearby items with the magnets of players in this level.
     * Purely cosmetic for remote players; only the local player can actually pick items up.
     */
    private void updateItemMagnetism() {
        for (GameObject obj : currentInstance.getWorld().getGameObjects()) {
            if (!(obj instanceof PlayerEntity player)) continue;

            Vector2 playerPos = player.getTransform().getPosition();
            float magnetRadius = player.getItemMagnet().getMagnetRadius();

            for (ItemPickupEntity item : worldItemManager.getItemsNear(playerPos, magnetRadius)) {
                player.getItemMagnet().registerItem(item);
            }
        }
    }

    /**
     * Checks whether the local player is touching any items.
     * Guests ask the host for the item; the host and single-player pick it up directly.
     */
    private void checkItemPickups() {
        if (localPlayer == null || isLocalPlayerDead()) return;

        boolean inventoryChanged = false;
        Vector2 playerPos = localPlayer.getTransform().getPosition();

        for (ItemPickupEntity item : worldItemManager.getAllItems()) {
            if (!item.canPickup() || !item.isActive()) continue;

            Transform itemTransform = item.getComponent(Transform.class);
            if (itemTransform == null || playerPos.dst(itemTransform.getPosition()) >= PICKUP_RADIUS) continue;

            if (session != null && session.requestPickup(item)) {
                continue;
            }

            ItemStack itemStack = item.getItemStack();
            ItemStack remaining = localPlayer.getInventory().addItem(itemStack);

            if (remaining == null) {
                // All picked up
                item.onPickup();
                worldItemManager.removeItem(item);
                SoundSystem.getInstance().playSound(COIN_PICKUP, 0.6f);
                inventoryChanged = true;
            } else if (remaining.getQuantity() < itemStack.getQuantity()) {
                // Partial pickup
                item.getItemStack().setQuantity(remaining.getQuantity());
                SoundSystem.getInstance().playSound(COIN_PICKUP, 0.6f);
                inventoryChanged = true;
            }
        }

        if (inventoryChanged) {
            presenter.onInventoryChanged();
        }
    }

    private void checkGatewayCollisions() {
        if (localPlayer == null || isLocalPlayerDead() || sleeping) return;

        GatewayEntity touching = gatewayUnderPlayer();
        if (touching == null) {
            ignoredGateway = null; // Stepped off: it works again
        } else if (touching != ignoredGateway) {
            pendingGateway = touching;
        }
    }

    /** The gateway the local player is standing on, or null. */
    private GatewayEntity gatewayUnderPlayer() {
        ColliderComponent playerCollider = localPlayer.getComponent(ColliderComponent.class);
        if (playerCollider == null) return null;
        Rectangle playerBounds = playerCollider.getBounds(localPlayer);

        for (GameObject obj : currentInstance.getWorld().getGameObjects()) {
            if (obj instanceof GatewayEntity gateway) {
                ColliderComponent gatewayCollider = gateway.getComponent(ColliderComponent.class);
                if (gatewayCollider != null && playerBounds.overlaps(gatewayCollider.getBounds(gateway))) {
                    return gateway;
                }
            }
        }
        return null;
    }

    /**
     * Gateway targets: a level ID, "dungeon:<theme>" (today's dungeon of that theme; the gateway's
     * targetSpawn is where its exit brings you back to), or "dungeon:exit".
     */
    private void useGateway(GatewayEntity gateway) {
        String target = gateway.getTargetLevel();
        if (DungeonPopulator.EXIT_TARGET.equals(target)) {
            leaveDungeon();
        } else if (target.startsWith(DungeonLevelSource.ID_PREFIX)) {
            if (!enterDungeon(target, gateway.getTargetSpawn())) {
                ignoredGateway = gateway; // Don't ask again until they step off
            }
        } else {
            changeLevel(target, gateway.getTargetSpawn());
        }
    }

    // ========== Death and respawn ==========

    /**
     * The local player's health reached zero. No penalty: they stay down for a moment,
     * then wake up at the start level with full health. Each player respawns on their own machine.
     */
    private void onLocalPlayerDied() {
        if (isLocalPlayerDead()) return;
        respawnTimer = RESPAWN_DELAY;
        pendingGateway = null;

        // Dying in a dungeon kicks you out for the rest of the day
        String dungeon = dungeonKeyOf(currentInstance.getLevelId());
        if (dungeon != null) {
            exhaustedDungeons.add(dungeon);
        }

        Vector2 at = localPlayer.getTransform().getPosition();
        presenter.showDeathAnimation(at.x, at.y);
        SoundSystem.getInstance().playSound(com.game.systems.audio.SoundRegistry.ENEMY_DEATH, 0.9f);
        presenter.onLocalPlayerDied();
        System.out.println("GameWorld: Local player was knocked out");
    }

    /** Whether the local player is knocked out and waiting to respawn. */
    public boolean isLocalPlayerDead() {
        return respawnTimer >= 0f;
    }

    /**
     * Respawn right away if the local player is knocked out (e.g. before saving or leaving),
     * so a save never holds a dead player.
     */
    public void respawnIfDead() {
        if (isLocalPlayerDead()) {
            respawnLocalPlayer();
        }
    }

    private void respawnLocalPlayer() {
        respawnTimer = -1f;
        boolean leftDungeon = dungeonKeyOf(currentInstance.getLevelId()) != null;
        localPlayer.heal(localPlayer.getMaxHealth());
        goHome();

        presenter.onLocalPlayerRespawned();
        if (leftDungeon) {
            presenter.showMessage("You were carried out of the dungeon.\nYou can't go back in today.");
        }
        System.out.println("GameWorld: Local player respawned at " + START_LEVEL);
    }

    /** Put the local player at the start level's spawn point. */
    private void goHome() {
        dungeonReturnLevel = null;
        dungeonReturnSpawn = null;
        if (currentInstance.getLevelId().equals(START_LEVEL)) {
            // Same level: just move (re-entering would make a client ask the host for the level again)
            Vector2 spawn = currentInstance.getSpawnPosition(null);
            localPlayer.getTransform().setPosition(spawn.x, spawn.y);
            ignoredGateway = gatewayUnderPlayer();
        } else {
            changeLevel(START_LEVEL, null);
        }
    }

    // ========== Days ==========

    @Override
    public DayCycle getDayCycle() {
        return dayCycle;
    }

    /**
     * Go to bed. Single-player: the next day starts right away. Multiplayer: it starts once every
     * player is asleep; until then the player can't move ({@link #wakeUp} to get up again).
     */
    public void sleep() {
        if (localPlayer == null || sleeping || isLocalPlayerDead()) return;
        setSleeping(true);
        pendingGateway = null;
        if (session == null) {
            endDay(false);
        } else {
            session.onLocalSleepChanged(true);
        }
    }

    /** Get out of bed before the day ends. */
    public void wakeUp() {
        if (!sleeping) return;
        setSleeping(false);
        if (session != null) {
            session.onLocalSleepChanged(false);
        }
    }

    private void setSleeping(boolean asleep) {
        sleeping = asleep;
        if (localPlayer != null) {
            localPlayer.setFrozen(asleep);
        }
    }

    @Override
    public boolean isLocalPlayerSleeping() {
        return sleeping;
    }

    public int getPlayersAsleep() {
        return playersAsleep;
    }

    public int getPlayersTotal() {
        return playersTotal;
    }

    @Override
    public void onSleepStatus(int asleep, int total) {
        playersAsleep = asleep;
        playersTotal = total;
    }

    /**
     * Interact with the level itself (not furniture): lie down in a bed ("bed" objects in the
     * level's Entities layer), or get up again.
     * @return true if something was used
     */
    public boolean interactWithLevel() {
        if (localPlayer == null || currentInstance == null || isLocalPlayerDead()) return false;
        if (sleeping) {
            wakeUp();
            return true;
        }
        ColliderComponent playerCollider = localPlayer.getComponent(ColliderComponent.class);
        if (playerCollider == null) return false;
        Rectangle playerBounds = playerCollider.getBounds(localPlayer);
        for (com.game.systems.level.LevelData.LevelObject bed : currentInstance.getLevelData().getObjectsByType("bed")) {
            Rectangle bedBounds = new Rectangle(bed.getX(), bed.getY(), Math.max(bed.getWidth(), 16), Math.max(bed.getHeight(), 16));
            if (playerBounds.overlaps(bedBounds)) {
                sleep();
                return true;
            }
        }
        return false;
    }

    /**
     * The day is over (everyone slept, or time ran out): start the next one. Host / single-player.
     */
    @Override
    public void endDay(boolean passedOut) {
        if (clientMode) return;
        dayCycle.startNextDay();
        if (session != null) {
            session.onDayEnded(passedOut);
        }
        startNewDayLocally(passedOut);
    }

    @Override
    public void onNewDay(int day, boolean passedOut) {
        dayCycle.set(day, 0f, dayCycle.getDayLength(), dayCycle.getWorldSeed());
        startNewDayLocally(passedOut);
    }

    /**
     * Morning: wake up at home with full health; yesterday's dungeon lockouts are gone.
     */
    private void startNewDayLocally(boolean passedOut) {
        System.out.println("GameWorld: Day " + dayCycle.getDay() + (passedOut ? " (passed out)" : ""));
        respawnTimer = -1f;
        setSleeping(false);
        playersAsleep = 0;
        exhaustedDungeons.clear();
        pendingGateway = null;
        if (localPlayer == null || currentInstance == null) return;

        localPlayer.heal(localPlayer.getMaxHealth());
        goHome();
        releaseOldDungeons();
        presenter.showMessage(passedOut
            ? "You passed out from exhaustion...\nDay " + dayCycle.getDay()
            : "Day " + dayCycle.getDay());
    }

    // ========== Dungeons ==========

    /**
     * Enter today's dungeon of a theme. Its layout and enemies are the same for everyone all day.
     * @param dungeonKey "dungeon:<theme>"
     * @param returnSpawn spawn point in the current level that the dungeon's exit leads back to
     * @return false if the player can't go in (they died there today)
     */
    public boolean enterDungeon(String dungeonKey, String returnSpawn) {
        if (exhaustedDungeons.contains(dungeonKey)) {
            presenter.showMessage("You're too worn out to go back in there today.");
            return false;
        }
        String returnLevel = currentInstance.getLevelId();
        changeLevel(dungeonKey + ":" + dayCycle.getDay(), null);
        dungeonReturnLevel = returnLevel;
        dungeonReturnSpawn = returnSpawn;
        return true;
    }

    /** Take the dungeon's exit: back to where the player came in (the start level if unknown). */
    public void leaveDungeon() {
        String level = dungeonReturnLevel != null ? dungeonReturnLevel : START_LEVEL;
        String spawn = dungeonReturnLevel != null ? dungeonReturnSpawn : null;
        dungeonReturnLevel = null;
        dungeonReturnSpawn = null;
        changeLevel(level, spawn);
    }

    /** Whether the local player died in this dungeon today ("dungeon:<theme>"). */
    public boolean isExhausted(String dungeonKey) {
        return exhaustedDungeons.contains(dungeonKey);
    }

    /** "dungeon:cave" for a daily dungeon's level ID ("dungeon:cave:3"), else null. */
    public static String dungeonKeyOf(String levelId) {
        if (levelId == null || !levelId.startsWith(DungeonLevelSource.ID_PREFIX)) return null;
        int lastColon = levelId.lastIndexOf(':');
        return lastColon > DungeonLevelSource.ID_PREFIX.length() ? levelId.substring(0, lastColon) : null;
    }

    /**
     * Build the source for a level ID: a Tiled map path, or a daily dungeon "dungeon:<theme>:<day>",
     * generated from the world seed so every machine builds the same one.
     */
    private LevelSource levelSourceFor(String levelId) {
        String dungeonKey = dungeonKeyOf(levelId);
        if (dungeonKey != null) {
            String theme = dungeonKey.substring(DungeonLevelSource.ID_PREFIX.length());
            int day = Integer.parseInt(levelId.substring(levelId.lastIndexOf(':') + 1));
            if (day != dayCycle.getDay()) {
                throw new IllegalArgumentException(levelId + " is from another day (today is day " + dayCycle.getDay() + ")");
            }
            return DungeonLevelSource.generate(levelId, theme, dayCycle.seedFor(theme, day));
        }
        return new TiledMapLevelSource(levelId);
    }

    @Override
    public void releaseLevelIfUnused(String levelId) {
        LevelInstance instance = instances.get(levelId);
        if (instance != null && instance != currentInstance) {
            releaseInstanceIfUnused(instance);
        }
    }

    // ========== Level management ==========

    /**
     * Move the local player to a Tiled map level (loading it if needed).
     */
    public void changeLevel(String levelPath, String spawnPointName) {
        LevelInstance target = instances.get(levelPath);
        if (target == null) {
            target = createInstance(levelSourceFor(levelPath));
        }
        enterLevel(target, target.getSpawnPosition(spawnPointName));
    }

    /**
     * Build a brand-new level from a source (e.g. a generated dungeon) and move the local player there.
     */
    public void enterNewLevel(LevelSource source) {
        LevelInstance instance = createInstance(source);
        enterLevel(instance, instance.getSpawnPosition(null));
    }

    /**
     * Build a level instance on this machine and register it.
     */
    private LevelInstance createInstance(LevelSource source) {
        LevelInstanceFactory.Mode mode = clientMode ? LevelInstanceFactory.Mode.REPLICA : LevelInstanceFactory.Mode.AUTHORITATIVE;
        LevelInstance instance = LevelInstanceFactory.create(source, mode, new EntityDecorator(source.getLevelName()));

        LevelInstance replaced = instances.put(instance.getLevelId(), instance);
        if (replaced != null && replaced != currentInstance) {
            if (session != null) {
                session.onInstanceDisposed(replaced);
            }
            replaced.dispose();
        }

        if (session != null) {
            session.onInstanceCreated(instance);
        }
        return instance;
    }

    /**
     * Move the local player into a level at a position. Creates the local player the first time.
     */
    public void enterLevel(LevelInstance target, Vector2 position) {
        System.out.println("Entering level: " + target.getLevelId() + " at (" + position.x + ", " + position.y + ")");

        LevelInstance previous = currentInstance;
        if (previous != null) {
            presenter.onLeavingLevel();
            if (localPlayer != null) {
                previous.getWorld().removeGameObject(localPlayer);
            }
        }

        currentInstance = target;
        worldItemManager.setCurrentLevel(target.getLevelId());
        String levelType = target.getSource().isDungeon() ? "dungeon" : "tiled_map";
        com.game.save.SaveManager.getInstance().setCurrentLevel(target.getLevelId(), levelType);
        presenter.onLevelEntered(target);

        if (localPlayer == null) {
            createLocalPlayer(position.x, position.y);
        } else {
            localPlayer.setWorld(target.getWorld());
            localPlayer.getTransform().setPosition(position.x, position.y);
            target.getWorld().addGameObject(localPlayer);
        }

        if (previous != null && previous != target) {
            releaseInstanceIfUnused(previous);
        }

        ignoredGateway = gatewayUnderPlayer();

        if (session != null) {
            session.onLocalLevelChanged(target);
        }
    }

    /**
     * Unload a level the local player just left, unless it should be kept.
     * Clients only keep their current level. The host and single-player keep Tiled maps
     * (so revisiting preserves broken pots etc.) and drop generated dungeons nobody is in.
     */
    private void releaseInstanceIfUnused(LevelInstance instance) {
        String levelId = instance.getLevelId();
        boolean keep = !clientMode && (!instance.getSource().isDungeon()
            || isTodaysDungeon(levelId) // Monsters stay dead (and loot stays put) until tomorrow
            || (session != null && session.isLevelOccupied(levelId)));
        if (keep) return;

        if (instances.get(levelId) == instance) {
            instances.remove(levelId);
            worldItemManager.clearLevel(levelId); // Replica items, or loot in a dungeon that's gone
        }
        if (session != null) {
            session.onInstanceDisposed(instance);
        }
        instance.dispose();
    }

    private boolean isTodaysDungeon(String levelId) {
        return dungeonKeyOf(levelId) != null && levelId.equals(dungeonKeyOf(levelId) + ":" + dayCycle.getDay());
    }

    /**
     * A new day: unload the previous days' dungeons (unless a guest is still in one; they leave soon
     * and releaseLevelIfUnused unloads it then).
     */
    private void releaseOldDungeons() {
        for (LevelInstance instance : new ArrayList<>(instances.values())) {
            if (instance != currentInstance && dungeonKeyOf(instance.getLevelId()) != null) {
                releaseInstanceIfUnused(instance);
            }
        }
    }

    /**
     * Dispose every level instance (the local player is detached and re-added on the next enterLevel).
     */
    public void unloadAllLevels() {
        if (currentInstance != null && localPlayer != null) {
            currentInstance.getWorld().removeGameObject(localPlayer);
        }
        for (LevelInstance instance : instances.values()) {
            instance.dispose();
        }
        instances.clear();
        currentInstance = null;
    }

    /**
     * Create the player controlled on this machine, in the current world.
     */
    private void createLocalPlayer(float x, float y) {
        WorldManager world = currentInstance.getWorld();
        PlayerEntity player = new PlayerEntity(world, x, y);
        player.setInputSource(presenter.createLocalInput(player));

        player.setDamageNumberCallback((dx, dy, damage) -> emitDamageNumber(levelIdOf(player.getWorld()), dx, dy, damage));
        player.setDeathListener(this::onLocalPlayerDied);
        player.setAttackListener((angle, weaponId) -> {
            if (session != null) {
                session.onLocalAttack(angle, weaponId);
            }
        });

        playerManager.addPlayer(player);
        localPlayer = player;
        if (session != null) {
            session.onLocalPlayerCreated(player);
        }
        world.addGameObject(player);
        presenter.onLocalPlayerCreated(player);
    }

    /**
     * Drop an item at a position: guests ask the host, everyone else spawns it directly.
     */
    public void dropItem(ItemStack stack, float x, float y) {
        if (session == null || !session.dropItem(stack, x, y)) {
            worldItemManager.spawnItem(stack, x, y, 0f);
        }
    }

    // ========== Furniture ==========

    /**
     * Place furniture as the local player (the item is used up by the caller only when onResult(true)).
     * Guests ask the host, so the result may arrive a few frames later.
     */
    public void requestPlaceFurniture(String itemId, float x, float y, Consumer<Boolean> onResult) {
        if (session != null && session.placeFurniture(itemId, x, y, onResult)) {
            return;
        }
        onResult.accept(currentInstance != null && placeFurniture(currentInstance.getLevelId(), itemId, x, y) != null);
    }

    /**
     * Pick furniture up into the local player's inventory (only empty chests can be picked up).
     * @return true if it was picked up (or, for a guest, requested from the host)
     */
    public boolean pickUpFurniture(FurnitureEntity furniture) {
        if (localPlayer == null || currentInstance == null || !furniture.canPickup()) {
            return false;
        }
        if (session != null && session.pickUpFurniture(furniture)) {
            return isGuest(); // Guest: requested from the host. Host: refused (someone is using it)
        }

        ItemStack item = ItemFactory.create(furniture.getItemId(), 1);
        if (item == null || !localPlayer.getInventory().hasSpace(item)) {
            return false;
        }
        localPlayer.getInventory().addItem(item);
        removeFurniture(currentInstance.getLevelId(), furniture);
        presenter.onInventoryChanged();
        return true;
    }

    /** Ask to use a chest; onResult(true) once the local player may (one player at a time in multiplayer). */
    public void openChest(ChestEntity chest, Consumer<Boolean> onResult) {
        if (session == null) {
            onResult.accept(true);
        } else {
            session.openChest(chest, onResult);
        }
    }

    public void closeChest(ChestEntity chest) {
        if (session != null) {
            session.closeChest(chest);
        }
    }

    @Override
    public FurnitureEntity placeFurniture(String levelId, String itemId, float x, float y) {
        LevelInstance level = instances.get(levelId);
        if (level == null || !FurnitureFactory.isPlaceable(itemId)
                || !level.getWorld().isPositionWalkable(x, y, 16, 16)) {
            return null;
        }
        FurnitureEntity furniture = FurnitureFactory.create(itemId, x, y);
        FurnitureManager.getInstance().placeFurniture(levelId, furniture);
        level.getWorld().addGameObject(furniture); // Replicated to guests in the level by the host session
        return furniture;
    }

    @Override
    public void removeFurniture(String levelId, FurnitureEntity furniture) {
        FurnitureManager.getInstance().removeFurniture(levelId, furniture);
        LevelInstance level = instances.get(levelId);
        if (level != null) {
            level.getWorld().removeGameObject(furniture);
        }
    }

    // ========== Effects ==========

    /**
     * Attaches game callbacks (damage numbers, death animations, particles) to entities
     * as they enter a level, wherever they came from (map, debug spawn, network).
     */
    private class EntityDecorator implements WorldManager.Listener {
        private final String levelId;

        EntityDecorator(String levelId) {
            this.levelId = levelId;
        }

        @Override
        public void onObjectAdded(GameObject obj) {
            if (obj instanceof EnemyEntity enemy && !enemy.isNetworkControlled()) {
                enemy.setDamageNumberCallback((x, y, damage) -> emitDamageNumber(levelId, x, y, damage));
                enemy.setDeathCallback((deadEnemy, x, y) -> emitDeath(levelId, x, y));
            } else if (obj instanceof ProjectileEntity projectile && !projectile.isNetworkControlled()) {
                projectile.setDamageCallback((x, y, damage) -> emitDamageNumber(levelId, x, y, damage));
            } else if (obj instanceof BreakableEntity breakable) {
                breakable.setParticleCallback((x, y, particleType) -> {
                    if (isCurrentLevel(levelId)) {
                        presenter.showParticles(x, y, particleType);
                    }
                    if (session != null && breakable.getNetId() != 0) {
                        Packets.Effect effect = new Packets.Effect();
                        effect.kind = Packets.Effect.BREAK;
                        effect.netId = breakable.getNetId();
                        session.broadcastEffect(levelId, effect);
                    }
                });
            }
        }

        @Override
        public void onObjectRemoved(GameObject obj) {
        }
    }

    private boolean isCurrentLevel(String levelId) {
        return currentInstance != null && currentInstance.getLevelId().equals(levelId);
    }

    private String levelIdOf(WorldManager someWorld) {
        for (LevelInstance instance : instances.values()) {
            if (instance.getWorld() == someWorld) return instance.getLevelId();
        }
        return null;
    }

    /**
     * A hit landed in some level: show the number here if we're looking at that level,
     * and tell the players in it.
     */
    private void emitDamageNumber(String levelId, float x, float y, int damage) {
        if (levelId == null) return;
        if (isCurrentLevel(levelId)) {
            presenter.showDamageNumber(x, y, damage);
        }
        if (session != null) {
            Packets.Effect effect = new Packets.Effect();
            effect.kind = Packets.Effect.DAMAGE_NUMBER;
            effect.x = x;
            effect.y = y;
            effect.amount = damage;
            session.broadcastEffect(levelId, effect);
        }
    }

    private void emitDeath(String levelId, float x, float y) {
        if (isCurrentLevel(levelId)) {
            presenter.showDeathAnimation(x, y);
        }
        if (session != null) {
            Packets.Effect effect = new Packets.Effect();
            effect.kind = Packets.Effect.DEATH;
            effect.x = x;
            effect.y = y;
            session.broadcastEffect(levelId, effect);
        }
    }

    // ========== Multiplayer ==========

    /**
     * Start hosting on the default port (Open to LAN).
     * @return false if already in a session or the server couldn't start
     */
    public boolean startHosting() {
        return startHosting(GameServer.DEFAULT_PORT);
    }

    public boolean startHosting(int port) {
        if (session != null || clientMode) {
            System.out.println("GameWorld: Already in multiplayer mode");
            return false;
        }

        HostSession host = new HostSession(this, port);
        session = host;
        if (!host.start()) {
            session = null;
            host.dispose();
            System.err.println("GameWorld: Could not start hosting (is the port already in use?)");
            return false;
        }
        return true;
    }

    /**
     * Join a host with an already-connected client. The level is built when the host's welcome arrives.
     */
    public void join(GameClient client) {
        if (session != null) {
            System.out.println("GameWorld: Already in multiplayer mode");
            return;
        }
        session = new ClientSession(client, this);
    }

    /** Joining: play this existing character of the host's world. */
    public void playCharacter(String characterId) {
        if (session instanceof ClientSession client) {
            client.playCharacter(characterId);
        }
    }

    /** Joining: create a new character in the host's world. */
    public void createCharacter(String name) {
        if (session instanceof ClientSession client) {
            client.createCharacter(name);
        }
    }

    /**
     * Stop hosting or disconnect from the host.
     */
    public void stopMultiplayer() {
        if (session != null) {
            NetSession ending = session;
            session = null;
            ending.dispose();
        }
    }

    public boolean isGuest() {
        return clientMode;
    }

    public NetSession getSession() {
        return session;
    }

    public void dispose() {
        stopMultiplayer();
        for (LevelInstance instance : instances.values()) {
            instance.dispose();
        }
        instances.clear();
    }

    // ========== NetGameContext ==========

    @Override
    public PlayerEntity getLocalPlayer() {
        return localPlayer;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public WorldItemManager getWorldItemManager() {
        return worldItemManager;
    }

    @Override
    public LevelInstance getCurrentInstance() {
        return currentInstance;
    }

    @Override
    public Collection<LevelInstance> getInstances() {
        return new ArrayList<>(instances.values());
    }

    @Override
    public LevelInstance getOrCreateInstance(String levelId) {
        LevelInstance instance = instances.get(levelId);
        if (instance == null) {
            instance = createInstance(levelSourceFor(levelId));
        }
        return instance;
    }

    @Override
    public PlayerEntity createRemotePlayer(int playerId, WorldManager playerWorld, float x, float y) {
        PlayerEntity player = new PlayerEntity(playerWorld, x, y);
        player.setPlayerId(playerId);
        player.setNetworkControlled(true);
        player.setDamageNumberCallback((dx, dy, damage) -> emitDamageNumber(levelIdOf(player.getWorld()), dx, dy, damage));
        playerManager.addPlayerWithId(player);
        return player;
    }

    @Override
    public void removeRemotePlayer(PlayerEntity player) {
        playerManager.removePlayer(player);
    }

    @Override
    public void chooseCharacter(Packets.CharacterInfo[] characters, String message) {
        presenter.chooseCharacter(characters, message);
    }

    @Override
    public void startAsClient(int playerId, String levelId, float x, float y, String savedPlayerJson) {
        LevelInstance instance = createInstance(levelSourceFor(levelId));
        enterLevel(instance, new Vector2(x, y));

        com.game.save.PlayerData saved = PlayerDataCodec.fromJson(savedPlayerJson);
        if (saved != null) {
            PlayerDataCodec.apply(localPlayer, saved);
            presenter.onLocalPlayerDataRestored(localPlayer);
            System.out.println("GameWorld: Restored character from the host's save");
        }
    }

    @Override
    public void showDamageNumber(float x, float y, int amount) {
        presenter.showDamageNumber(x, y, amount);
    }

    @Override
    public void showDeathAnimation(float x, float y) {
        presenter.showDeathAnimation(x, y);
    }

    @Override
    public void onInventoryChanged() {
        presenter.onInventoryChanged();
    }

    @Override
    public void onConnectionLost(String reason) {
        System.out.println("GameWorld: " + reason);
        stopMultiplayer();
        presenter.onConnectionLost(reason);
    }
}
