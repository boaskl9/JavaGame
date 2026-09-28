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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
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
import com.game.networking.PlayerDataCodec;
import com.game.world.LevelInstance;
import com.game.world.GameWorld;

import static com.game.systems.audio.SoundRegistry.*;

/**
 * Refactored GameScreen using the new decoupled architecture.
 * All systems are now independent and reusable.
 */
public class GameScreen implements Screen, GameWorld.Presenter {
    private static final int VIEWPORT_WIDTH = 640;
    private static final int VIEWPORT_HEIGHT = 360;

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

    // Longest simulation step per frame; bigger hitches (e.g. while loading a level) are clamped
    // so entities can't tunnel through walls
    private static final float MAX_FRAME_DELTA = 1f / 20f;

    // The simulation (levels, players, multiplayer); this screen draws it and runs the UI
    private final GameWorld gameWorld;
    public WorldManager world; // World of the current level (what this machine sees)
    private com.game.systems.level.LevelSource currentLevelSource;

    public WorldItemManager worldItemManager;
    public PlayerManager playerManager;
    private PlayerEntity localPlayer; // The player controlled on this machine (owned by gameWorld)
    private OrthogonalTiledMapRenderer mapRenderer;
    private YSortRenderer ySortRenderer;
    private UIManagerNew uiManager;
    private InputManager inputManager;

    private DebugManager debugManager;
    private DebugConsole debugConsole;

    // Level loading abstraction
    private com.game.systems.dungeon.DungeonController dungeonController;
    private com.game.systems.dungeon.DungeonDebugRenderer dungeonDebugRenderer;

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
    private boolean placementPending = false; // Waiting for the host to confirm (guests)
    private ChestEntity chestPendingOpen = null; // Waiting for permission to use this chest
    private FurnitureManager furnitureManager;

    // Currently open chest (for auto-closing)
    private ChestEntity currentlyOpenChest = null;
    private static final float CHEST_AUTO_CLOSE_DISTANCE = 32f; // pixels

    // Save name tracking for auto-save
    private String currentSaveName = null;

    private boolean screenClosed = false;

