package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.game.components.AttackComponent;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.systems.audio.SoundSystem;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.GatewayEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.entity.PlayerManager;
import com.game.systems.entity.entities.DamageNumberEntity;
import com.game.systems.input.LocalKeyboardInput;
import com.game.systems.entity.entities.DeathAnimationEntity;
import com.game.systems.entity.entities.DestructionParticleEntity;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.enemies.LizardEnemy;
import com.game.systems.entity.entities.enemies.Axolot;
import com.game.systems.entity.entities.enemies.CatEnemy;
import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.rendering.YSortRenderer;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.collision.TiledMapCollisionLoader;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.input.InputAction;
import com.game.systems.input.InputManager;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;
import com.game.systems.item.TestItems;
import com.game.systems.level.LevelData;
import com.game.systems.level.TiledMapParser;
import com.game.systems.ui.UIManagerNew;
import com.game.systems.debug.DebugConsole;
import com.game.systems.debug.DebugManager;
import com.game.systems.furniture.FurnitureManager;
import com.game.systems.furniture.ChestEntity;
import com.game.systems.item.ItemDefinition;
import com.game.systems.item.ItemRegistry;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;

import static com.game.systems.audio.SoundRegistry.*;

/**
 * Refactored GameScreen using the new decoupled architecture.
 * All systems are now independent and reusable.
 */
public class GameScreen implements Screen {
    private static final int VIEWPORT_WIDTH = 350;
    private static final int VIEWPORT_HEIGHT = 200;

    private SpriteBatch batch;
    private BitmapFont debugFont;
    private BitmapFont damageFont; // Separate font for damage numbers
    private boolean debugMode = false;
    private ShapeRenderer shapeRenderer;

    // Debug time scale
    private float timeScale = 1.0f;
    private static final float TIME_SCALE_STEP = 0.25f;
    private static final float MIN_TIME_SCALE = 0.25f;
    private static final float MAX_TIME_SCALE = 4.0f;

    // Camera and viewport
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;

    public WorldManager world;
    public WorldItemManager worldItemManager;
    public PlayerManager playerManager;
    private TiledMap currentMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private YSortRenderer ySortRenderer;
    private UIManagerNew uiManager;
    private InputManager inputManager;

    private DebugManager debugManager;
    private DebugConsole debugConsole;

    // Level loading abstraction
    private com.game.systems.level.LevelSource currentLevelSource;
    private com.game.systems.dungeon.DungeonController dungeonController;
    private com.game.systems.dungeon.DungeonDebugRenderer dungeonDebugRenderer;

    private GatewayEntity pendingGateway = null;

    // Damage numbers
    private java.util.List<DamageNumberEntity> damageNumbers;

    // Death animations
    private java.util.List<DeathAnimationEntity> deathAnimations;

    // Destruction particles
    private java.util.List<DestructionParticleEntity> destructionParticles;

    // Furniture placement mode
    private boolean furniturePlacementMode = false;
    private ItemStack placementFurnitureItem = null;
    private Texture placementPreviewTexture = null;
    private com.game.systems.ui.ItemSlotUI placementSourceSlot = null;
    private FurnitureManager furnitureManager;

    // Currently open chest (for auto-closing)
    private ChestEntity currentlyOpenChest = null;
    private static final float CHEST_AUTO_CLOSE_DISTANCE = 32f; // pixels

    // Save name tracking for auto-save
    private String currentSaveName = null;

    // Networking
    private com.game.networking.GameServer gameServer;
    private com.game.networking.GameClient gameClient;
    private boolean isHost = false;
    private boolean isClient = false;
    private int localPlayerId = -1; // The player ID controlled by this client (-1 = not set)
    private float inputSendTimer = 0f;
    private float stateSendTimer = 0f;
    private static final float INPUT_SEND_INTERVAL = 0.0166f; // ~60 times per second (every frame)
    private static final float STATE_SEND_INTERVAL = 0.05f; // 20 times per second (server broadcasts)

    // Client prediction with periodic checkpoints
    private static final float POSITION_CORRECTION_THRESHOLD = 4.5f; // pixels - only correct if off by more than this
    private static final float CORRECTION_SPEED = 0.1f; // How fast to lerp to server position (0-1, higher = faster)

    /**
     * Create a new game with default starting level.
     */
    public GameScreen() {
        this((com.game.save.SaveData) null, false); // Delegate to save data constructor
    }

    /**
     * Create a new game in client mode (joining multiplayer).
     * @param clientMode If true, this is a client joining a multiplayer game
     */
    public GameScreen(boolean clientMode) {
        this((com.game.save.SaveData) null, clientMode);
        // isClient is already set in the main constructor
    }

    /**
     * Create a new game with a save name.
     * @param saveName The name for this save
     */
    public GameScreen(String saveName) {
        this((com.game.save.SaveData) null, false);
        this.currentSaveName = saveName;
    }

    /**
     * Create game from save data.
     * If saveData is null, starts a new game.
     */
    public GameScreen(com.game.save.SaveData saveData) {
        this(saveData, false);
    }

    /**
     * Create game from save data or start new game.
     * @param saveData Save data to load, or null for new game
     * @param clientMode If true, this is a client joining multiplayer
     */
    public GameScreen(com.game.save.SaveData saveData, boolean clientMode) {
        // Set client mode FIRST, before any level loading
        if (clientMode) {
            isClient = true;
            System.out.println("GameScreen: Created in client mode");
        }

        // Create camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        debugFont = new BitmapFont();
        debugFont.setColor(1, 1, 0, 1);
        debugFont.getData().setScale(0.5f);

        // Separate font for damage numbers to avoid interfering with debug text
        damageFont = new BitmapFont();
        damageFont.setColor(1, 1, 1, 1);

        // Reset all game singletons to prevent stale references
        // This ensures clean state when creating new GameScreen (new game or load)
        com.game.util.SingletonManager.resetAllGameSingletons();

        // Initialize systems
        worldItemManager = new WorldItemManager();
        inputManager = new InputManager();
        debugManager = new DebugManager();
        playerManager = new PlayerManager();

        // Initialize singleton systems with new dependencies
        furnitureManager = FurnitureManager.getInstance();
        com.game.systems.loot.LootSystem.initialize(worldItemManager);

        damageNumbers = new java.util.ArrayList<>();
        deathAnimations = new java.util.ArrayList<>();
        destructionParticles = new java.util.ArrayList<>();

        // Initialize level loading systems
        dungeonController = new com.game.systems.dungeon.DungeonController();
        dungeonDebugRenderer = new com.game.systems.dungeon.DungeonDebugRenderer();
        dungeonDebugRenderer.setShowAll(false); // Off by default, toggle with F3

        // Initialize BreakableObjectRegistry
        com.game.systems.breakable.BreakableObjectRegistry.loadConfigs();

        // Register test items
        TestItems.registerTestItems();

        // Reset texture flag to force reload for new WorldItemManager instance
        TestItems.resetTextureFlag();
        TestItems.loadTextures(worldItemManager);

        // Load game from save data or start new game
        if (saveData != null) {
            // Store save name for auto-save
            currentSaveName = saveData.saveName;
            loadFromSaveData(saveData);
        } else {
            // Load initial level for new game
            loadLevel("Maps/prototype.tmx", null);
        }
    }

