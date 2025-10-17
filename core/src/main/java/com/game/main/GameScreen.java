package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.entity.GatewayEntity;
import com.game.entity.ItemPickupEntity;
import com.game.entity.PlayerEntity;
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

/**
 * Refactored GameScreen using the new decoupled architecture.
 * All systems are now independent and reusable.
 */
public class GameScreen implements Screen {
    private static final int VIEWPORT_WIDTH = 350;
    private static final int VIEWPORT_HEIGHT = 200;

    private SpriteBatch batch;
    private BitmapFont debugFont;
    private boolean debugMode = false;
    private ShapeRenderer shapeRenderer;

    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;

    private WorldManager world;
    private WorldItemManager worldItemManager;
    private PlayerEntity player;
    private TiledMap currentMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private YSortRenderer ySortRenderer;
    private UIManagerNew uiManager;
    private InputManager inputManager;

    private GatewayEntity pendingGateway = null;

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

        // Initialize systems
        worldItemManager = new WorldItemManager();
        inputManager = new InputManager();

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

        // Update input
        inputManager.update();

        // Handle input actions
        handleInputActions();

        // Check for debug toggle
        if (inputManager.isJustPressed(InputAction.DEBUG_TOGGLE)) {
            debugMode = !debugMode;
            if (ySortRenderer != null) {
                ySortRenderer.setDebugMode(debugMode);
            }
            System.out.println("Debug mode: " + debugMode);
        }

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update world
        world.update(delta);

        // Update world items
        worldItemManager.update(delta);

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

        // Render UI
        if (uiManager != null) {
            uiManager.render();
        }

        // Render debug
        if (debugMode) {
            renderCollisionDebug();
            renderDebugStats();
        }
    }

    /**
     * Handles input actions from InputManager.
     */
    private void handleInputActions() {
        // Open inventory
        if (inputManager.isJustPressed(InputAction.OPEN_INVENTORY)) {
            if (uiManager != null) {
                uiManager.toggleInventory();

                // If inventory is closed, restore input to stage (for HUD)
                // If open, input processor stays on stage (for dragging)
                // Stage always handles input when UI exists
            }
        }

        // Debug: Spawn wood item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_ITEM)) {
            spawnDebugItem("wood");
        }

        // Debug: Spawn bag item
        if (debugMode && inputManager.isJustPressed(InputAction.DEBUG_SPAWN_BAG)) {
            spawnDebugItem("bag");
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
                    System.out.println("Picked up: " + itemStack.toString());
                    inventoryChanged = true;
                } else if (remaining.getQuantity() < itemStack.getQuantity()) {
                    // Partial pickup
                    item.getItemStack().setQuantity(remaining.getQuantity());
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
     * Load a level using the new decoupled systems.
     */
    private void loadLevel(String levelPath, String spawnPointName) {
        System.out.println("Loading level: " + levelPath + " at spawn: " + spawnPointName);

        // Dispose previous resources
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (currentMap != null) {
            currentMap.dispose();
        }

        // Load Tiled map
        currentMap = new TmxMapLoader().load(levelPath);
        mapRenderer = new OrthogonalTiledMapRenderer(currentMap);
        ySortRenderer = new YSortRenderer(mapRenderer, currentMap);

        // Parse level data
        LevelData levelData = TiledMapParser.parse(currentMap);

        // Create world manager
        world = new WorldManager(levelData.getWidth(), levelData.getHeight());

        // Load collision system
        SpatialQuery collisionSystem = new SpatialQuery();
        TiledMapCollisionLoader.loadFromTiledMap(currentMap, collisionSystem);
        world.setCollisionSystem(collisionSystem);
        System.out.println("Loaded " + collisionSystem.getShapeCount() + " collision shapes");

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

        // OLD SYSTEM: Convert to grid and back to match old behavior
        // This ensures spawn positions match the old LevelLoader exactly
        int spawnGridX = (int)(spawnX / world.getTileSize());
        int spawnGridY = (int)(spawnY / world.getTileSize());
        spawnX = spawnGridX * world.getTileSize();
        spawnY = spawnGridY * world.getTileSize();

        System.out.println("Spawning player at: (" + spawnX + ", " + spawnY + ") - Grid: (" + spawnGridX + ", " + spawnGridY + ")");

        // Create or update player
        if (player == null) {
            player = new PlayerEntity(world, spawnX, spawnY);
        } else {
            player.setWorld(world);
            player.getTransform().setPosition(spawnX, spawnY);
        }
        world.addGameObject(player);

        // Initialize UI manager
        if (uiManager == null) {
            uiManager = new UIManagerNew(player.getInventory(), worldItemManager);
            uiManager.setItemDropCallback(itemStack -> {
                // Drop item at player position
                Vector2 playerPos = player.getTransform().getPosition();
                worldItemManager.spawnItem(itemStack, playerPos.x, playerPos.y, 3f);
            });
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
        Transform playerTransform = player.getTransform();
        float playerCenterX = playerTransform.getX() + (world.getTileSize() / 2f);
        float playerCenterY = playerTransform.getY() + (world.getTileSize() / 2f);

        float worldWidth = world.getWorldWidth() * world.getTileSize();
        float worldHeight = world.getWorldHeight() * world.getTileSize();

        float cameraHalfWidth = camera.viewportWidth * camera.zoom / 2f;
        float cameraHalfHeight = camera.viewportHeight * camera.zoom / 2f;

        float camX = Math.max(cameraHalfWidth, Math.min(playerCenterX, worldWidth - cameraHalfWidth));
        float camY = Math.max(cameraHalfHeight, Math.min(playerCenterY, worldHeight - cameraHalfHeight));

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
        }

        shapeRenderer.end();
    }

    /**
     * Render a single entity. Called by Y-sort renderer.
     */
    private void renderEntity(SpriteBatch batch, GameObject gameObject) {
        RenderComponent renderComp = gameObject.getComponent(RenderComponent.class);
        if (renderComp != null) {
            renderComp.render(batch, gameObject);
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
        debugFont.draw(batch, "Press F3 to toggle debug", x, y - lineHeight * 4);

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
        if (mapRenderer != null) mapRenderer.dispose();
        if (currentMap != null) currentMap.dispose();
        if (uiManager != null) uiManager.dispose();
    }
}
