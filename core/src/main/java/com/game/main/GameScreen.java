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
import com.game.systems.entity.entities.DamageNumberEntity;
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

    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;

    public WorldManager world;
    public WorldItemManager worldItemManager;
    public PlayerEntity player;
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

    public GameScreen() {
        // Create cameras
        camera = new OrthographicCamera();
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        debugFont = new BitmapFont();
        debugFont.setColor(1, 1, 0, 1);
        debugFont.getData().setScale(0.5f);

        // Separate font for damage numbers to avoid interfering with debug text
        damageFont = new BitmapFont();
        damageFont.setColor(1, 1, 1, 1);

        // Initialize systems
        worldItemManager = new WorldItemManager();
        inputManager = new InputManager();
        debugManager = new DebugManager();
        furnitureManager = FurnitureManager.getInstance();
        damageNumbers = new java.util.ArrayList<>();
        deathAnimations = new java.util.ArrayList<>();
        destructionParticles = new java.util.ArrayList<>();

        // Initialize level loading systems
        dungeonController = new com.game.systems.dungeon.DungeonController();
        dungeonDebugRenderer = new com.game.systems.dungeon.DungeonDebugRenderer();
        dungeonDebugRenderer.setShowAll(false); // Off by default, toggle with F3

        // Initialize LootSystem singleton
        com.game.systems.loot.LootSystem.initialize(worldItemManager);

        // Initialize BreakableObjectRegistry
        com.game.systems.breakable.BreakableObjectRegistry.loadConfigs();

        // Register test items
        TestItems.registerTestItems();
        TestItems.loadTextures(worldItemManager);

        // Load initial level
        loadLevel("Maps/prototype.tmx", null);
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

        // Disable player movement when console is open
        if (player != null) {
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

        // Update UI
        if (uiManager != null) {
            uiManager.update(delta);
        }

        // Update camera
        updateCamera();

        // Render map with Y-sorting
        mapRenderer.setView(camera);
        batch.setProjectionMatrix(camera.combined);

        if (ySortRenderer != null) {
            // Y-sorted rendering (entities sorted with feature layers)
            ySortRenderer.render(batch, world.getGameObjects(), this::renderEntity);
        } else {
            // Fallback: render map then entities (no Y-sorting)
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

        // Handle furniture placement mode
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
     * Updates item magnetism - registers nearby items with player's magnet component.
     */
    private void updateItemMagnetism() {
        if (player == null) return;

        Vector2 playerPos = player.getTransform().getPosition();
        float magnetRadius = player.getItemMagnet().getMagnetRadius();

        // Get nearby items
        for (ItemPickupEntity item : worldItemManager.getItemsNear(playerPos, magnetRadius)) {
            player.getItemMagnet().registerItem(item);
        }
    }

    /**
     * Checks for item pickup collisions with player.
     */
    private void checkItemPickups() {
        if (player == null) return;

        Transform playerTransform = player.getTransform();
        ColliderComponent playerCollider = player.getEnvironmentCollider();

        if (playerCollider == null) return;

        Rectangle playerBounds = playerCollider.getBounds(player);

        boolean inventoryChanged = false;

        // Check all items
        for (ItemPickupEntity item : worldItemManager.getAllItems()) {
            if (!item.canPickup() || !item.isActive()) continue;

            Transform itemTransform = item.getComponent(Transform.class);
            if (itemTransform == null) continue;

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
                    SoundSystem.getInstance().playSound(COIN_PICKUP,0.6f);

                    System.out.println("Picked up: " + itemStack.toString());
                    inventoryChanged = true;
                } else if (remaining.getQuantity() < itemStack.getQuantity()) {
                    // Partial pickup
                    item.getItemStack().setQuantity(remaining.getQuantity());

                    // Play pickup sound
                    SoundSystem.getInstance().playSound(COIN_PICKUP,0.6f);

                    inventoryChanged = true;
                }
            }
        }

        // Notify UI if inventory changed
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

        System.out.println("Spawning player at: (" + spawnX + ", " + spawnY + ") - Grid: (" + spawnGridX + ", " + spawnGridY + ")");

        // Create or update player
        if (player == null) {
            com.game.systems.entity.entities.PlayerEntity.initialize(world, spawnX, spawnY);
            player = com.game.systems.entity.entities.PlayerEntity.getInstance();
        } else {
            player.setWorld(world);
            player.getTransform().setPosition(spawnX, spawnY);
        }

        // Set camera for mouse position tracking
        player.setCamera(camera);

        // Set damage number callback
        player.setDamageNumberCallback((x, y, damage) -> {
            DamageNumberEntity damageNumber = new DamageNumberEntity(x, y, damage, damageFont);
            damageNumbers.add(damageNumber);
        });

        world.addGameObject(player);

        // Initialize UI manager (if not already initialized)
        if (uiManager == null) {
            uiManager = new UIManagerNew(player.getInventory(), worldItemManager);
            uiManager.setItemDropCallback(itemStack -> {
                Vector2 playerPos = player.getTransform().getPosition();
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

            uiManager.setPlayerHealth(player.getHealthComponent());
            uiManager.setPlayer(player);

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
        if (player == null) return;

        Transform playerTransform = player.getTransform();
        ColliderComponent playerCollider = player.getComponent(ColliderComponent.class);

        if (playerCollider == null) return;

        Rectangle playerBounds = playerCollider.getBounds(player);

        // Check all gateways
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof GatewayEntity) {
                GatewayEntity gateway = (GatewayEntity) obj;
                ColliderComponent gatewayCollider = gateway.getComponent(ColliderComponent.class);

                if (gatewayCollider != null) {
                    Rectangle gatewayBounds = gatewayCollider.getBounds(gateway);
                    if (playerBounds.overlaps(gatewayBounds)) {
                        pendingGateway = gateway;
                        return;
                    }
                }
            }
        }
    }

    private void updateCamera() {
        // Apply camera scale from settings
        if (uiManager != null) {
            float cameraScale = uiManager.getGameSettings().getCameraScale();
            camera.zoom = 1f / cameraScale; // Higher scale = zoomed in (lower zoom value)
        }

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

        // Render player colliders
        if (player != null) {
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

        Transform playerTransform = player.getTransform();
        float playerX = playerTransform.getX();
        float playerY = playerTransform.getY();

        float x = 10;
        float y = VIEWPORT_HEIGHT - 10;
        float lineHeight = 9;

        debugFont.draw(batch, "FPS: " + fps, x, y);
        debugFont.draw(batch, "Memory: " + memUsed + "/" + memTotal + " MB", x, y - lineHeight);
        debugFont.draw(batch, "Player Pos: (" + (int)playerX + ", " + (int)playerY + ")", x, y - lineHeight * 2);
        debugFont.draw(batch, "Objects: " + world.getGameObjects().size(), x, y - lineHeight * 3);
        debugFont.draw(batch, "Time Scale: " + String.format("%.2fx", timeScale) + " (+/- to adjust, 0 to reset)", x, y - lineHeight * 4);
        debugFont.draw(batch, "Press F3 to toggle debug", x, y - lineHeight * 5);

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
     * Checks for furniture near the player and opens UI or picks up.
     */
    private void handleFurnitureInteraction() {
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
     * Pick up furniture and return it to player inventory.
     */
    private void pickupFurniture(com.game.systems.furniture.FurnitureEntity furniture) {
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
     * Check if player has moved too far from open chest and auto-close it.
     * Call this every frame in update loop.
     */
    private void updateOpenChestDistance() {
        if (currentlyOpenChest != null && player != null) {
            Vector2 playerPos = player.getTransform().getPosition();
            Vector2 chestPos = new Vector2(
                currentlyOpenChest.getTransform().getX(),
                currentlyOpenChest.getTransform().getY()
            );

            float distance = playerPos.dst(chestPos);
            if (distance > CHEST_AUTO_CLOSE_DISTANCE) {
                closeChest(currentlyOpenChest);
                System.out.println("GameScreen: Auto-closed chest (player moved away)");
            }
        }
    }
}