    @Override
    public void render(float delta) {
        // Handle pending gateway transition
        if (pendingGateway != null) {
            loadLevel(pendingGateway.getTargetLevel(), pendingGateway.getTargetSpawn());
            pendingGateway = null;
        }

        // Check for debug console toggle (always check this first)
        inputManager.update();
        if (inputManager.isJustPressed(InputAction.DEBUG_CONSOLE)) {
            if (debugConsole != null) {
                debugConsole.toggle();
            }
        }
        else if (inputManager.isJustPressed(InputAction.DEBUG_ITEMS)) {
            uiManager.toggleItemBrowser();
        }

        // Only process game input when console is NOT open
        boolean consoleOpen = debugConsole != null && debugConsole.isVisible();

        // Disable all players' movement when console is open
        for (PlayerEntity player : playerManager.getAllPlayers()) {
            player.setInputEnabled(!consoleOpen);
        }

        if (!consoleOpen) {
            // Handle input actions
            handleInputActions();

            // Check for debug toggle
            if (inputManager.isJustPressed(InputAction.DEBUG_TOGGLE)) {
                debugMode = !debugMode;
                if (ySortRenderer != null) {
                    ySortRenderer.setDebugMode(debugMode);
                }
                // Toggle dungeon debug rendering
                if (dungeonDebugRenderer != null) {
                    dungeonDebugRenderer.setShowAll(debugMode);
                }
                System.out.println("Debug mode: " + debugMode);
            }
        }

        // Apply time scale to delta (only affects game simulation, not rendering)
        float scaledDelta = delta * timeScale;

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update world
        world.update(scaledDelta);

        // Check if player has moved away from open chest
        updateOpenChestDistance();

        // Update damage numbers
        damageNumbers.removeIf(dn -> {
            dn.update(scaledDelta);
            return !dn.isAlive();
        });

        // Update death animations
        deathAnimations.removeIf(da -> {
            da.update(scaledDelta);
            return !da.isAlive();
        });

        // Update destruction particles
        destructionParticles.removeIf(dp -> {
            dp.update(scaledDelta);
            return !dp.isAlive();
        });

        // Update world items
        worldItemManager.update(scaledDelta);

        // Update item magnetism (register nearby items)
        updateItemMagnetism();

        // Check for item pickup collisions
        checkItemPickups();

        // Check for gateway collisions
        checkGatewayCollisions();

        // Update networking (send/receive packets)
        updateNetworking(delta);

        // Update UI
        if (uiManager != null) {
            uiManager.update(delta);
        }

        // Update camera
        updateCamera();

        // Render world
        viewport.apply();
        mapRenderer.setView(camera);
        batch.setProjectionMatrix(camera.combined);

        if (ySortRenderer != null) {
            ySortRenderer.render(batch, world.getGameObjects(), this::renderEntity);
        } else {
            mapRenderer.render();
            batch.begin();
            world.render(batch);
            batch.end();
        }

        // Render world items
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldItemManager.render(batch);
        batch.end();

        // Render damage numbers
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (DamageNumberEntity damageNumber : damageNumbers) {
            damageNumber.render(batch);
        }
        batch.end();

        // Render death animations
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (DeathAnimationEntity deathAnimation : deathAnimations) {
            deathAnimation.render(batch);
        }
        batch.end();

        // Render destruction particles
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (DestructionParticleEntity particle : destructionParticles) {
            particle.render(batch);
        }
        batch.end();

        // Handle furniture placement mode (only for player 1)
        if (furniturePlacementMode) {
            handleFurniturePlacement(delta);
        }

        // Render UI
        if (uiManager != null) {
            uiManager.render();
        }

        // Render debug
        if (debugMode || debugManager.isEnabled("colliders")) {
            renderCollisionDebug();
        }
        if (debugMode || debugManager.isEnabled("navmesh")) {
            renderNavMeshDebug();
        }
        if (debugMode || debugManager.isEnabled("fps")) {
            renderDebugStats();
        }
    }

    /**
     * Handles input actions from InputManager.
     */
    private void handleInputActions() {
        // Open settings menu
        if (inputManager.isJustPressed(InputAction.OPEN_SETTINGS)) {
            if (uiManager != null) {
                uiManager.toggleSettings();
            }
        }

        // Open inventory (B key - bags only)
        if (inputManager.isJustPressed(InputAction.OPEN_INVENTORY)) {
            if (uiManager != null) {
                uiManager.toggleInventory();

                // If inventory is closed, restore input to stage (for HUD)
                // If open, input processor stays on stage (for dragging)
                // Stage always handles input when UI exists
            }
        }

        // Open equipment + inventory (I key - both)
        if (inputManager.isJustPressed(InputAction.OPEN_EQUIPMENT)) {
            if (uiManager != null) {
                uiManager.toggleEquipmentAndInventory();

                // Stage always handles input when UI exists
            }
        }

        // Interact with nearby furniture (E key)
        if (inputManager.isJustPressed(InputAction.INTERACT)) {
            handleFurnitureInteraction();
        }

        // Debug: Spawn wood item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_ITEM)) {
            spawnDebugItem("wood");
        }