    // Joining a host: character selection, shown until the local player exists
    private com.badlogic.gdx.scenes.scene2d.Stage joinStage;
    private com.badlogic.gdx.scenes.scene2d.ui.Skin joinSkin;
    private com.game.ui.CharacterSelectDialog characterSelect;
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
        // Create camera and viewport
        // ExtendViewport shows more of the game world instead of adding black bars
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
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
        gameWorld = new GameWorld(clientMode, worldItemManager, playerManager, this);

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
        } else if (!clientMode) {
            // Load initial level for new game
            gameWorld.changeLevel(GameWorld.START_LEVEL, null);
        }
        // Client mode: the level is built when the host's welcome arrives
    }

    @Override
    public void render(float delta) {
        // Network first: packets may build the level (client join) or move players around
        gameWorld.updateNetwork(delta);
        if (screenClosed) {
            return;
        }

        // Client still waiting for the host to tell us where we are
        if (gameWorld.getCurrentInstance() == null) {
            renderConnecting();
            return;
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

        // Disable local movement when console is open
        if (localPlayer != null) {
            localPlayer.setInputEnabled(!consoleOpen);
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
        float scaledDelta = Math.min(delta, MAX_FRAME_DELTA) * timeScale;

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Simulate: levels (incl. world items), item pickups, gateways
        gameWorld.update(scaledDelta);

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

        // Handle furniture placement mode
        if (furniturePlacementMode) {
            handleFurniturePlacement(delta);
        }

        // Render UI
        if (uiManager != null) {
            uiManager.render();
        }

        if (gameWorld.isLocalPlayerDead()) {
            renderKnockedOut();
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
     * Dim the screen while the local player is down, until they respawn.
     */
    private void renderKnockedOut() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.55f);
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        String text = "You were knocked out...";
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(debugFont, text);
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        debugFont.draw(batch, text, (VIEWPORT_WIDTH - layout.width) / 2f, (VIEWPORT_HEIGHT + layout.height) / 2f);
        batch.end();
    }

    @Override
    public void onLocalPlayerDied() {
        if (!placementPending) {
            exitFurniturePlacementMode(false);
        }
    }

    @Override
    public void onLocalPlayerRespawned() {
    }

    /**
     * Shown on a client between connecting and the host's welcome: the character selection.
     */
    private void renderConnecting() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (joinStage != null) {
            joinStage.act(Gdx.graphics.getDeltaTime());
            joinStage.draw();
            return;
        }
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        debugFont.draw(batch, "Joining game...", VIEWPORT_WIDTH / 2f - 30, VIEWPORT_HEIGHT / 2f);
        batch.end();
    }

    @Override
    public void chooseCharacter(com.game.networking.Packets.CharacterInfo[] characters, String message) {
        if (characterSelect == null) {
            joinStage = new com.badlogic.gdx.scenes.scene2d.Stage(new com.badlogic.gdx.utils.viewport.ScreenViewport());
            joinSkin = new com.badlogic.gdx.scenes.scene2d.ui.Skin(Gdx.files.internal("assets/ui/wood-theme.json"));
            Gdx.input.setInputProcessor(joinStage);
            characterSelect = new com.game.ui.CharacterSelectDialog(joinSkin, new com.game.ui.CharacterSelectDialog.CharacterSelectCallback() {
                @Override
                public void onPlay(String characterId) {
                    gameWorld.playCharacter(characterId);
                }

                @Override
                public void onCreate(String name) {
                    gameWorld.createCharacter(name);
                }

                @Override
                public void onLeave() {
                    stopMultiplayer();
                    returnToMainMenu(null);
                }
            });
            characterSelect.setCharacters(characters, message);
            characterSelect.show(joinStage);
        } else {
            characterSelect.setCharacters(characters, message);
        }
    }

    private void disposeJoinUi() {
        if (joinStage == null) return;
        joinStage.dispose();
        joinSkin.dispose();
        joinStage = null;
        joinSkin = null;
        characterSelect = null;
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
        if (debugMode && !gameWorld.isGuest() && inputManager.isJustPressed(InputAction.DEBUG_SAVE)) {
            gameWorld.respawnIfDead(); // Never save a knocked-out player
            com.game.save.SaveManager.getInstance().save("debug_save");
            System.out.println("=== SAVED GAME (F6) ===");
        }

        // Debug: Load game
        if (debugMode && gameWorld.getSession() == null && inputManager.isJustPressed(InputAction.DEBUG_LOAD)) {
            com.game.save.SaveData saveData = com.game.save.SaveManager.getInstance().load("debug_save");
            if (saveData != null) {
                gameWorld.respawnIfDead(); // Otherwise the pending respawn would move the loaded player
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
            gameWorld.dropItem(itemStack, mousePos.x, mousePos.y);
            System.out.println("Spawned " + itemId + " at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
        }
    }

    /**
     * Debug function: Spawns an enemy at mouse position.
     * @param enemyType The enemy type to spawn (slime, frog, cat)
     */
    private void spawnDebugEnemy(String enemyType) {
        if (isGuest()) {
            System.out.println("Only the host can spawn enemies");
            return;
        }

        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Create enemy based on type (callbacks are attached when it enters the world)
        EnemyEntity enemy = null;
        switch (enemyType.toLowerCase()) {
            case "slime":
                enemy = new LizardEnemy(world, mousePos.x, mousePos.y);
                break;

            case "frog":
                enemy = new Axolot(world, mousePos.x, mousePos.y);
                break;

            case "cat":
                enemy = new CatEnemy(world, mousePos.x, mousePos.y);
                break;

            default:
                System.out.println("Unknown enemy type: " + enemyType);
                break;
        }

        if (enemy != null) {
            world.addGameObject(enemy);
            System.out.println("Spawned " + enemyType + " at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
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
        if (isGuest()) {
            System.out.println("Only the host can spawn breakables");
            return;
        }

        // Get mouse position in world coordinates
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        // Create breakable object using factory (callbacks are attached when it enters the world)
        BreakableEntity breakable = com.game.systems.breakable.BreakableObjectFactory.create(
            objectType,
            mousePos.x,
            mousePos.y
        );

        if (breakable != null) {
            world.addGameObject(breakable);
            System.out.println("Spawned " + objectType + " at: (" + (int)mousePos.x + ", " + (int)mousePos.y + ")");
        } else {
            System.out.println("Failed to spawn breakable: " + objectType);
        }
    }

    /**
     * Load an assembled dungeon directly (from dungeon generation system).
     * Public so DebugConsole can access it.
     */
    public void loadAssembledDungeon(com.game.systems.dungeon.assembly.AssembledDungeon dungeon) {
        if (isGuest()) {
            System.out.println("GameScreen: Only the host can load dungeons");
            return;
        }
        // Dungeons are always built fresh
        gameWorld.enterNewLevel(new com.game.systems.dungeon.DungeonLevelSource(dungeon, null));
    }

    /**
     * Get the dungeon controller for external access (e.g., DebugConsole).
     * @return The dungeon controller
     */
    public com.game.systems.dungeon.DungeonController getDungeonController() {
        return dungeonController;
    }

    // ========== Level Management ==========

    // ========== GameWorld.Presenter ==========

    @Override
    public com.game.systems.input.InputSource createLocalInput(PlayerEntity player) {
        LocalKeyboardInput input = LocalKeyboardInput.createPlayer1();
        input.setCamera(camera);
        input.setPlayerTransform(player.getTransform());
        return input;
    }

    @Override
    public void onLeavingLevel() {
        // Close any open chest; it belongs to the old level
        if (currentlyOpenChest != null && uiManager != null) {
            closeChest(currentlyOpenChest);
        }
    }

    @Override
    public void onLocalPlayerCreated(PlayerEntity player) {
        localPlayer = player;
        disposeJoinUi();
        if (uiManager == null) {
            initLocalPlayerUI(player);
        }
    }

    @Override
    public void onLocalPlayerDataRestored(PlayerEntity player) {
        player.updateWeaponSprite();
        uiManager.refreshAllWindows();
    }

    /**
     * A short floating message in the world (e.g. "In use" above a chest).
     */
    private void showWorldMessage(float x, float y, String text, Color color) {
        damageNumbers.add(new DamageNumberEntity(x, y, text, color, damageFont));
    }

    @Override
    public void showParticles(float x, float y, String particleType) {
        destructionParticles.add(new DestructionParticleEntity(x, y, particleType));
    }

    /**
     * The local player now stands in this level: render it.
     */
    @Override
    public void onLevelEntered(LevelInstance instance) {
        world = instance.getWorld();
        currentLevelSource = instance.getSource();

        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        mapRenderer = new OrthogonalTiledMapRenderer(instance.getTiledMap());
        ySortRenderer = new YSortRenderer(mapRenderer, instance.getTiledMap());
        ySortRenderer.setDebugMode(debugMode);
    }

    /**
     * Build the HUD, inventory windows, debug console etc. for the local player.
     */
    private void initLocalPlayerUI(PlayerEntity player) {
        uiManager = new UIManagerNew(player.getInventory(), worldItemManager);

        // IMPORTANT: Resize the UI manager to match current window size
        uiManager.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uiManager.setItemDropCallback(itemStack -> {
            Vector2 playerPos = localPlayer.getTransform().getPosition();
            gameWorld.dropItem(itemStack, playerPos.x, playerPos.y);
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

        uiManager.setPauseMenuCallback(new UIManagerNew.PauseMenuCallback() {
            @Override
            public void onOpenToLAN() {
                startHosting();
            }

            @Override
            public void onReturnToMainMenu() {
                // Wake up first, so neither our save nor the host's copy of us is knocked out
                gameWorld.respawnIfDead();

                // Stop multiplayer before returning
                stopMultiplayer();

                // Auto-save before returning to main menu
                if (currentSaveName != null) {
                    com.game.save.SaveManager.getInstance().save(currentSaveName);
                    System.out.println("GameScreen: Auto-saved to: " + currentSaveName);
                } else {
                    System.out.println("GameScreen: No save name set, skipping auto-save");
                }

                returnToMainMenu(null);
            }
        });

        // Initialize SaveManager (use local player)
        com.game.save.SaveManager.getInstance().initialize(
            player,
            player.getInventory(),
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
    }

    private void updateCamera() {
        // Apply camera scale from settings
        if (uiManager != null) {
            float cameraScale = uiManager.getGameSettings().getCameraScale();
            camera.zoom = 1f / cameraScale;
        }

        // Follow the local player
        PlayerEntity player = localPlayer;
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
        // Knocked-out players (ours or others', whose health comes with their state) vanish until they respawn
        if (gameObject instanceof PlayerEntity player && !player.isAlive()) {
            return;
        }

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
        if (joinStage != null) {
            joinStage.getViewport().update(width, height, true);
            characterSelect.setPosition(
                (joinStage.getWidth() - characterSelect.getWidth()) / 2,
                (joinStage.getHeight() - characterSelect.getHeight()) / 2
            );
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
        gameWorld.dispose();
        if (uiManager != null) uiManager.dispose();
        disposeJoinUi();

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
        if (Gdx.input.justTouched() && validPlacement && !placementPending) {
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
     * Place furniture at the specified world position. The item is only taken from the inventory
     * once the placement is confirmed (immediately for the host, after the host replies for guests).
     */
    private void placeFurniture(float x, float y) {
        if (placementFurnitureItem == null) return;

        placementPending = true;
        gameWorld.requestPlaceFurniture(placementFurnitureItem.getDefinition().getId(), x, y, placed -> {
            placementPending = false;
            if (!furniturePlacementMode) return; // Canceled while waiting

            if (placed) {
                uiManager.onFurniturePlaced(); // Removes the item from the inventory
                exitFurniturePlacementMode(true);
            } else {
                showWorldMessage(x + 8, y + 20, "Can't place here", Color.ORANGE);
            }
        });
    }

    /**
     * Handle interaction with nearby furniture.
     * Checks for furniture near the first player and opens UI or picks up.
     */
    private void handleFurnitureInteraction() {
        PlayerEntity player = localPlayer;
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
                    requestOpenChest(chest);
                }
            } else {
                // Other furniture types - just call onInteract
                nearestFurniture.onInteract(player);
            }
        }
    }

    /**
     * Ask to use a chest; opens it if allowed, otherwise shows that someone else is using it.
     */
    private void requestOpenChest(ChestEntity chest) {
        if (chestPendingOpen != null) return;
        chestPendingOpen = chest;
        gameWorld.openChest(chest, granted -> {
            chestPendingOpen = null;
            if (granted) {
                openChest(chest);
            } else {
                showWorldMessage(chest.getTransform().getX() + 8, chest.getTransform().getY() + 20, "In use", Color.LIGHT_GRAY);
            }
        });
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
        gameWorld.closeChest(chest); // Let others use it
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
        if (currentlyOpenChest != null && localPlayer != null) {
            Vector2 chestPos = new Vector2(
                currentlyOpenChest.getTransform().getX(),
                currentlyOpenChest.getTransform().getY()
            );

            // Close chest once the local player walks away
            if (localPlayer.getTransform().getPosition().dst(chestPos) > CHEST_AUTO_CLOSE_DISTANCE) {
                closeChest(currentlyOpenChest);
                System.out.println("GameScreen: Auto-closed chest (player moved away)");
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
        // Level instances load furniture from the manager when they are built
        if (saveData.world.furnitureByLevel != null) {
            furnitureManager.importSaveData(saveData.world.furnitureByLevel);
        }

        // Rebuild levels from scratch so they match the save
        gameWorld.unloadAllLevels();

        String levelId = saveData.world.currentLevelId;
        String levelType = saveData.world.levelType;

        if (levelType.equals("dungeon")) {
            System.out.println("GameScreen: Cannot load dungeon levels (they are temporary)");
            // Fallback to default map
            levelId = GameWorld.START_LEVEL;
        }

        gameWorld.changeLevel(levelId, null);

        // Now apply the rest of the save data (player, inventory, dropped items)
        com.game.save.SaveManager.getInstance().applySaveData(saveData);
        worldItemManager.setCurrentLevel(levelId);

        // Refresh UI to show loaded inventory
        uiManager.refreshAllWindows();

        System.out.println("GameScreen: Loaded save - Level: " + levelId +
                         ", Position: (" + saveData.player.x + ", " + saveData.player.y + ")");
    }

    // ========== Multiplayer ==========

    /**
     * Start hosting a multiplayer game (Open to LAN).
     */
    public void startHosting() {
        if (gameWorld.startHosting()) {
            System.out.println("GameScreen: Other players can connect using 'localhost' or your LAN IP");
        }
    }

    /**
     * Join a host with an already-connected client (called from the main menu).
     * The level is built when the host's welcome arrives.
     */
    public void setGameClient(com.game.networking.GameClient client) {
        gameWorld.join(client);
    }

    /**
     * Whether this machine joined someone else's game.
     */
    public boolean isGuest() {
        return gameWorld.isGuest();
    }

    public void stopMultiplayer() {
        gameWorld.stopMultiplayer();
    }

    public PlayerEntity getLocalPlayer() {
        return localPlayer;
    }

    public GameWorld getGameWorld() {
        return gameWorld;
    }

    private void returnToMainMenu(String errorMessage) {
        screenClosed = true;
        com.badlogic.gdx.Game game = (com.badlogic.gdx.Game) Gdx.app.getApplicationListener();
        game.setScreen(new MainMenuScreen((Main) game, errorMessage));
    }

    @Override
    public void showDamageNumber(float x, float y, int amount) {
        damageNumbers.add(new DamageNumberEntity(x, y, amount, damageFont));
    }

    @Override
    public void showDeathAnimation(float x, float y) {
        deathAnimations.add(new DeathAnimationEntity(x, y));
    }

    @Override
    public void onInventoryChanged() {
        if (uiManager != null) {
            uiManager.notifyInventoryChanged();
        }
    }

    @Override
    public void onConnectionLost(String reason) {
        System.out.println("GameScreen: " + reason);
        returnToMainMenu(reason);
    }
}