        // Debug: Spawn bag item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_BAG)) {
            spawnDebugItem("bag");
        }

        // Debug: Spawn bag2 item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_BAG2)) {
            spawnDebugItem("bag2");
        }

        // Debug: Spawn bag3 item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_BAG3)) {
            spawnDebugItem("bag3");
        }

        // Debug: Spawn wooden chest item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_CHEST)) {
            spawnDebugItem("wooden_chest");
        }

        // Debug: Spawn enemies
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_SLIME)) {
            spawnDebugEnemy("slime");
        }

        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_FROG)) {
            spawnDebugEnemy("frog");
        }

        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_CAT)) {
            spawnDebugEnemy("cat");
        }

        // Debug: Spawn breakable objects
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_POT)) {
            spawnDebugBreakable("pot");
        }

        // Debug: Test sound system
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_TEST_SOUND)) {
            testSound();
        }

        // Debug: Time scale controls
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_INCREASE_SPEED)) {
            timeScale = Math.min(timeScale + TIME_SCALE_STEP, MAX_TIME_SCALE);
            System.out.println("Time scale: " + timeScale + "x");
        }
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_DECREASE_SPEED)) {
            timeScale = Math.max(timeScale - TIME_SCALE_STEP, MIN_TIME_SCALE);
            System.out.println("Time scale: " + timeScale + "x");
        }
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_RESET_SPEED)) {
            timeScale = 1.0f;
            System.out.println("Time scale reset to: " + timeScale + "x");
        }

        // Debug: Save game
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SAVE)) {
            com.game.save.SaveManager.getInstance().save("debug_save");
            System.out.println("=== SAVED GAME (F6) ===");
        }

        // Debug: Load game
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_LOAD)) {
            com.game.save.SaveData saveData = com.game.save.SaveManager.getInstance().load("debug_save");
            if (saveData != null) {
                loadFromSaveData(saveData);
                System.out.println("=== LOADED GAME (F7) ===");
            } else {
                System.out.println("=== NO SAVE FOUND (F7) ===");
            }
        }
    }

    /**
     * Debug function: Spawns an item at mouse position.
     * @param itemId The item ID to spawn
     */
    private void spawnDebugItem(String itemId) {
        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Create item
        ItemStack itemStack = ItemFactory.create(itemId, 1);
        if (itemStack != null) {
            worldItemManager.spawnItem(itemStack, mousePos.x, mousePos.y, 0);
            System.out.println("Spawned " + itemId + " at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
        }
    }

    /**
     * Debug function: Spawns an enemy at mouse position.
     * @param enemyType The enemy type to spawn (slime, frog, cat)
     */
    private void spawnDebugEnemy(String enemyType) {
        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Create enemy based on type
        EnemyEntity enemy = null;
        switch (enemyType.toLowerCase()) {
            case "slime":
                enemy = new LizardEnemy(world, mousePos.x, mousePos.y);
                System.out.println("Spawned Slime at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
                break;

            case "frog":
                enemy = new Axolot(world, mousePos.x, mousePos.y);
                System.out.println("Spawned Frog at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
                break;

            case "cat":
                enemy = new CatEnemy(world, mousePos.x, mousePos.y);
                System.out.println("Spawned Cat at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
                break;

            default:
                System.out.println("Unknown enemy type: " + enemyType);
                break;
        }

        // Set damage number callback for enemy
        if (enemy != null) {
            enemy.setDamageNumberCallback((x, y, damage) -> {
                DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
                damageNumbers.add(damageNumber);
            });

            // Set death callback to spawn animation and disable hitbox
            enemy.setDeathCallback((deadEnemy, x, y) -> {
                // Spawn death animation
                DeathAnimationEntity deathAnimation = new DeathAnimationEntity(x, y);
                deathAnimations.add(deathAnimation);

                // Disable combat collider so enemy can't be hit again
                // Enemy stays in world but is inactive (already set by onDeath -> setActive(false))
                com.game.components.ColliderComponent combatCollider = deadEnemy.getCombatCollider();
                if (combatCollider != null) {
                    // Mark collider as disabled by setting size to 0
                    combatCollider.setSize(0, 0);
                }
            });

            world.addGameObject(enemy);
        }
    }

    /**
     * Debug function: Tests the sound system by playing a test sound.
     */
    private void testSound() {
        SoundSystem.getInstance().playSound(
            SWORD_SWING
        );
    }

    /**
     * Debug function: Spawns a breakable object at mouse position.
     * @param objectType The object type to spawn (pot, crate, etc.)
     */
    private void spawnDebugBreakable(String objectType) {
        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Create breakable object using factory
        BreakableEntity breakable = com.game.systems.breakable.BreakableObjectFactory.create(
            objectType,
            mousePos.x,
            mousePos.y
        );

        if (breakable != null) {
            // Set particle callback to spawn destruction particles
            breakable.setParticleCallback((x, y, particleType) -> {
                DestructionParticleEntity particle = new DestructionParticleEntity(x, y, particleType);
                destructionParticles.add(particle);
            });

            world.addGameObject(breakable);
            System.out.println("Spawned " + objectType + " at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
        } else {
            System.out.println("Failed to spawn breakable: " + objectType);
        }
    }

    /**
     * Updates item magnetism - registers nearby items with all players' magnet components.
     */
    private void updateItemMagnetism() {
        // Update magnet for all players
        for (PlayerEntity player : playerManager.getAllPlayers()) {
            Vector2 playerPos = player.getTransform().getPosition();
            float magnetRadius = player.getItemMagnet().getMagnetRadius();

            // Get nearby items
            for (ItemPickupEntity item : worldItemManager.getItemsNear(playerPos, magnetRadius)) {
                player.getItemMagnet().registerItem(item);
            }
        }
    }

    /**
     * Checks for item pickup collisions with all players.
     */
    private void checkItemPickups() {
        if (!playerManager.hasPlayers()) return;

        boolean inventoryChanged = false;

        // Check all items against all players
        for (ItemPickupEntity item : worldItemManager.getAllItems()) {
            if (!item.canPickup() || !item.isActive()) continue;

            Transform itemTransform = item.getComponent(Transform.class);
            if (itemTransform == null) continue;

            // Check each player for pickup
            for (PlayerEntity player : playerManager.getAllPlayers()) {
                Transform playerTransform = player.getTransform();
                ColliderComponent playerCollider = player.getEnvironmentCollider();

                if (playerCollider == null) continue;

                // Simple distance check (could use collider for more precision)
                float distance = playerTransform.getPosition().dst(itemTransform.getPosition());
                if (distance < 16f) { // Pickup radius
                    // Try to add to inventory
                    ItemStack itemStack = item.getItemStack();
                    ItemStack remaining = player.getInventory().addItem(itemStack);

                    if (remaining == null) {
                        // All picked up
                        item.onPickup();
                        worldItemManager.removeItem(item);

                        // Play pickup sound
                        SoundSystem.getInstance().playSound(COIN_PICKUP, 0.6f);

                        System.out.println("Player " + player.getPlayerId() + " picked up: " + itemStack.toString());
                        inventoryChanged = true;
                        break; // Item was picked up, stop checking other players
                    } else if (remaining.getQuantity() < itemStack.getQuantity()) {
                        // Partial pickup
                        item.getItemStack().setQuantity(remaining.getQuantity());

                        // Play pickup sound
                        SoundSystem.getInstance().playSound(COIN_PICKUP, 0.6f);

                        inventoryChanged = true;
                        break; // Partial pickup, stop checking other players
                    }
                }
            }
        }

        // Notify UI if inventory changed (for first player's UI)
        if (inventoryChanged && uiManager != null) {
            uiManager.notifyInventoryChanged();
        }
    }

    /**
     * Load an assembled dungeon directly (from dungeon generation system).
     * Public so DebugConsole can access it.
     * Wrapper that uses the unified loadLevel(LevelSource) method.
     */
    public void loadAssembledDungeon(com.game.systems.dungeon.assembly.AssembledDungeon dungeon) {
        // Create a DungeonLevelSource and delegate to unified method
        com.game.systems.dungeon.DungeonLevelSource levelSource =
            new com.game.systems.dungeon.DungeonLevelSource(dungeon, null);
        loadLevel(levelSource, null);
    }

    /**
     * Get the dungeon controller for external access (e.g., DebugConsole).
     * @return The dungeon controller
     */
    public com.game.systems.dungeon.DungeonController getDungeonController() {
        return dungeonController;
    }


    /**
     * Unified level loading method using LevelSource abstraction.
     * This replaces the old loadLevel() and loadAssembledDungeon() methods.
     * @param levelSource The level source (Tiled map or dungeon)
     * @param spawnPointName Optional spawn point name (null = use default)
     */
    private void loadLevel(com.game.systems.level.LevelSource levelSource, String spawnPointName) {
        System.out.println("Loading level: " + levelSource.getLevelName() +
            (spawnPointName != null ? " at spawn: " + spawnPointName : ""));

        // Dispose previous resources
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (currentMap != null) {
            currentMap.dispose();
        }
        if (currentLevelSource != null) {
            currentLevelSource.dispose();
        }

        // Store the new level source
        currentLevelSource = levelSource;

        // Get TiledMap and LevelData from the source
        currentMap = levelSource.getTiledMap();
        mapRenderer = new OrthogonalTiledMapRenderer(currentMap);
        ySortRenderer = new YSortRenderer(mapRenderer, currentMap);

        LevelData levelData = levelSource.getLevelData();

        // Create world manager
        world = new WorldManager(levelData.getWidth(), levelData.getHeight());

        // Load collision system using the source's method
        SpatialQuery collisionSystem = new SpatialQuery();
        levelSource.loadCollision(collisionSystem);
        world.setCollisionSystem(collisionSystem);

        // Build grid pathfinder for pathfinding
        world.buildGridPathfinder(currentMap);

        // Get spawn position - with proper fallback logic
        LevelData.SpawnPoint spawn;
        if (spawnPointName != null) {
            // Try to get the named spawn point
            spawn = levelData.getSpawnPoint(spawnPointName);
            if (spawn == null) {
                // If named spawn doesn't exist, fall back to default player_spawn
                System.out.println("Warning: Spawn point '" + spawnPointName + "' not found, using player_spawn");
                spawn = levelData.getDefaultSpawnPoint();
            }
        } else {
            // No spawn name specified, use default
            spawn = levelData.getDefaultSpawnPoint();
        }

        float spawnX = spawn != null ? spawn.getX() : 50;
        float spawnY = spawn != null ? spawn.getY() : 750;

        // Convert to grid and back to match old behavior
        int spawnGridX = (int)(spawnX / world.getTileSize());
        int spawnGridY = (int)(spawnY / world.getTileSize());
        spawnX = spawnGridX * world.getTileSize();
        spawnY = spawnGridY * world.getTileSize();

        System.out.println("Spawning players at: (" + spawnX + ", " + spawnY + ") - Grid: (" + spawnGridX + ", " + spawnGridY + ")");

        // Create or update players
        if (playerManager.getPlayerCount() == 0) {
            // If we're a client waiting for player assignment, don't create a player yet
            if (isClient && localPlayerId == -1) {
                System.out.println("Client mode: Waiting for server to assign player ID...");
            } else {
                // Create local player (host or single-player)
                PlayerEntity player1 = new PlayerEntity(world, spawnX, spawnY);
                LocalKeyboardInput input1 = LocalKeyboardInput.createPlayer1();
                input1.setCamera(camera);
                input1.setPlayerTransform(player1.getTransform());
                player1.setInputSource(input1);

                // Set damage number callback
                player1.setDamageNumberCallback((x, y, damage) -> {
                    DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
                    damageNumbers.add(damageNumber);
                });

                playerManager.addPlayer(player1);
                world.addGameObject(player1);

                if (isHost) {
                    System.out.println("Created host player (Player 0)");
                    localPlayerId = 0; // Host is always Player 0
                } else {
                    System.out.println("Created local player:");
                    System.out.println("  Player 1: WASD + Left Click");
                }
            }
        } else {
            // Update existing players' world and re-add to new world
            for (PlayerEntity player : playerManager.getAllPlayers()) {
                player.setWorld(world);

                // In multiplayer, only teleport the local player to spawn
                // Remote players keep their positions (they move independently)
                if (isHost || isClient) {
                    // Only teleport if this is the local player
                    if (player.getPlayerId() == localPlayerId) {
                        player.getTransform().setPosition(spawnX, spawnY);
                    }
                    // Remote players: don't change position
                } else {
                    // Single-player: teleport all players (usually just one)
                    player.getTransform().setPosition(spawnX, spawnY);
                }

                world.addGameObject(player);
            }
        }

        // Initialize UI manager (if not already initialized)
        // Use the local player for UI
        PlayerEntity localPlayer = (localPlayerId >= 0) ? playerManager.getPlayerById(localPlayerId) : playerManager.getFirstPlayer();
        if (uiManager == null && localPlayer != null) {
            uiManager = new UIManagerNew(localPlayer.getInventory(), worldItemManager);
            uiManager.setItemDropCallback(itemStack -> {
                Vector2 playerPos = localPlayer.getTransform().getPosition();
                worldItemManager.spawnItem(itemStack, playerPos.x, playerPos.y, 0f);
            });

            uiManager.setFurniturePlacementCallback(new UIManagerNew.FurniturePlacementCallback() {
                @Override
                public void onPlaceFurniture(ItemStack furnitureItem, com.game.systems.ui.ItemSlotUI sourceSlot) {
                    enterFurniturePlacementMode(furnitureItem, sourceSlot);
                }

                @Override
                public void onCancelPlacement() {
                    exitFurniturePlacementMode(false);
                }
            });

            uiManager.setPlayerHealth(localPlayer.getHealthComponent());
            uiManager.setPlayer(localPlayer);

            // Set pause menu callback
            uiManager.setPauseMenuCallback(new com.game.systems.ui.UIManagerNew.PauseMenuCallback() {
                @Override
                public void onOpenToLAN() {
                    startHosting();
                }

                @Override
                public void onReturnToMainMenu() {
                    // Stop multiplayer before returning
                    stopMultiplayer();

                    // Auto-save before returning to main menu
                    if (currentSaveName != null) {
                        com.game.save.SaveManager.getInstance().save(currentSaveName);
                        System.out.println("GameScreen: Auto-saved to: " + currentSaveName);
                    } else {
                        System.out.println("GameScreen: No save name set, skipping auto-save");
                    }

                    // Return to main menu
                    com.badlogic.gdx.Game game = (com.badlogic.gdx.Game) Gdx.app.getApplicationListener();
                    game.setScreen(new MainMenuScreen((Main) game));
                }
            });

            // Initialize SaveManager (use local player)
            com.game.save.SaveManager.getInstance().initialize(
                localPlayer,
                localPlayer.getInventory(),
                furnitureManager,
                worldItemManager
            );

            // Initialize debug console
            debugConsole = new DebugConsole(uiManager.getSkin(), this, debugManager);
            debugConsole.setSize(400, 600);
            debugConsole.setPosition(10, VIEWPORT_HEIGHT - 70);
            debugConsole.padTop(20);
            uiManager.getStage().addActor(debugConsole);

            Gdx.input.setInputProcessor(uiManager.getStage());
            System.out.println("GameScreen: Input processor set to UI Stage");
        }

        // Create gateway entities
        for (LevelData.LevelObject obj : levelData.getObjectsByType("gateway")) {
            String targetLevel = obj.getPropertyString("targetLevel", null);
            String targetSpawn = obj.getPropertyString("targetSpawn", null);

            if (targetLevel != null) {
                GatewayEntity gateway = new GatewayEntity(
                    obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight(),
                    targetLevel, targetSpawn
                );
                world.addGameObject(gateway);
                System.out.println("Loaded gateway to: " + targetLevel + " at spawn: " + targetSpawn);
            }
        }

        // Load breakable objects
        String[] breakableTypes = {"pot", "clay_pot"};
        for (String breakableType : breakableTypes) {
            for (LevelData.LevelObject obj : levelData.getObjectsByType(breakableType)) {
                BreakableEntity breakable = com.game.systems.breakable.BreakableObjectFactory.create(
                    breakableType,
                    obj.getX(),
                    obj.getY()
                );

                if (breakable != null) {
                    // Set particle callback
                    breakable.setParticleCallback((x, y, particleType) -> {
                        DestructionParticleEntity particle = new DestructionParticleEntity(x, y, particleType);
                        destructionParticles.add(particle);
                    });

                    world.addGameObject(breakable);
                    System.out.println("Loaded breakable: " + breakableType);
                }
            }
        }

        // Load placed furniture for this level
        furnitureManager.loadFurnitureIntoWorld(levelSource.getLevelName(), world);

        // Update SaveManager with current level info
        String levelType = (levelSource instanceof com.game.systems.dungeon.DungeonLevelSource) ? "dungeon" : "tiled_map";
        com.game.save.SaveManager.getInstance().setCurrentLevel(levelSource.getLevelName(), levelType);

        // Update WorldItemManager with current level
        worldItemManager.setCurrentLevel(levelSource.getLevelName());
    }

    /**
     * Load a level from a Tiled map file (.tmx).
     * Wrapper that uses the unified loadLevel(LevelSource) method.
     */
    private void loadLevel(String levelPath, String spawnPointName) {
        // Create a TiledMapLevelSource and delegate to unified method
        com.game.systems.level.TiledMapLevelSource levelSource =
            new com.game.systems.level.TiledMapLevelSource(levelPath);
        loadLevel(levelSource, spawnPointName);
    }

    private void checkGatewayCollisions() {
        if (!playerManager.hasPlayers()) return;

        // Check all gateways against any player
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof GatewayEntity) {
                GatewayEntity gateway = (GatewayEntity) obj;
                ColliderComponent gatewayCollider = gateway.getComponent(ColliderComponent.class);

                if (gatewayCollider != null) {
                    Rectangle gatewayBounds = gatewayCollider.getBounds(gateway);

                    // Check if any player is touching the gateway
                    for (PlayerEntity player : playerManager.getAllPlayers()) {
                        ColliderComponent playerCollider = player.getComponent(ColliderComponent.class);
                        if (playerCollider != null) {
                            Rectangle playerBounds = playerCollider.getBounds(player);
                            if (playerBounds.overlaps(gatewayBounds)) {
                                pendingGateway = gateway;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateCamera() {
        // Apply camera scale from settings
        if (uiManager != null) {
            float cameraScale = uiManager.getGameSettings().getCameraScale();
            camera.zoom = 1f / cameraScale;
        }

        if (!playerManager.hasPlayers()) return;

        // Follow local player (or first player in single-player mode)
        PlayerEntity player = (localPlayerId >= 0) ? playerManager.getPlayerById(localPlayerId) : playerManager.getFirstPlayer();
        if (player == null) return;

        Transform playerTransform = player.getTransform();
        float playerCenterX = playerTransform.getX() + (world.getTileSize() / 2f);
        float playerCenterY = playerTransform.getY() + (world.getTileSize() / 2f);

        float worldWidth = world.getWorldWidth() * world.getTileSize();
        float worldHeight = world.getWorldHeight() * world.getTileSize();

        float cameraHalfWidth = camera.viewportWidth * camera.zoom / 2f;
        float cameraHalfHeight = camera.viewportHeight * camera.zoom / 2f;

        float camX = Math.max(cameraHalfWidth, Math.min(playerCenterX, worldWidth - cameraHalfWidth));
        float camY = Math.max(cameraHalfHeight - 12, Math.min(playerCenterY, worldHeight - cameraHalfHeight));

        camera.position.set(camX, camY, 0);
        camera.update();
    }

    private void renderCollisionDebug() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Render world collision (red)
        shapeRenderer.setColor(1, 0, 0, 1);
        for (Rectangle rect : world.getCollisionSystem().getRectangles()) {
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        for (Polygon poly : world.getCollisionSystem().getPolygons()) {
            shapeRenderer.polygon(poly.getTransformedVertices());
        }

        // Render all players' colliders
        for (PlayerEntity player : playerManager.getAllPlayers()) {
            // Environment collider (green) - feet
            shapeRenderer.setColor(0, 1, 0, 1);
            ColliderComponent envCollider = player.getEnvironmentCollider();
            if (envCollider != null) {
                Rectangle envBounds = envCollider.getBounds(player);
                shapeRenderer.rect(envBounds.x, envBounds.y, envBounds.width, envBounds.height);
            }

            // Combat collider (yellow) - full body
            shapeRenderer.setColor(1, 1, 0, 1);
            ColliderComponent combatCollider = player.getCombatCollider();
            if (combatCollider != null) {
                Rectangle combatBounds = combatCollider.getBounds(player);
                shapeRenderer.rect(combatBounds.x, combatBounds.y, combatBounds.width, combatBounds.height);
            }

            // Attack hitbox (red, semi-transparent) - only when attacking
            com.game.components.AttackComponent attackComp = player.getAttackComponent();
            if (attackComp != null && attackComp.isAttacking() && attackComp.getCurrentWeapon() != null) {
                shapeRenderer.setColor(1, 0, 0, 0.5f);

                // Get attack strategy and render polygon
                com.game.systems.combat.AttackStrategy strategy =
                    com.game.systems.combat.AttackSystem.getStrategy(attackComp.getCurrentWeapon().getType());

                float[] polygon = strategy.getHitboxPolygon(player, attackComp, attackComp.getCurrentWeapon());
                if (polygon != null && polygon.length == 8) {
                    // Draw the 4-sided polygon
                    shapeRenderer.triangle(polygon[0], polygon[1], polygon[2], polygon[3], polygon[4], polygon[5]);
                    shapeRenderer.triangle(polygon[0], polygon[1], polygon[4], polygon[5], polygon[6], polygon[7]);
                }
            }
        }

        // Render enemy colliders
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof EnemyEntity enemy) {
                // Skip inactive enemies (dead, etc.)
                if (!enemy.isActive()) continue;

                // Environment collider (cyan) - feet
                shapeRenderer.setColor(0, 1, 1, 1);
                ColliderComponent envCollider = enemy.getEnvironmentCollider();
                if (envCollider != null) {
                    Rectangle envBounds = envCollider.getBounds(enemy);
                    shapeRenderer.rect(envBounds.x, envBounds.y, envBounds.width, envBounds.height);
                }

                // Combat collider (magenta) - full body
                shapeRenderer.setColor(1, 0, 1, 1);
                ColliderComponent combatCollider = enemy.getCombatCollider();
                if (combatCollider != null) {
                    Rectangle combatBounds = combatCollider.getBounds(enemy);
                    shapeRenderer.rect(combatBounds.x, combatBounds.y, combatBounds.width, combatBounds.height);
                }

                // Attack hitbox (orange, semi-transparent) - only when attacking
                AttackComponent attackComp = enemy.getComponent(AttackComponent.class);
                if (attackComp != null && attackComp.isAttacking() && attackComp.getCurrentWeapon() != null) {
                    shapeRenderer.setColor(1, 0.5f, 0, 0.5f);

                    // Get attack strategy and render polygon
                    com.game.systems.combat.AttackStrategy strategy =
                        com.game.systems.combat.AttackSystem.getStrategy(attackComp.getCurrentWeapon().getType());

                    float[] polygon = strategy.getHitboxPolygon(enemy, attackComp, attackComp.getCurrentWeapon());
                    if (polygon != null && polygon.length == 8) {
                        // Draw the 4-sided polygon
                        shapeRenderer.triangle(polygon[0], polygon[1], polygon[2], polygon[3], polygon[4], polygon[5]);
                        shapeRenderer.triangle(polygon[0], polygon[1], polygon[4], polygon[5], polygon[6], polygon[7]);
                    }
                }
            } else if (obj instanceof BreakableEntity breakable) {
                // Skip inactive breakables (destroyed, etc.)
                if (!breakable.isActive()) continue;

                // Environment collider (blue) - feet/base area for walking collision
                shapeRenderer.setColor(0, 0.5f, 1, 1); // Light blue
                ColliderComponent envCollider = breakable.getEnvironmentCollider();
                if (envCollider != null) {
                    Rectangle envBounds = envCollider.getBounds(breakable);
                    shapeRenderer.rect(envBounds.x, envBounds.y, envBounds.width, envBounds.height);
                }

                // Combat collider (light green) - full body for attacks
                shapeRenderer.setColor(0.5f, 1, 0.5f, 1); // Light green
                ColliderComponent combatCollider = breakable.getCombatCollider();
                if (combatCollider != null) {
                    Rectangle combatBounds = combatCollider.getBounds(breakable);
                    shapeRenderer.rect(combatBounds.x, combatBounds.y, combatBounds.width, combatBounds.height);
                }
            } else if (obj instanceof com.game.systems.furniture.FurnitureEntity furniture) {
                // Furniture collider (orange) - collision box
                shapeRenderer.setColor(1, 0.65f, 0, 1); // Orange
                ColliderComponent furnitureCollider = furniture.getCollider();
                if (furnitureCollider != null) {
                    Rectangle furnitureBounds = furnitureCollider.getBounds(furniture);
                    shapeRenderer.rect(furnitureBounds.x, furnitureBounds.y, furnitureBounds.width, furnitureBounds.height);
                }
            }
        }

        // Render dungeon-specific debug overlays if in a dungeon
        if (currentLevelSource != null && currentLevelSource.isDungeon()) {
            com.game.systems.dungeon.DungeonLevelSource dungeonSource =
                (com.game.systems.dungeon.DungeonLevelSource) currentLevelSource;
            dungeonDebugRenderer.render(shapeRenderer, world.getCollisionSystem(), dungeonSource);
        }

        shapeRenderer.end();
    }

    private void renderNavMeshDebug() {
        if (world.getGridPathfinder() == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Render grid pathfinder (only unwalkable cells for performance)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 0, 0, 0.15f); // Red outline, very transparent

        com.game.systems.pathfinding.GridPathfinder pathfinder = world.getGridPathfinder();
        boolean[][] grid = pathfinder.getWalkableGrid();
        int cellSize = pathfinder.getCellSize();

        // Only render unwalkable cells (more visible and faster)
        for (int x = 0; x < pathfinder.getGridWidth(); x++) {
            for (int y = 0; y < pathfinder.getGridHeight(); y++) {
                if (!grid[x][y]) { // Unwalkable
                    float worldX = x * cellSize;
                    float worldY = y * cellSize;
                    shapeRenderer.rect(worldX, worldY, cellSize, cellSize);
                }
            }
        }

        shapeRenderer.end();

        // Render enemy paths
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof EnemyEntity enemy) {
                // Skip inactive enemies (dead, etc.)
                if (!enemy.isActive()) continue;

                // Access current path via a getter we'll need to add
                Array<Vector2> path = enemy.getCurrentPath();
                int waypointIndex = enemy.getCurrentWaypointIndex();

                if (path != null && path.size > 0) {
                    // Draw waypoints
                    for (int i = 0; i < path.size; i++) {
                        Vector2 waypoint = path.get(i);

                        if (i < waypointIndex) {
                            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.5f); // Gray - passed
                        } else if (i == waypointIndex) {
                            shapeRenderer.setColor(1, 1, 0, 0.8f); // Yellow - current
                        } else {
                            shapeRenderer.setColor(1, 1, 1, 0.6f); // White - future
                        }

                        shapeRenderer.circle(waypoint.x, waypoint.y, 3f, 8);
                    }
                }
            }
        }

        shapeRenderer.end();

        // Draw lines between waypoints
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof EnemyEntity enemy) {
                // Skip inactive enemies (dead, etc.)
                if (!enemy.isActive()) continue;

                Array<Vector2> path = enemy.getCurrentPath();
                int waypointIndex = enemy.getCurrentWaypointIndex();

                if (path != null && path.size > 1) {
                    shapeRenderer.setColor(0, 1, 0, 0.5f); // Green lines

                    // Draw lines between waypoints
                    for (int i = waypointIndex; i < path.size - 1; i++) {
                        shapeRenderer.line(path.get(i), path.get(i + 1));
                    }

                    // Draw line from enemy feet to current waypoint
                    if (waypointIndex < path.size) {
                        shapeRenderer.setColor(1, 0.5f, 0, 0.7f); // Orange

                        // Get feet position from environment collider
                        ColliderComponent envCollider = enemy.getEnvironmentCollider();
                        Transform enemyTransform = enemy.getTransform();
                        float feetX = enemyTransform.getX() + envCollider.getOffsetX() + envCollider.getWidth() / 2f;
                        float feetY = enemyTransform.getY() + envCollider.getOffsetY() + envCollider.getHeight() / 2f;

                        shapeRenderer.line(
                            feetX, feetY,
                            path.get(waypointIndex).x, path.get(waypointIndex).y
                        );
                    }
                }
            }
        }

        shapeRenderer.end();
    }

    /**
     * Render a single entity. Called by Y-sort renderer.
     */
    private void renderEntity(SpriteBatch batch, GameObject gameObject) {
        // Render character
        RenderComponent renderComp = gameObject.getComponent(RenderComponent.class);
        if (renderComp != null) {
            renderComp.render(batch, gameObject);
        }

        // Render weapon on top of character (if equipped and attacking)
        com.game.components.WeaponRenderComponent weaponRender = gameObject.getComponent(com.game.components.WeaponRenderComponent.class);
        if (weaponRender != null) {
            weaponRender.render(batch, gameObject);
        }
    }

    private void renderDebugStats() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        int fps = Gdx.graphics.getFramesPerSecond();
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        long memTotal = Runtime.getRuntime().totalMemory() / 1048576;

        float x = 10;
        float y = VIEWPORT_HEIGHT - 10;
        float lineHeight = 9;

        debugFont.draw(batch, "FPS: " + fps, x, y);
        debugFont.draw(batch, "Memory: " + memUsed + "/" + memTotal + " MB", x, y - lineHeight);

        // Show all players' positions
        int lineOffset = 2;
        for (PlayerEntity player : playerManager.getAllPlayers()) {
            Transform playerTransform = player.getTransform();
            float playerX = playerTransform.getX();
            float playerY = playerTransform.getY();
            debugFont.draw(batch, "Player " + player.getPlayerId() + " Pos: (" + (int)playerX + ", " + (int)playerY + ")",
                         x, y - lineHeight * lineOffset);
            lineOffset++;
        }

        debugFont.draw(batch, "Objects: " + world.getGameObjects().size(), x, y - lineHeight * lineOffset++);
        debugFont.draw(batch, "Time Scale: " + String.format("%.2fx", timeScale) + " (+/- to adjust, 0 to reset)", x, y - lineHeight * lineOffset++);
        debugFont.draw(batch, "Press F3 to toggle debug", x, y - lineHeight * lineOffset);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiCamera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        if (uiManager != null) {
            uiManager.resize(width, height);
        }
    }

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        // Stop multiplayer connections
        stopMultiplayer();

        batch.dispose();
        shapeRenderer.dispose();
        debugFont.dispose();
        damageFont.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (currentMap != null) currentMap.dispose();
        if (uiManager != null) uiManager.dispose();

        // Dispose audio resources
        SoundSystem.getInstance().dispose();
    }

    // ==================== GETTERS ====================

    public UIManagerNew getUiManager() {
        return uiManager;
    }

    public float getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(MIN_TIME_SCALE, Math.min(timeScale, MAX_TIME_SCALE));
        System.out.println("Time scale set to: " + this.timeScale + "x");
    }

    // ========== Furniture Placement System ==========

    /**
     * Enter furniture placement mode.
     * Called by UIManager when user selects "Place" on a furniture item.
     */
    private void enterFurniturePlacementMode(ItemStack furnitureItem, com.game.systems.ui.ItemSlotUI sourceSlot) {
        if (!furnitureItem.getDefinition().isFurniture()) {
            System.err.println("GameScreen: Cannot place non-furniture item");
            return;
        }

        furniturePlacementMode = true;
        placementFurnitureItem = furnitureItem;
        placementSourceSlot = sourceSlot;

        // Load preview texture
        String iconPath = furnitureItem.getDefinition().getIconPath();
        try {
            placementPreviewTexture = new Texture(iconPath);
            System.out.println("GameScreen: Entered furniture placement mode for " + furnitureItem.getDefinition().getName());
        } catch (Exception e) {
            System.err.println("GameScreen: Failed to load placement preview texture: " + iconPath);
            e.printStackTrace();
            exitFurniturePlacementMode(false);
        }
    }

    /**
     * Exit furniture placement mode.
     * @param placed true if furniture was successfully placed, false if canceled
     */
    private void exitFurniturePlacementMode(boolean placed) {
        if (!furniturePlacementMode) return;

        furniturePlacementMode = false;
        placementFurnitureItem = null;
        placementSourceSlot = null;

        if (placementPreviewTexture != null) {
            placementPreviewTexture.dispose();
            placementPreviewTexture = null;
        }

        if (placed) {
            System.out.println("GameScreen: Furniture placed successfully");
        } else {
            System.out.println("GameScreen: Furniture placement canceled");
        }
    }

    /**
     * Handle furniture placement input and rendering.
     * Called during render loop when placement mode is active.
     */
    private void handleFurniturePlacement(float delta) {
        if (!furniturePlacementMode || placementPreviewTexture == null) return;

        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Snap to 16x16 grid
        float snappedX = Math.round(mousePos.x / 16f) * 16f;
        float snappedY = Math.round(mousePos.y / 16f) * 16f;

        // Check if placement is valid (not on collision)
        boolean validPlacement = world.isPositionWalkable(snappedX, snappedY, 16, 16);

        // Render placement preview
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Set tint based on validity (green if valid, red if invalid)
        if (validPlacement) {
            batch.setColor(0.5f, 1f, 0.5f, 0.7f); // Green tint
        } else {
            batch.setColor(1f, 0.5f, 0.5f, 0.7f); // Red tint
        }

        batch.draw(placementPreviewTexture, snappedX, snappedY, 16, 16);
        batch.setColor(Color.WHITE); // Reset color
        batch.end();

        // Handle input
        if (Gdx.input.justTouched() && validPlacement) {
            // Place furniture at snapped position
            placeFurniture(snappedX, snappedY);
        } else if (inputManager.isJustPressed(InputAction.CANCEL) ||
                   Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            // Cancel placement
            uiManager.onPlacementCanceled();
            exitFurniturePlacementMode(false);
        }
    }

    /**
     * Place furniture at the specified world position.
     */
    private void placeFurniture(float x, float y) {
        if (placementFurnitureItem == null) return;

        ItemDefinition definition = placementFurnitureItem.getDefinition();
        String itemId = definition.getId();

        // Create furniture entity based on type
        // For now, all furniture is chests
        ChestEntity chest = new ChestEntity(itemId, definition, x, y);

        // Add to world
        world.addGameObject(chest);

        // Add to furniture manager for persistence
        String currentLevelName = currentLevelSource != null ? currentLevelSource.getLevelName() : "unknown";
        furnitureManager.placeFurniture(currentLevelName, chest);

        // Notify UI manager to remove item from inventory
        uiManager.onFurniturePlaced();

        // Exit placement mode
        exitFurniturePlacementMode(true);
    }

    /**
     * Handle interaction with nearby furniture.
     * Checks for furniture near the first player and opens UI or picks up.
     */
    private void handleFurnitureInteraction() {
        PlayerEntity player = playerManager.getFirstPlayer();
        if (player == null) return;

        // Find nearby furniture
        Vector2 playerPos = player.getTransform().getPosition();
        float interactionRange = 24f; // pixels

        com.game.systems.furniture.FurnitureEntity nearestFurniture = null;
        float nearestDistance = Float.MAX_VALUE;

        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof com.game.systems.furniture.FurnitureEntity furniture) {
                Vector2 furniturePos = new Vector2(
                    furniture.getTransform().getX(),
                    furniture.getTransform().getY()
                );

                float distance = playerPos.dst(furniturePos);
                if (distance < interactionRange && distance < nearestDistance) {
                    nearestFurniture = furniture;
                    nearestDistance = distance;
                }
            }
        }

        // Interact with nearest furniture
        if (nearestFurniture != null) {
            if (nearestFurniture instanceof ChestEntity chest) {
                // Toggle chest - close if it's already open, open if closed
                if (currentlyOpenChest == chest) {
                    closeChest(chest);
                } else {
                    // Close any other open chest first
                    if (currentlyOpenChest != null) {
                        closeChest(currentlyOpenChest);
                    }
                    openChest(chest);
                }
            } else {
                // Other furniture types - just call onInteract
                nearestFurniture.onInteract(player);
            }
        }
    }

    /**
     * Pick up furniture and return it to first player's inventory.
     */
    private void pickupFurniture(com.game.systems.furniture.FurnitureEntity furniture) {
        PlayerEntity player = playerManager.getFirstPlayer();
        if (player == null) return;

        // Create item stack for the furniture
        String itemId = furniture.getItemId();
        ItemStack furnitureItem = ItemFactory.create(itemId, 1);

        if (furnitureItem == null) {
            System.err.println("GameScreen: Failed to create item for furniture: " + itemId);
            return;
        }

        // Try to add to player inventory
        ItemStack remaining = player.getInventory().addItem(furnitureItem);
        if (remaining == null) {
            // Successfully added - remove from world
            world.removeGameObject(furniture);

            // Remove from furniture manager
            String currentLevelName = currentLevelSource != null ? currentLevelSource.getLevelName() : "unknown";
            furnitureManager.removeFurniture(currentLevelName, furniture);

            System.out.println("GameScreen: Picked up furniture: " + itemId);
        } else {
            System.out.println("GameScreen: Inventory full, cannot pick up furniture");
        }
    }

    /**
     * Open a chest and show its inventory UI.
     */
    private void openChest(ChestEntity chest) {
        if (uiManager != null) {
            uiManager.openChest(chest);
            uiManager.refreshAllWindows(); // Refresh to load item icons
            currentlyOpenChest = chest;
            System.out.println("GameScreen: Opened chest");
        }
    }

    /**
     * Close a chest UI.
     */
    private void closeChest(ChestEntity chest) {
        if (uiManager != null) {
            uiManager.closeChest(chest);
            if (currentlyOpenChest == chest) {
                currentlyOpenChest = null;
            }
            System.out.println("GameScreen: Closed chest");
        }
    }

    /**
     * Check if all players have moved too far from open chest and auto-close it.
     * Call this every frame in update loop.
     */
    private void updateOpenChestDistance() {
        if (currentlyOpenChest != null && playerManager.hasPlayers()) {
            Vector2 chestPos = new Vector2(
                currentlyOpenChest.getTransform().getX(),
                currentlyOpenChest.getTransform().getY()
            );

            // Check if ANY player is still in range
            boolean anyPlayerNear = false;
            for (PlayerEntity player : playerManager.getAllPlayers()) {
                Vector2 playerPos = player.getTransform().getPosition();
                float distance = playerPos.dst(chestPos);
                if (distance <= CHEST_AUTO_CLOSE_DISTANCE) {
                    anyPlayerNear = true;
                    break;
                }
            }

            // Close chest if no players are nearby
            if (!anyPlayerNear) {
                closeChest(currentlyOpenChest);
                System.out.println("GameScreen: Auto-closed chest (all players moved away)");
            }
        }
    }

    // ========== Save/Load Support ==========

    /**
     * Load game state from save data.
     * Reloads the level and applies all saved state.
     */
    private void loadFromSaveData(com.game.save.SaveData saveData) {
        if (saveData == null || saveData.world == null) {
            System.err.println("GameScreen: Cannot load - invalid save data");
            return;
        }

        // Close any open chests/UI before reloading
        if (currentlyOpenChest != null) {
            uiManager.closeChest(currentlyOpenChest);
            currentlyOpenChest = null;
        }

        // IMPORTANT: Import furniture data BEFORE loading the level
        // This is because loadLevel() calls furnitureManager.loadFurnitureIntoWorld()
        // which needs the furniture data to already be in the manager
        if (saveData.world != null && saveData.world.furnitureByLevel != null) {
            furnitureManager.importSaveData(saveData.world.furnitureByLevel);
        }

        // First, reload the level (this will create a new world)
        String levelId = saveData.world.currentLevelId;
        String levelType = saveData.world.levelType;

        if (levelType.equals("dungeon")) {
            System.out.println("GameScreen: Cannot load dungeon levels (they are temporary)");
            // Fallback to default map
            levelId = "Maps/prototype.tmx";
        }

        // Reload the level (will load furniture from manager into world)
        loadLevel(levelId, null);

        // Now apply the rest of the save data (player, inventory, dropped items)
        com.game.save.SaveManager.getInstance().applySaveData(saveData);

        // Refresh UI to show loaded inventory
        uiManager.refreshAllWindows();

        System.out.println("GameScreen: Loaded save - Level: " + levelId +
                         ", Position: (" + saveData.player.x + ", " + saveData.player.y + ")");
    }

    // ========== Networking Support ==========

    /**
     * Start hosting a multiplayer game (Open to LAN).
     * The current game becomes a server that others can connect to.
     */
    public void startHosting() {
        if (isHost || isClient) {
            System.out.println("GameScreen: Already in multiplayer mode");
            return;
        }

        isHost = true;
        localPlayerId = 0; // Host is always Player 0

        // Ensure the existing player is assigned Player 0
        PlayerEntity existingPlayer = playerManager.getFirstPlayer();
        if (existingPlayer != null) {
            existingPlayer.setPlayerId(0);
            System.out.println("GameScreen: Set existing player as host (Player 0)");
        }

        // Create and start server
        gameServer = new com.game.networking.GameServer();

        // Set up server callbacks
        gameServer.setClientConnectedCallback((clientId, playerName) -> {
            System.out.println("GameScreen: Client " + clientId + " connected: " + playerName);

            // Create a new player for the connected client
            Gdx.app.postRunnable(() -> {
                if (world != null && playerManager != null) {
                    // Get spawn position from first player
                    PlayerEntity firstPlayer = playerManager.getFirstPlayer();
                    float spawnX = 50;
                    float spawnY = 50;
                    if (firstPlayer != null) {
                        spawnX = firstPlayer.getTransform().getX() + 32; // Spawn near host
                        spawnY = firstPlayer.getTransform().getY();
                    }

                    // Create new player with NetworkInputSource
                    PlayerEntity newPlayer = new PlayerEntity(world, spawnX, spawnY);
                    com.game.networking.NetworkInputSource networkInput = new com.game.networking.NetworkInputSource();
                    newPlayer.setInputSource(networkInput);

                    // Set damage number callback
                    newPlayer.setDamageNumberCallback((x, y, damage) -> {
                        DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
                        damageNumbers.add(damageNumber);
                    });

                    // CRITICAL: Set player ID to match client ID before adding to manager
                    newPlayer.setPlayerId(clientId);
                    playerManager.addPlayerWithId(newPlayer);  // Use addPlayerWithId to preserve ID
                    world.addGameObject(newPlayer);

                    System.out.println("GameScreen: Created remote player " + clientId + " for connected client");
                }
            });
        });

        gameServer.setClientDisconnectedCallback((clientId, reason) -> {
            System.out.println("GameScreen: Client " + clientId + " disconnected: " + reason);
            // TODO: Remove player from game
        });

        gameServer.setInputReceivedCallback((clientId, inputPacket) -> {
            // Apply input to the player
            Gdx.app.postRunnable(() -> {
                PlayerEntity player = playerManager.getPlayerById(clientId);
                if (player != null && player.getInputSource() instanceof com.game.networking.NetworkInputSource) {
                    com.game.networking.NetworkInputSource networkInput =
                        (com.game.networking.NetworkInputSource) player.getInputSource();
                    networkInput.updateFromPacket(inputPacket);
                }
            });
        });

        gameServer.start();

        System.out.println("GameScreen: Hosting on port " + gameServer.getPort());
        System.out.println("GameScreen: Other players can connect using 'localhost' or your LAN IP");
    }

    /**
     * Set up an already-connected game client (called from main menu).
     * @param client The already-connected GameClient
     */
    public void setGameClient(com.game.networking.GameClient client) {
        if (isHost || (isClient && gameClient != null)) {
            System.out.println("GameScreen: Already in multiplayer mode");
            return;
        }

        isClient = true;
        gameClient = client;

        System.out.println("GameScreen: Setting up pre-connected client...");

        // Check if client already received player ID during initial connection
        int existingPlayerId = gameClient.getAssignedPlayerId();
        if (existingPlayerId != -1) {
            System.out.println("GameScreen: Client already has player ID: " + existingPlayerId);
            localPlayerId = existingPlayerId;

            // Create the local player immediately
            createLocalPlayer(existingPlayerId);
        }

        // Set up client callbacks for future events
        setupClientCallbacks();

        System.out.println("GameScreen: Client callbacks configured");
    }

    /**
     * Create the local controllable player for multiplayer.
     * @param playerId The player ID assigned by the server
     */
    private void createLocalPlayer(int playerId) {
        if (world == null) {
            System.err.println("GameScreen: Cannot create local player - world not loaded yet");
            return;
        }

        // Get spawn position from the loaded level
        float spawnX = 50;
        float spawnY = 750;

        if (currentLevelSource != null) {
            com.game.systems.level.LevelData levelData = currentLevelSource.getLevelData();
            com.game.systems.level.LevelData.SpawnPoint spawn = levelData.getDefaultSpawnPoint();

            if (spawn != null) {
                spawnX = spawn.getX();
                spawnY = spawn.getY();

                // Convert to grid and back to match loadLevel behavior
                int spawnGridX = (int)(spawnX / world.getTileSize());
                int spawnGridY = (int)(spawnY / world.getTileSize());
                spawnX = spawnGridX * world.getTileSize();
                spawnY = spawnGridY * world.getTileSize();
            }
        }

        System.out.println("GameScreen: Creating local player " + playerId + " at (" + spawnX + ", " + spawnY + ")");

        // Create local controllable player with assigned ID
        PlayerEntity localPlayer = new PlayerEntity(world, spawnX, spawnY);
        LocalKeyboardInput input = LocalKeyboardInput.createPlayer1();
        input.setCamera(camera);
        input.setPlayerTransform(localPlayer.getTransform());
        localPlayer.setInputSource(input);

        // Set damage number callback
        localPlayer.setDamageNumberCallback((x, y, damage) -> {
            DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
            damageNumbers.add(damageNumber);
        });

        // Manually set the player ID to match server assignment
        localPlayer.setPlayerId(playerId);
        playerManager.addPlayerWithId(localPlayer);  // Use addPlayerWithId to preserve ID
        world.addGameObject(localPlayer);

        System.out.println("GameScreen: Created local player with ID: " + playerId);
    }

    /**
     * Setup client callbacks for an existing gameClient.
     */
    private void setupClientCallbacks() {
        gameClient.setConnectionCallback((assignedPlayerId) -> {
            System.out.println("GameScreen: Connected to server with player ID: " + assignedPlayerId);
            localPlayerId = assignedPlayerId;

            // Create the local player on the main thread
            Gdx.app.postRunnable(() -> {
                createLocalPlayer(assignedPlayerId);
            });
        });

        gameClient.setDisconnectionCallback((reason) -> {
            System.out.println("GameScreen: Disconnected from server: " + reason);
            isClient = false;
            // TODO: Return to main menu or show error
        });

        gameClient.setStateUpdateCallback((statePacket) -> {
            // Apply state update from server
            Gdx.app.postRunnable(() -> {
                applyStateUpdate(statePacket);
            });
        });

        gameClient.setPlayerJoinCallback((playerId, playerName) -> {
            System.out.println("GameScreen: Player " + playerId + " joined: " + playerName);
            // Players are managed by the server and state updates
        });
    }

    /**
     * Connect to a multiplayer game as a client (legacy method - prefer setGameClient).
     * @param serverIp Server IP address to connect to
     * @deprecated Use setGameClient with a pre-connected client for better error handling
     */
    public void connectToServer(String serverIp) {
        if (isHost || isClient) {
            System.out.println("GameScreen: Already in multiplayer mode");
            return;
        }

        isClient = true;

        // Create and connect client
        gameClient = new com.game.networking.GameClient();

        // Set up client callbacks
        setupClientCallbacks();

        // Connect to server
        boolean success = gameClient.connect(serverIp);
        if (!success) {
            System.err.println("GameScreen: Failed to connect to server: " + serverIp);
            isClient = false;
            // TODO: Show error dialog
        }
    }

    /**
     * Update networking - called every frame.
     * Sends input to server (client) or broadcasts state (host).
     */
    private void updateNetworking(float delta) {
        inputSendTimer += delta;
        stateSendTimer += delta;

        if (isClient && gameClient != null && gameClient.isConnected()) {
            // Send input to server at high frequency (every frame)
            if (inputSendTimer >= INPUT_SEND_INTERVAL) {
                sendInputToServer();
                inputSendTimer = 0f;
            }
        } else if (isHost && gameServer != null && gameServer.isRunning()) {
            // Broadcast state to all clients at lower frequency (20Hz)
            if (stateSendTimer >= STATE_SEND_INTERVAL) {
                broadcastStateToClients();
                stateSendTimer = 0f;
            }
        }
    }

    /**
     * Send local player input to server (client mode).
     */
    private void sendInputToServer() {
        // Get the local controllable player by ID
        PlayerEntity localPlayer = playerManager.getPlayerById(localPlayerId);
        if (localPlayer == null) {
            System.err.println("GameScreen: Cannot send input - local player " + localPlayerId + " not found");
            return;
        }

        com.game.systems.input.InputSource input = localPlayer.getInputSource();
        if (input == null) return;

        com.badlogic.gdx.math.Vector2 movement = input.getMovementInput();
        com.badlogic.gdx.math.Vector2 aim = input.getAimDirection();

        gameClient.sendInput(
            movement.x, movement.y,
            input.isAttackPressed(), input.isAttackJustPressed(),
            aim.x, aim.y,
            input.isRunning()
        );
    }

    /**
     * Broadcast game state to all clients (host mode).
     */
    private void broadcastStateToClients() {
        if (gameServer == null || !gameServer.isRunning()) return;

        com.game.networking.StateUpdatePacket statePacket = new com.game.networking.StateUpdatePacket();

        // Add all player states
        for (PlayerEntity player : playerManager.getAllPlayers()) {
            int playerId = player.getPlayerId();
            com.badlogic.gdx.math.Vector2 pos = player.getTransform().getPosition();
            com.game.components.HealthComponent health = player.getHealthComponent();

            // Get animation state, direction, and flip from AnimationComponent
            com.game.components.AnimationComponent animComp = player.getComponent(com.game.components.AnimationComponent.class);
            String currentAnimation = "idle";
            int currentDirection = 180; // Default: facing down
            boolean flipX = false;

            if (animComp != null) {
                currentAnimation = animComp.getCurrentState();
                currentDirection = animComp.getCurrentDirection();

                // Get flip state from the animator
                com.game.systems.animation.SpriteAnimator animator = animComp.getAnimator();
                if (animator != null) {
                    flipX = animator.isFlipX();
                }
            }

            com.game.networking.StateUpdatePacket.PlayerState playerState =
                new com.game.networking.StateUpdatePacket.PlayerState(
                    pos.x, pos.y,
                    health.getCurrentHealth(), health.getMaxHealth(),
                    currentAnimation, currentDirection, flipX
                );

            statePacket.addPlayerState(playerId, playerState);
        }

        gameServer.broadcastPacket(statePacket);
    }

    /**
     * Apply state update from server (client mode).
     * Uses client prediction with periodic checkpoints:
     * - Local player: only corrects if prediction error exceeds threshold
     * - Remote players: applies server state directly
     */
    private void applyStateUpdate(com.game.networking.StateUpdatePacket statePacket) {
        if (world == null) return;

        // Iterate through all player states from server
        for (java.util.Map.Entry<Integer, com.game.networking.StateUpdatePacket.PlayerState> entry :
             statePacket.getPlayerStates().entrySet()) {

            int playerId = entry.getKey();
            com.game.networking.StateUpdatePacket.PlayerState state = entry.getValue();

            // Find or create the player
            PlayerEntity player = playerManager.getPlayerById(playerId);

            if (player == null) {
                // Create a new player (server told us about them)
                player = new PlayerEntity(world, state.x, state.y);
                player.setPlayerId(playerId);

                // Mark as network-controlled (remote player - puppet controlled by server)
                player.setNetworkControlled(true);

                // Set damage number callback
                player.setDamageNumberCallback((x, y, damage) -> {
                    DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
                    damageNumbers.add(damageNumber);
                });

                // Remote players have no input source - they're controlled by server state
                // Local player already has LocalKeyboardInput from createLocalPlayer()

                playerManager.addPlayerWithId(player);
                world.addGameObject(player);

                System.out.println("GameScreen: Created remote player " + playerId + " as network-controlled puppet");
            }

            // CLIENT PREDICTION: Handle local player differently
            boolean isLocalPlayer = (playerId == localPlayerId);

            if (isLocalPlayer) {
                // For local player: use client prediction with periodic checkpoints
                com.badlogic.gdx.math.Vector2 currentPos = player.getTransform().getPosition();
                float dx = state.x - currentPos.x;
                float dy = state.y - currentPos.y;
                float distanceSquared = dx * dx + dy * dy;
                float distance = (float) Math.sqrt(distanceSquared);

                // Only correct if prediction error exceeds threshold
                if (distance > POSITION_CORRECTION_THRESHOLD) {
                    // Smoothly interpolate towards server position
                    float newX = currentPos.x + dx * CORRECTION_SPEED;
                    float newY = currentPos.y + dy * CORRECTION_SPEED;
                    player.getTransform().setPosition(newX, newY);

                    System.out.println("GameScreen: Correcting local player position by " +
                                     String.format("%.2f", distance) + " pixels");
                }
                // If within threshold: do nothing, client prediction was accurate!

            } else {
                // For remote players (network-controlled): enqueue snapshot for interpolation
                // Calculate velocity from previous position
                com.badlogic.gdx.math.Vector2 currentPos = player.getTransform().getPosition();
                float deltaX = state.x - currentPos.x;
                float deltaY = state.y - currentPos.y;
                float vx = deltaX / STATE_SEND_INTERVAL;
                float vy = deltaY / STATE_SEND_INTERVAL;

                // Create snapshot with current timestamp
                long timestamp = System.currentTimeMillis();
                com.game.networking.EntitySnapshot snapshot = new com.game.networking.EntitySnapshot(
                    timestamp,
                    state.x, state.y,
                    vx, vy,
                    state.currentAnimation,
                    state.currentDirection,
                    state.flipX,
                    state.health,
                    state.maxHealth
                );

                // Enqueue snapshot for interpolation (rendering will use this)
                player.enqueueSnapshot(snapshot);

                // Also update health directly (trust server for health)
                com.game.components.HealthComponent health = player.getHealthComponent();
                if (health != null) {
                    health.setHealth(state.health);
                    health.setMaxHealth(state.maxHealth);
                }
            }

            // Local player health is also updated from server
            if (isLocalPlayer) {
                com.game.components.HealthComponent health = player.getHealthComponent();
                if (health != null) {
                    health.setHealth(state.health);
                    health.setMaxHealth(state.maxHealth);
                }
            }
        }
    }

    /**
     * Stop hosting or disconnect from server.
     */
    public void stopMultiplayer() {
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }

        if (gameClient != null) {
            gameClient.disconnect();
            gameClient = null;
        }

        isHost = false;
        isClient = false;
    }
}
